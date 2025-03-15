package cc.allio.turbo.modules.office.service.impl;

import cc.allio.turbo.common.db.mybatis.service.impl.TurboCrudServiceImpl;
import cc.allio.turbo.modules.office.constant.WebhookType;
import cc.allio.turbo.modules.office.documentserver.vo.Track;
import cc.allio.turbo.modules.office.entity.DocWebhook;
import cc.allio.turbo.modules.office.mapper.DocWebhookMapper;
import cc.allio.turbo.modules.office.service.IDocWebhookService;
import cc.allio.uno.http.metadata.HttpSwapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Service
public class DocWebhookServiceImpl extends TurboCrudServiceImpl<DocWebhookMapper, DocWebhook> implements IDocWebhookService {

    @Override
    public void trigger(WebhookType type, Track track) {
        List<DocWebhook> webhooks = list(Wrappers.<DocWebhook>lambdaQuery().eq(DocWebhook::getType, type.getValue()));
        Flux.fromIterable(webhooks)
                .map(webhook -> {
                    String url = webhook.getUrl();
                    Map<String, String> headers = webhook.getHeaders();

                    HttpSwapper httpSwapper = HttpSwapper.build(url, HttpMethod.POST);
                    httpSwapper.addBody(track);
                    headers.forEach(httpSwapper::addHeader);
                    return httpSwapper;
                })
                .flatMap(HttpSwapper::swap)
                .subscribeOn(Schedulers.fromExecutor(Executors.newVirtualThreadPerTaskExecutor()))
                .subscribe();
    }
}
