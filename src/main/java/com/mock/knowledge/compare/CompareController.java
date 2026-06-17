package com.mock.knowledge.compare;

import com.mock.knowledge.compare.dto.CompareRequest;
import com.mock.knowledge.compare.dto.CompareResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 630接口对比测试入口
 *
 * POST /api/compare/enterprise  → 单企业对比
 * GET  /api/compare/enterprises → 列出可用企业
 * POST /api/compare/batch       → 批量三户对比
 */
@RestController
@RequestMapping("/api/compare")
public class CompareController {

    private static final Logger log = LoggerFactory.getLogger(CompareController.class);

    private final CompareService compareService;
    private final OfflineDataLoader offlineLoader;

    public CompareController(CompareService compareService, OfflineDataLoader offlineLoader) {
        this.compareService = compareService;
        this.offlineLoader = offlineLoader;
    }

    @PostMapping("/enterprise")
    public CompareResponse compare(@RequestBody CompareRequest req) {
        log.info("对比请求: enterprise={}, interfaces={}", req.getEnterpriseName(), req.getInterfaces());
        return compareService.compare(req.getEnterpriseName(), req.getInterfaces());
    }

    @GetMapping("/enterprises")
    public Map<String, Object> enterprises() {
        Map<String, Object> m = new HashMap<>();
        m.put("enterprises", offlineLoader.listEnterprises());
        return m;
    }

    @PostMapping("/batch")
    public Map<String, Object> batch(@RequestBody(required = false) CompareRequest req) {
        List<String> enterprises;
        if (req != null && req.getEnterpriseName() != null && !req.getEnterpriseName().isEmpty()) {
            enterprises = Collections.singletonList(req.getEnterpriseName());
        } else {
            enterprises = offlineLoader.listEnterprises();
        }

        List<CompareResponse> results = new ArrayList<>();
        for (String ent : enterprises) {
            log.info("批量对比: {}", ent);
            results.add(compareService.compare(ent, req != null ? req.getInterfaces() : null));
        }

        int totalOk = 0, totalPerm = 0, totalErr = 0, totalFields = 0, totalMatched = 0, totalMismatch = 0;
        for (CompareResponse r : results) {
            if (r.getSummary() != null) {
                totalOk += r.getSummary().getSuccessInterfaces();
                totalPerm += r.getSummary().getNoPermissionInterfaces();
                totalErr += r.getSummary().getErrorInterfaces();
                totalFields += r.getSummary().getTotalFieldsCompared();
                totalMatched += r.getSummary().getMatched();
                totalMismatch += r.getSummary().getOnlyRealtime() + r.getSummary().getOnlyOffline()
                               + r.getSummary().getValueMismatch();
            }
        }

        Map<String, Object> summaryMap = new LinkedHashMap<>();
        summaryMap.put("totalOk", totalOk);
        summaryMap.put("totalNoPermission", totalPerm);
        summaryMap.put("totalError", totalErr);
        summaryMap.put("totalFieldsCompared", totalFields);
        summaryMap.put("totalMatched", totalMatched);
        summaryMap.put("totalMismatch", totalMismatch);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("enterprises", enterprises);
        result.put("results", results);
        result.put("summary", summaryMap);
        return result;
    }
}
