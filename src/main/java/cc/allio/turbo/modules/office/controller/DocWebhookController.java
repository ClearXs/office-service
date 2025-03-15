package cc.allio.turbo.modules.office.controller;

import cc.allio.turbo.common.web.TurboCrudController;
import cc.allio.turbo.modules.office.entity.DocWebhook;
import cc.allio.turbo.modules.office.service.IDocWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/office/doc/webhook")
@Tag(name = "webhook")
public class DocWebhookController extends TurboCrudController<DocWebhook, DocWebhook, IDocWebhookService> {
}
