package com.mock.knowledge.controller;

import com.mock.knowledge.model.KnowledgeRequest;
import com.mock.knowledge.model.KnowledgeResponse;
import com.mock.knowledge.service.RoutingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 知识服务挡板统一入口
 */
@RestController
public class KnowledgeController {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeController.class);

    private final RoutingService routingService;

    public KnowledgeController(RoutingService routingService) {
        this.routingService = routingService;
    }

    @PostMapping("/api/knowledge")
    public KnowledgeResponse handle(@RequestBody KnowledgeRequest req) {
        log.info("收到请求: transcode={}, entName={}, moduleCode={}, key={}",
                req.getTranscode(),
                req.getParams() != null ? req.getParams().getEntName() : null,
                req.getParams() != null ? req.getParams().getModuleCode() : null,
                req.getParams() != null ? req.getParams().getKey() : null);

        String transcode = req.getTranscode();

        if ("RM1201".equals(transcode)) {
            return routingService.handleRM1201(req);
        } else if ("RM1202".equals(transcode)) {
            return routingService.handleRM1202(req);
        } else {
            log.warn("不支持的 transcode: {}", transcode);
            return routingService.handleRM1201(req); // 兼容，走默认错误
        }
    }
}
