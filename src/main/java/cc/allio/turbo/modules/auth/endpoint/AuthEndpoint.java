package cc.allio.turbo.modules.auth.endpoint;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.common.web.R;
import cc.allio.turbo.common.web.TurboController;
import cc.allio.turbo.modules.auth.authentication.TurboJwtAuthenticationToken;
import cc.allio.turbo.modules.auth.dto.CaptchaDTO;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.auth.service.IAuthService;
import cc.allio.turbo.modules.system.domain.SysMenuTree;
import cc.allio.turbo.modules.system.dto.ChangePasswordDTO;
import cc.allio.turbo.modules.system.entity.SysOrg;
import cc.allio.turbo.modules.system.entity.SysPost;
import cc.allio.turbo.modules.system.entity.SysRole;
import cc.allio.turbo.modules.system.entity.SysUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@AllArgsConstructor
@Tag(name = "auth")
public class AuthEndpoint extends TurboController {

    private IAuthService authService;

    @GetMapping("/captcha")
    @Operation(summary = "获取验证码")
    public R<CaptchaDTO> captcha() {
        CaptchaDTO captcha = authService.captcha();
        return ok(captcha);
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public R<TurboJwtAuthenticationToken> register(@RequestBody SysUser sysUser) throws BizException {
        TurboJwtAuthenticationToken token = authService.register(sysUser);
        return ok(token);
    }

    @GetMapping("/current-user")
    @Operation(summary = "获取当前用户")
    public R<TurboUser> currentUser(@Valid @NotNull TurboUser user) {
        return R.ok(user);
    }

    @GetMapping("/current-user-menus")
    @Operation(summary = "获取当前用户菜单")
    public R<List<SysMenuTree>> currentUserMenus(@Valid @NotNull TurboUser user) {
        List<SysMenuTree> sysMenuTrees = authService.getUserMenus(user);
        return ok(sysMenuTrees);
    }

    @GetMapping("/current-user-org")
    @Operation(summary = "获取当前用户组织")
    public R<SysOrg> currentUserOrg(@Valid @NotNull TurboUser user) {
        SysOrg sysOrg = authService.getUserOrg(user);
        return ok(sysOrg);
    }

    @GetMapping("/current-user-role")
    @Operation(summary = "获取当前用户角色")
    public R<List<SysRole>> currentUserRole(@Valid @NotNull TurboUser user) {
        List<SysRole> sysRoles = authService.getUserRole(user);
        return ok(sysRoles);
    }

    @GetMapping("/current-user-post")
    @Operation(summary = "获取当前用户岗位")
    public R<List<SysPost>> currentUserPost(@Valid @NotNull TurboUser user) {
        List<SysPost> sysPosts = authService.getUserPost(user);
        return ok(sysPosts);
    }

    @GetMapping("/logout")
    @Operation(summary = "退出")
    public R logout() {
        // TODO 后续处理
        return success();
    }

    @PutMapping("/changePassword")
    @Operation(summary = "修改密码")
    public R<TurboJwtAuthenticationToken> changePassword(@Valid @NotNull TurboUser user, @RequestBody @Validated ChangePasswordDTO changePassword) throws BizException {
        TurboJwtAuthenticationToken newToken = authService.changePassword(user, changePassword);
        return ok(newToken);
    }

    @PutMapping("/modify")
    @Operation(summary = "修改用户信息")
    public R<TurboJwtAuthenticationToken> modify(@RequestBody TurboUser user) throws BizException {
        TurboJwtAuthenticationToken newToken = authService.modify(user);
        return ok(newToken);
    }
}
