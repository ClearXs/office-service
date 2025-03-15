package cc.allio.turbo.modules.office.constant;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.common.util.AuthUtil;
import cc.allio.turbo.modules.auth.provider.TurboUser;
import cc.allio.turbo.modules.office.dto.PermissionShareDTO;
import cc.allio.turbo.modules.office.vo.DocUser;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * share mode
 *
 * @author j.x
 * @date 2024/8/22 16:53
 */
@Getter
@AllArgsConstructor
public enum ShareMode {

    // allow anyone visit the share document
    ANYONE(
            "anyone",
            (share, docUser) -> {
                List<String> cooperator = share.getCooperator();

                if (AuthUtil.hasAuthentication() || !cooperator.contains(AuthUtil.getUserId())) {
                    // current user not cooperator
                    throw new BizException("current user unauthorized visit document");
                }
                String userId = AuthUtil.getUserId();
                String username = AuthUtil.getUsername();
                docUser.setUserId(userId);
                docUser.setUsername(username);
            }),

    // only specific user allowable visit share document
    SPECIFIC(
            "specific",
            (share, docUser) -> {
                if (!AuthUtil.hasAuthentication()) {
                    // anonymous user
                    docUser.setUserId("anonymous");
                    docUser.setUsername("anonymous");
                } else {
                    String userId = AuthUtil.getUserId();
                    String username = AuthUtil.getUsername();
                    docUser.setUserId(userId);
                    docUser.setUsername(username);
                }
            });

    @JsonValue
    private final String value;
    private final Visitor visitor;

    /**
     * through Visitor Pattern allow share mode stateful modification {@link DocUser} content.
     *
     * @param share   the {@link PermissionShareDTO} instance
     * @param docUser the {@link DocUser} instance
     * @throws BizException throw if current user unauthorized
     */
    public void visit(PermissionShareDTO share, DocUser docUser) throws BizException {
        this.visitor.visit(share, docUser);
    }

    @FunctionalInterface
    interface Visitor {
        void visit(PermissionShareDTO share, DocUser docUser) throws BizException;
    }
}
