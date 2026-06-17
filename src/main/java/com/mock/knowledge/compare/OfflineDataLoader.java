package com.mock.knowledge.compare;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 加载科大讯飞离线JSON数据（由convert_xlsx_to_json.py预生成）
 */
@Component
public class OfflineDataLoader {

    private static final Logger log = LoggerFactory.getLogger(OfflineDataLoader.class);
    private static final String DATA_DIR = "./offline-data/iflytek";

    private final ObjectMapper mapper;

    public OfflineDataLoader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 获取某企业的所有离线接口数据
     * @return {transcode: {sheet_name: [row_dict,...]}}
     */
    public Map<String, Map<String, List<Map<String, Object>>>> loadEnterprise(String enterpriseName) {
        Map<String, Map<String, List<Map<String, Object>>>> result = new LinkedHashMap<>();
        Path entDir = Paths.get(DATA_DIR, enterpriseName);
        if (!Files.exists(entDir)) {
            log.warn("企业离线数据目录不存在: {}", entDir.toAbsolutePath());
            return result;
        }

        File[] files = entDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        if (files == null) return result;

        for (File file : files) {
            String transcode = file.getName().replace(".json", "");
            try {
                String content = new String(Files.readAllBytes(file.toPath()), java.nio.charset.StandardCharsets.UTF_8);
                Map<String, List<Map<String, Object>>> sheetData = mapper.readValue(content,
                        new TypeReference<Map<String, List<Map<String, Object>>>>() {});
                result.put(transcode, sheetData);
            } catch (Exception e) {
                log.error("加载{}失败: {}", file.getName(), e.getMessage());
            }
        }
        return result;
    }

    /**
     * 获取可用企业列表
     */
    public List<String> listEnterprises() {
        List<String> list = new ArrayList<>();
        Path baseDir = Paths.get(DATA_DIR);
        File[] dirs = baseDir.toFile().listFiles(File::isDirectory);
        if (dirs != null) {
            for (File dir : dirs) {
                if (!dir.getName().startsWith("_")) {
                    list.add(dir.getName());
                }
            }
        }
        return list;
    }

    /**
     * 获取某企业某接口的离线数据
     */
    public Map<String, List<Map<String, Object>>> loadInterface(String enterpriseName, String transcode) {
        Path filePath = Paths.get(DATA_DIR, enterpriseName, transcode.replace("/", "_") + ".json");
        if (!Files.exists(filePath)) {
            return null;
        }
        try {
            String content = new String(Files.readAllBytes(filePath), java.nio.charset.StandardCharsets.UTF_8);
            return mapper.readValue(content, new TypeReference<Map<String, List<Map<String, Object>>>>() {});
        } catch (Exception e) {
            log.error("加载 {}/{} 失败: {}", enterpriseName, transcode, e.getMessage());
            return null;
        }
    }
}
