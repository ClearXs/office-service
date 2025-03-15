package cc.allio.turbo.modules.office.documentserver.configurers.wrappers;

import cc.allio.turbo.modules.office.documentserver.models.filemodel.Permission;
import cc.allio.turbo.modules.office.documentserver.util.DocDescriptor;
import cc.allio.turbo.modules.office.vo.DocUser;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DefaultDocumentWrapper {
    private DocDescriptor doc;
    private DocUser docUser;
    private Permission permission;
    private Long fileId;
    private String filename;
    private String filepath;
    private Boolean favorite;
    private Boolean isEnableDirectUrl;
}
