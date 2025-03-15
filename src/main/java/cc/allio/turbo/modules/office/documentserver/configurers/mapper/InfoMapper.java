package cc.allio.turbo.modules.office.documentserver.configurers.mapper;

import cc.allio.turbo.modules.office.documentserver.configurers.wrappers.DefaultDocumentWrapper;
import cc.allio.turbo.modules.office.documentserver.models.configurations.Info;
import cc.allio.turbo.modules.office.documentserver.util.DocDescriptor;
import cc.allio.turbo.modules.office.vo.DocUser;

/**
 * map to {@link Info}
 *
 * @author j.x
 * @date 2024/5/13 14:11
 * @since 0.0.1
 */
public class InfoMapper implements Mapper<DefaultDocumentWrapper, Info> {

    @Override
    public Info toModel(DefaultDocumentWrapper wrapper) {
        DocDescriptor doc = wrapper.getDoc();
        Info info = new Info();
        DocUser docUser = wrapper.getDocUser();
        info.setOwner(docUser.getUsername());
        info.setUploaded(doc.getCreateTime());
        return info;
    }
}
