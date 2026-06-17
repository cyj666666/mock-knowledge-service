package com.mock.knowledge.compare;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

/**
 * 加载表字典JSON，提供 中文列名→英文字段名 映射
 * 字典JSON由 analyze_report.py 生成
 */
@Component
public class DictionaryService {

    private static final Logger log = LoggerFactory.getLogger(DictionaryService.class);
    private static final String DICT_JSON_PATH = "./offline-data/table_dict.json";

    private final ObjectMapper mapper;

    /** CN_normalized → EN_fieldName 全局映射 */
    private final Map<String, String> cnToEn = new LinkedHashMap<>();

    public DictionaryService(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @PostConstruct
    public void load() {
        java.nio.file.Path path = Paths.get(DICT_JSON_PATH);
        if (!Files.exists(path)) {
            log.warn("表字典JSON不存在: {}，CN→EN映射不可用", path.toAbsolutePath());
            return;
        }
        try {
            String json = new String(Files.readAllBytes(path), java.nio.charset.StandardCharsets.UTF_8);
            Map<String, Object> dict = mapper.readValue(json, new TypeReference<Map<String, Object>>() {});

            for (Map.Entry<String, Object> modEntry : dict.entrySet()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> subtables = (Map<String, Object>) modEntry.getValue();
                for (Map.Entry<String, Object> subEntry : subtables.entrySet()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fields = (Map<String, Object>) subEntry.getValue();
                    for (Map.Entry<String, Object> fEntry : fields.entrySet()) {
                        String enName = fEntry.getKey();
                        @SuppressWarnings("unchecked")
                        Map<String, Object> info = (Map<String, Object>) fEntry.getValue();
                        String cnName = String.valueOf(info.getOrDefault("cn", ""));
                        if (!cnName.isEmpty()) {
                            String cnKey = normalize(cnName);
                            if (!cnToEn.containsKey(cnKey)) {
                                cnToEn.put(cnKey, enName);
                            }
                        }
                    }
                }
            }
            log.info("字典加载完成: {} 个 CN→EN 映射", cnToEn.size());
        } catch (Exception e) {
            log.error("字典加载失败: {}", e.getMessage());
        }
    }

    /**
     * 中文名 → 英文名
     */
    public String toEn(String cnName) {
        if (cnName == null || cnName.isEmpty()) return null;
        // 精确
        String key = normalize(cnName);
        String en = cnToEn.get(key);
        if (en != null) return en;
        // 包含
        for (Map.Entry<String, String> e : cnToEn.entrySet()) {
            if (e.getKey().length() >= 2 && key.length() >= 2 &&
                (key.contains(e.getKey()) || e.getKey().contains(key))) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * 离线行 → 英文字段名Map
     */
    public Map<String, Object> normalizeOfflineRow(Map<String, Object> offlineRow) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : offlineRow.entrySet()) {
            String en = toEn(e.getKey());
            if (en != null) {
                result.put(en, e.getValue());
            } else {
                // 保留原文（可能是元数据列）
                result.put(e.getKey(), e.getValue());
            }
        }
        return result;
    }

    private static String normalize(String s) {
        return s.replaceAll("\\s+", "").replace("（","(").replace("）",")")
                .replace("：",":").replace("，",",").toLowerCase();
    }
}
