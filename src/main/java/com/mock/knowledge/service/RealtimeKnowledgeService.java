package com.mock.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.mock.knowledge.compare.RealtimeApiClient;
import com.mock.knowledge.config.AppConfig;
import com.mock.knowledge.model.KnowledgeRequest;
import com.mock.knowledge.model.KnowledgeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * RM1203 实时知识服务
 * moduleCode → 630接口 → 实时API调用 → 子表提取 → 统一返参
 * 支持 preStep 二步调用（如先用企业名查信用代码、先用R4G05查serialNo）
 */
@Service
public class RealtimeKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(RealtimeKnowledgeService.class);
    private static final String MAPPING_PATH = "./offline-data/rm1203_modulecode_mapping.json";

    private final AppConfig appConfig;
    private final RealtimeApiClient apiClient;
    private final ObjectMapper mapper;

    /** moduleCode → {name, transcode, dataKey, preStep?} */
    private Map<String, Map<String, Object>> mapping = new LinkedHashMap<>();

    public RealtimeKnowledgeService(AppConfig appConfig, RealtimeApiClient apiClient,
                                     ObjectMapper mapper) {
        this.appConfig = appConfig;
        this.apiClient = apiClient;
        this.mapper = mapper;
    }

    @PostConstruct
    @SuppressWarnings("unchecked")
    public void loadMapping() {
        try {
            String json = new String(Files.readAllBytes(Paths.get(MAPPING_PATH)),
                    java.nio.charset.StandardCharsets.UTF_8);
            mapping = mapper.readValue(json, new TypeReference<Map<String, Map<String, Object>>>() {});
            log.info("RM1203 映射加载完成: {} 个 moduleCode", mapping.size());
        } catch (Exception e) {
            log.error("RM1203 映射加载失败: {}", e.getMessage());
        }
    }

    // ========== 入口 ==========

    @SuppressWarnings("unchecked")
    public KnowledgeResponse handle(KnowledgeRequest req) {
        String entName = req.getParams().getEntName();
        String moduleCode = req.getParams().getModuleCode();

        if (isBlank(entName) || isBlank(moduleCode)) {
            return error("1000", "请求参数不合法: entName/moduleCode 不能为空");
        }

        Map<String, Object> cfg = mapping.get(moduleCode);
        if (cfg == null) {
            log.warn("RM1203 moduleCode 未注册: {}", moduleCode);
            return error("9999", "未支持的 moduleCode: " + moduleCode);
        }

        String transcode = (String) cfg.get("transcode");
        String dataKey = (String) cfg.get("dataKey");
        String name = (String) cfg.get("name");
        Map<String, Object> preStep = (Map<String, Object>) cfg.get("preStep");
        Map<String, Object> extraParams = (Map<String, Object>) cfg.get("extraParams");

        log.info("RM1203 entName={} moduleCode={} -> {} dataKey={} preStep={}",
                entName, moduleCode, transcode, dataKey, preStep != null);

        try {
            Map<String, Object> apiResp;

            // --- 执行 preStep（如果有）---
            if (preStep != null) {
                String preTranscode = (String) preStep.get("transcode");
                String preDataKey = (String) preStep.get("dataKey");
                String extractField = (String) preStep.get("extractField");
                String useAsParam = (String) preStep.get("useAsParam");

                log.info("RM1203 preStep: {} → extract {}.{} → use as {}", preTranscode, preDataKey, extractField, useAsParam);

                // 调 preStep 接口
                Map<String, Object> preResp = apiClient.call(preTranscode, entName, "1", Collections.emptyMap());
                String preCode = String.valueOf(preResp.getOrDefault("code", "?"));

                if (!appConfig.getSuccessCode().equals(preCode)) {
                    log.warn("RM1203 preStep 失败: {} code={}", preTranscode, preCode);
                    return error(preCode, "preStep " + preTranscode + " 失败: " + preResp.getOrDefault("msg", ""));
                }

                // 提取目标字段值
                Object extractedValue = extractField(preResp, preDataKey, extractField);

                if (extractedValue == null || (extractedValue instanceof String && ((String) extractedValue).isEmpty())) {
                    log.warn("RM1203 preStep 提取字段为空: {}.{}", preDataKey, extractField);
                    return error("9999", "preStep 无法提取 " + extractField + "，企业可能无此数据");
                }

                String extractedStr = String.valueOf(extractedValue);
                log.info("RM1203 preStep 提取完成: {}={}", useAsParam,
                        extractedStr.substring(0, Math.min(30, extractedStr.length())));

                // 构建最终参数（含extraParams + 提取值）
                Map<String, Object> finalParams = new LinkedHashMap<>();
                if (extraParams != null) finalParams.putAll(extraParams);
                finalParams.put(useAsParam, extractedStr);
                if ("name".equals(useAsParam)) {
                    finalParams.put("nameType", "2");
                }
                apiResp = apiClient.callWithParams(transcode, finalParams);

            } else {
                // 无 preStep：直接用 call() 走参数名适配
                Map<String, Object> effectiveExtra = (extraParams != null) ? extraParams : Collections.emptyMap();
                apiResp = apiClient.call(transcode, entName, "1", effectiveExtra);
            }

            String code = String.valueOf(apiResp.getOrDefault("code", "?"));
            if (!appConfig.getSuccessCode().equals(code)) {
                String msg = String.valueOf(apiResp.getOrDefault("msg", ""));
                log.warn("RM1203 630返回异常: code={} msg={}", code, msg);
                return error(code, msg);
            }

            // 提取data + 子表
            Object dataObj = apiResp.get("data");
            if (dataObj == null) {
                return error("9999", "630接口返回无数据");
            }

            Object resultData;
            if (dataKey != null && !dataKey.isEmpty()) {
                Map<String, Object> root = firstElement(dataObj);
                resultData = (root != null) ? root.getOrDefault(dataKey, Collections.emptyList()) : Collections.emptyList();
            } else {
                resultData = dataObj;
            }

            ArrayNode dataArray = toArrayNode(resultData);
            return new KnowledgeResponse(appConfig.getSuccessCode(), appConfig.getSuccessMsg(), dataArray);

        } catch (Exception e) {
            log.error("RM1203 调用失败: moduleCode={} transcode={}", moduleCode, transcode, e);
            return error("9999", "实时接口调用异常: " + e.getMessage());
        }
    }

    public List<Map<String, String>> listModuleCodes() {
        List<Map<String, String>> result = new ArrayList<>();
        for (Map.Entry<String, Map<String, Object>> e : mapping.entrySet()) {
            Map<String, String> item = new LinkedHashMap<>();
            item.put("moduleCode", e.getKey());
            item.put("name", (String) e.getValue().get("name"));
            item.put("transcode", (String) e.getValue().get("transcode"));
            result.add(item);
        }
        return result;
    }

    // ========== helpers ==========

    /** 从API响应中提取字段值（支持嵌套） */
    @SuppressWarnings("unchecked")
    private Object extractField(Map<String, Object> apiResp, String dataKey, String fieldName) {
        Object data = apiResp.get("data");
        if (data == null) return null;

        // 先找到目标对象
        Object target = data;
        if (dataKey != null && !dataKey.isEmpty()) {
            Map<String, Object> root = firstElement(data);
            if (root != null) {
                target = root.get(dataKey);
            }
        }

        // 数组取第一个元素
        if (target instanceof List) {
            List<?> list = (List<?>) target;
            if (!list.isEmpty()) {
                target = list.get(0);
            } else {
                return null;
            }
        }

        // 从对象中取字段
        if (target instanceof Map) {
            return ((Map<String, Object>) target).get(fieldName);
        }
        return null;
    }

    /** data数组取第一个元素 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> firstElement(Object dataObj) {
        if (dataObj instanceof List) {
            List<?> list = (List<?>) dataObj;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                return (Map<String, Object>) list.get(0);
            }
        }
        return null;
    }

    /** 任意对象 → ArrayNode */
    @SuppressWarnings("unchecked")
    private ArrayNode toArrayNode(Object obj) {
        ArrayNode arr = mapper.createArrayNode();
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                arr.add(mapper.valueToTree(item));
            }
        } else if (obj instanceof Map) {
            arr.add(mapper.valueToTree(obj));
        } else if (obj != null) {
            ObjectNode wrapper = mapper.createObjectNode();
            wrapper.put("data", String.valueOf(obj));
            arr.add(wrapper);
        }
        return arr;
    }

    private KnowledgeResponse error(String code, String msg) {
        return new KnowledgeResponse(code, msg, mapper.createArrayNode());
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
