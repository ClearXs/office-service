package cc.allio.turbo.modules.office.entity;

import cc.allio.turbo.common.db.entity.TenantEntity;
import cc.allio.turbo.modules.office.constant.WebhookType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("office_doc_webhook")
@Schema(description = "webhook")
public class DocWebhook extends TenantEntity {

    @TableField("url")
    @Schema(name = "callback url")
    private String url;

    @TableField(value = "headers", typeHandler = JacksonTypeHandler.class)
    @Schema(name = "headers")
    private List<Header> headers;

    @TableField("type")
    @Schema(name = "hook type")
    private WebhookType type;

    @Data
    public static class Header {
        private String id;
        private String key;
        private String value;
    }
}
