package com.mock.knowledge.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.knowledge.model.KnowledgeRequest;
import com.mock.knowledge.model.KnowledgeResponse;
import com.mock.knowledge.service.RealtimeKnowledgeService;
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
    private static final int RESP_LOG_MAX = 2000;
    private static final String PFX = "[MOCK] ";

    private final RoutingService routingService;
    private final RealtimeKnowledgeService realtimeKnowledgeService;
    private final ObjectMapper objectMapper;

    public KnowledgeController(RoutingService routingService,
                               RealtimeKnowledgeService realtimeKnowledgeService,
                               ObjectMapper objectMapper) {
        this.routingService = routingService;
        this.realtimeKnowledgeService = realtimeKnowledgeService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/knowledge")
    public KnowledgeResponse handle(@RequestBody KnowledgeRequest req) {
        String transcode = req.getTranscode();
        String entName = req.getParams() != null ? req.getParams().getEntName() : null;
        String moduleCode = req.getParams() != null ? req.getParams().getModuleCode() : null;
        String key = req.getParams() != null ? req.getParams().getKey() : null;

        log.info("{}>> {} entName={} moduleCode={} key={}", PFX, transcode, entName, moduleCode, key);

        KnowledgeResponse resp;
        if ("RM1201".equals(transcode)) {
            resp = routingService.handleRM1201(req);
        } else if ("RM1202".equals(transcode)) {
            resp = routingService.handleRM1202(req);
        } else if ("RM1203".equals(transcode)) {
            resp = realtimeKnowledgeService.handle(req);
        } else {
            log.warn("{}不支持的 transcode: {}", PFX, transcode);
            resp = new KnowledgeResponse("9999", "接口不存在: " + transcode, objectMapper.createArrayNode());
        }

        // 打印返回报文
        try {
            String respJson = objectMapper.writeValueAsString(resp);
            if (respJson.length() > RESP_LOG_MAX) {
                respJson = respJson.substring(0, RESP_LOG_MAX) + "...(truncated, total=" + respJson.length() + ")";
            }
            log.info("{}<< {}", PFX, respJson);
        } catch (Exception e) {
            log.info("{}<< code={} (序列化失败)", PFX, resp.getCode());
        }

        return resp;
    }
}
