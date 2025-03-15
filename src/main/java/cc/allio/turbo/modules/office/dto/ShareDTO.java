package cc.allio.turbo.modules.office.dto;

import cc.allio.turbo.modules.office.constant.ShareExpired;
import cc.allio.turbo.modules.office.constant.ShareMode;
import cc.allio.turbo.modules.office.vo.DocPermission;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class ShareDTO {

    /**
     * 文档Id
     */
    @Schema(name = "文档Id")
    @NotNull
    private Long docId;

    /**
     * 权限组
     */
    @Schema(name = "权限组")
    @NotNull
    private DocPermission permission;

    /**
     * 过期时间
     */
    @Schema(name = "过期时间")
    private ShareExpired expired = ShareExpired.ONE_DAY;

    /**
     * 分享模式
     */
    @Schema(name = "分享模式")
    private ShareMode mode = ShareMode.ANYONE;

    /**
     * 协作者
     */
    @Schema(name = "协作者")
    private List<String> cooperator = Collections.emptyList();
}
