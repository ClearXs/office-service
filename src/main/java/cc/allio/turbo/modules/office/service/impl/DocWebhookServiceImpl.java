package cc.allio.turbo.modules.office.service.impl;

import cc.allio.turbo.common.db.mybatis.service.impl.TurboCrudServiceImpl;
import cc.allio.turbo.modules.office.constant.WebhookType;
import cc.allio.turbo.modules.office.entity.DocWebhook;
import cc.allio.turbo.modules.office.mapper.DocWebhookMapper;
import cc.allio.turbo.modules.office.service.IDocWebhookService;
import cc.allio.turbo.modules.system.entity.SysAttachment;
import cc.allio.uno.core.util.CollectionUtils;
import cc.allio.uno.http.metadata.HttpSwapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.Data;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.concurrent.Executors;

@Service
public class DocWebhookServiceImpl extends TurboCrudServiceImpl<DocWebhookMapper, DocWebhook> implements IDocWebhookService {

    @Override
    public void trigger(WebhookType type, Long docId, SysAttachment attachment) {
        List<DocWebhook> webhooks = list(Wrappers.<DocWebhook>lambdaQuery().eq(DocWebhook::getType, type.getValue()));
        Flux.fromIterable(webhooks)
                .map(webhook -> {
                    String url = webhook.getUrl();
                    HttpSwapper httpSwapper = HttpSwapper.build(url, HttpMethod.POST);

                    List<DocWebhook.Header> headers = webhook.getHeaders();
                    if (CollectionUtils.isNotEmpty(headers)) {
                        headers.forEach(header -> httpSwapper.addHeader(header.getKey(), header.getValue()));
                    }

                    Trace trace = new Trace();
                    trace.setDocId(docId);
                    trace.setAttachment(attachment);
                    httpSwapper.addBody(trace);

                    return httpSwapper;
                })
                .flatMap(HttpSwapper::swap)
                .subscribeOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
                .subscribe();
    }

    @Data
    public static class Trace {

        private Long docId;
        private SysAttachment attachment;
    }
}
