package cc.allio.turbo.modules.office.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PermissionShareDTO extends ShareDTO {

    /**
     * permission group id
     */
    @Schema(name = "权限组id")
    private Long permissionGroupId;


    /**
     * from {@link ShareDTO} return new {@link PermissionShareDTO}
     *
     * @param share the {@link ShareDTO} instance
     * @return
     */
    public static PermissionShareDTO from(ShareDTO share) {
        PermissionShareDTO permissionShareDTO = new PermissionShareDTO();
        permissionShareDTO.setDocId(share.getDocId());
        permissionShareDTO.setExpired(share.getExpired());
        permissionShareDTO.setMode(share.getMode());
        permissionShareDTO.setCooperator(share.getCooperator());
        permissionShareDTO.setPermission(share.getPermission());
        return permissionShareDTO;
    }
}
