package com.mock.knowledge.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.knowledge.config.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 启动时加载 keymap.json 到内存，key → 相对路径（entName/moduleCode）
 */
@Service
public class KeymapLoader {

    private static final Logger log = LoggerFactory.getLogger(KeymapLoader.class);

    private final AppConfig appConfig;
    private final ObjectMapper objectMapper;

    /** key → entName/moduleCode */
    private Map<String, String> keymap = new ConcurrentHashMap<>();

    public KeymapLoader(AppConfig appConfig, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void load() {
        Path keymapFile = Paths.get(appConfig.getDataPath()).resolve("keymap.json");
        if (!Files.exists(keymapFile)) {
            log.warn("keymap.json 不存在: {}，RM1202 将无法匹配任何 key", keymapFile.toAbsolutePath());
            return;
        }
        try {
            byte[] bytes = Files.readAllBytes(keymapFile);
            String content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            Map<String, String> map = objectMapper.readValue(content,
                    new TypeReference<Map<String, String>>() {});
            keymap = new ConcurrentHashMap<>(map);
            log.info("keymap.json 加载完成，共 {} 条记录", keymap.size());
        } catch (IOException e) {
            log.error("keymap.json 解析失败: {}", e.getMessage());
            keymap = new ConcurrentHashMap<>();
        }
    }

    /**
     * 根据 key 获取相对路径
     */
    public String getPath(String key) {
        return keymap.get(key);
    }

    /**
     * 手动重载（不需要重启服务）
     */
    public void reload() {
        load();
    }

    public Map<String, String> getKeymap() {
        return Collections.unmodifiableMap(keymap);
    }
}
