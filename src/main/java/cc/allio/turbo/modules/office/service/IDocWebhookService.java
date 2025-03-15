package cc.allio.turbo.modules.office.service;

import cc.allio.turbo.common.db.mybatis.service.ITurboCrudService;
import cc.allio.turbo.modules.office.constant.WebhookType;
import cc.allio.turbo.modules.office.documentserver.vo.Track;
import cc.allio.turbo.modules.office.entity.DocWebhook;

public interface IDocWebhookService extends ITurboCrudService<DocWebhook> {

    /**
     * trigger webhook
     */
    void trigger(WebhookType type, Track track);
}
