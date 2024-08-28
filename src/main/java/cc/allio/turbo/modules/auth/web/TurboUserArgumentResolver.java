package cc.allio.turbo.modules.auth.web;

import cc.allio.turbo.common.util.WebUtil;
import cc.allio.turbo.modules.auth.authentication.TurboJwtAuthenticationToken;
import cc.allio.turbo.modules.auth.authority.TurboGrantedAuthority;
import cc.allio.turbo.modules.auth.jwt.JwtAuthentication;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.auth.service.IAuthService;
import cc.allio.turbo.modules.system.constant.UserSource;
import cc.allio.turbo.modules.system.domain.SysUserVO;
import cc.allio.turbo.modules.system.entity.SysRole;
import cc.allio.turbo.modules.system.entity.SysThirdUser;
import cc.allio.turbo.modules.system.entity.SysUser;
import cc.allio.turbo.modules.system.service.ISysThirdUserService;
import cc.allio.turbo.modules.system.service.ISysUserService;
import cc.allio.uno.core.StringPool;
import cc.allio.uno.core.util.JsonUtils;
import cc.allio.uno.core.util.StringUtils;
import cc.allio.uno.data.tx.TransactionContext;
import lombok.Data;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.data.util.Optionals;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * resolve the {@link TurboUser}.
 * <p></p>
 *
 * @author j.x
 * @date 2024/8/27 10:43
 * @since 0.1.1
 */
@Slf4j
public class TurboUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final JwtAuthentication jwtAuthentication;
    private static final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    private final IAuthService authService;
    private final ISysUserService sysUserService;
    private final ISysThirdUserService thirdUserService;

    static final String USER_IDENTIFIER = "user";

    public TurboUserArgumentResolver(JwtAuthentication jwtAuthentication,
                                     IAuthService authService,
                                     ISysUserService sysUserService,
                                     ISysThirdUserService thirdUserService) {
        this.jwtAuthentication = jwtAuthentication;
        this.authService = authService;
        this.sysUserService = sysUserService;
        this.thirdUserService = thirdUserService;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().isAssignableFrom(TurboUser.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) throws Exception {
        // try to request get user and then from token
        return Optionals.firstNonEmpty(
                        () -> loadByAuthentication(webRequest),
                        () -> loadByUser(parameter, webRequest)
                )
                .orElse(null);
    }

    /**
     * from current request header or parameter
     *
     * @param webRequest the current request
     * @return
     */
    Optional<TurboUser> loadByAuthentication(NativeWebRequest webRequest) {
        SecurityContext context = securityContextHolderStrategy.getContext();
        Authentication authentication = context.getAuthentication();
        return Optional.ofNullable(authentication)
                .map(JwtAuthenticationToken.class::cast)
                .map(TurboUser::new)
                .or(() ->
                        Optionals.firstNonEmpty(
                                        // first try to security context holder
                                        () -> Optional.ofNullable(webRequest.getHeader(WebUtil.X_AUTHENTICATION)),
                                        () -> Optional.ofNullable(webRequest.getParameter(WebUtil.X_AUTHENTICATION))
                                )
                                .flatMap(token -> {
                                    Jwt jwt = jwtAuthentication.decode(token);
                                    return Optional.ofNullable(jwt).map(TurboUser::new);
                                }));
    }

    /**
     * from current request header or parameter get user info (determinate user is third user)
     *
     * @param parameter the method parameter to resolve.
     * @param webRequest the current request
     * @return maybe null
     */
    Optional<TurboUser> loadByUser(MethodParameter parameter, NativeWebRequest webRequest) {
        return Optionals.firstNonEmpty(
                        () -> Optional.ofNullable(webRequest.getHeader(USER_IDENTIFIER)),
                        () -> Optional.ofNullable(webRequest.getParameter(USER_IDENTIFIER))
                )
                .flatMap(userString -> {
                    if (StringUtils.isBlank(userString)) {
                        return Optional.empty();
                    }
                    SimpleUser user;
                    try {
                        user = JsonUtils.parse(userString, SimpleUser.class);
                        if (user == null || user.getUserId() == null) {
                            return Optional.empty();
                        }
                        if (log.isDebugEnabled()) {
                            log.debug("Get endpoint {} method {} from web method TurboUser parameter is {}",
                                    parameter.getContainingClass().getName(),
                                    Optional.ofNullable(parameter.getMethod()).map(Method::getName).orElse(StringPool.EMPTY),
                                    user);
                        }
                    } catch (Exception ex) {
                        log.error("Failed parse from web parameter get TurboUser", ex);
                        return Optional.empty();
                    }
                    return findAndRegisterUser(user);
                });
    }

    /**
     * if user not exiting turbo database. then register user to turbo.
     *
     * @param user the parameter user
     * @return maybe null
     */
    Optional<TurboUser> findAndRegisterUser(SimpleUser user) {
        SysThirdUser thirdUser = thirdUserService.getById(user.getUserId());
        return Optional.ofNullable(thirdUser)
                .map(SysThirdUser::getUserId)
                .flatMap(userId -> {
                    SysUserVO userDetails = sysUserService.findThirdUserDetails(user.getUserId());
                    List<SysRole> roles = userDetails.getRoles();
                    Set<TurboGrantedAuthority> authorities =
                            roles.stream()
                                    .map(role -> new TurboGrantedAuthority(role.getId(), role.getCode(), role.getName()))
                                    .collect(Collectors.toSet());
                    return Optional.of(new TurboUser(userDetails, authorities));
                })
                .or(() -> {
                    TurboUser turboUser =
                            TransactionContext.execute(
                                    () -> {
                                        // register third user
                                        SysThirdUser newThirdUser = new SysThirdUser();
                                        newThirdUser.setUuid(user.getUserId());
                                        SysUser sysUser = new SysUser();
                                        sysUser.setUsername(user.getUserName());
                                        sysUser.setNickname(user.getNickname());
                                        sysUser.setSource(UserSource.THIRD);
                                        TurboJwtAuthenticationToken token = authService.register(sysUser);
                                        // save to thirdUser
                                        newThirdUser.setUserId(sysUser.getId());
                                        thirdUserService.save(newThirdUser);
                                        return new TurboUser(token.getToken());
                                    });
                    return Optional.ofNullable(turboUser);
                });
    }

    @Data
    @ToString
    static class SimpleUser {
        private String userId;
        private String userName;
        private String nickname;
    }
}
