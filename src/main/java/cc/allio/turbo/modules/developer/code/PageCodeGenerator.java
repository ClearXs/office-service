package cc.allio.turbo.modules.developer.code;

import cc.allio.turbo.common.exception.BizException;
import cc.allio.turbo.modules.developer.constant.CodeGenerateSource;
import cc.allio.turbo.modules.developer.domain.BoSchema;
import cc.allio.turbo.modules.developer.domain.hybrid.HybridBoSchema;
import cc.allio.turbo.modules.developer.domain.view.DataView;
import cc.allio.turbo.modules.developer.entity.DevCodeGenerate;
import cc.allio.turbo.modules.developer.entity.DevCodeGenerateTemplate;
import cc.allio.turbo.modules.developer.entity.DevPage;
import cc.allio.turbo.modules.developer.service.IDevBoService;
import cc.allio.turbo.modules.developer.service.IDevPageService;
import cc.allio.turbo.modules.developer.vo.CodeContent;
import cc.allio.turbo.modules.system.entity.SysCategory;
import cc.allio.turbo.modules.system.service.ISysCategoryService;
import cc.allio.uno.core.util.JsonUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

/**
 * the {@link CodeGenerateSource#PAGE} of {@link CodeGenerator} implementation. it will be use page bo schema
 *
 * @author j.x
 * @date 2024/6/20 22:21
 * @since 0.1.1
 */
@Slf4j
public class PageCodeGenerator implements CodeGenerator {

    private final IDevPageService devPageService;
    private final IDevBoService devBoService;
    private final ISysCategoryService categoryService;

    public PageCodeGenerator(IDevPageService devPageService, IDevBoService devBoService, ISysCategoryService categoryService) {
        this.devPageService = devPageService;
        this.devBoService = devBoService;
        this.categoryService = categoryService;
    }

    @Override
    public List<CodeContent> generate(DevCodeGenerate codeGenerate, List<DevCodeGenerateTemplate> templates) {
        Long pageId = codeGenerate.getPageId();
        DevPage page = devPageService.getById(pageId);
        if (page == null) {
            log.error("code generator by 'page', but page is null");
            return Collections.emptyList();
        }
        Long boId = page.getBoId();
        BoSchema boSchema;
        try {
            boSchema = devBoService.cacheToSchema(boId);
        } catch (BizException ex) {
            log.error("not find business object", ex);
            return Collections.emptyList();
        }
        if (boSchema == null) {
            log.error("not found bo schema");
            return Collections.emptyList();
        }
        String dataviewString = codeGenerate.getDataView();
        DataView dataView = JsonUtils.parse(dataviewString, DataView.class);
        HybridBoSchema hybridBoSchema =
                HybridBoSchema.composite(
                        boSchema,
                        dataView,
                        new FilterFieldColumnStrategy(codeGenerate.getIgnoreDefaultField()));
        SysCategory category = categoryService.getById(codeGenerate.getCategoryId());
        Module module = Module.from(category);
        Instance instance = Instance.from(codeGenerate);
        CodeGenerateContext codeGenerateContext = new CodeGenerateContext();
        codeGenerateContext.setModule(module);
        codeGenerateContext.setBoSchema(hybridBoSchema);
        codeGenerateContext.setInstance(instance);
        return doGenerate(codeGenerateContext, templates);
    }

    @Override
    public CodeGenerateSource getSource() {
        return CodeGenerateSource.PAGE;
    }
}
