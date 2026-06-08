package com.mock.knowledge.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.mock.knowledge.config.AppConfig;
import com.mock.knowledge.model.KnowledgeRequest;
import com.mock.knowledge.model.KnowledgeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 核心路由：RM1201 → 直接读文件返回 / RM1202 → 查 keymap 读取文件返回
 */
@Service
public class RoutingService {

    private static final Logger log = LoggerFactory.getLogger(RoutingService.class);

    private final AppConfig appConfig;
    private final KeymapLoader keymapLoader;
    private final ObjectMapper objectMapper;

    public RoutingService(AppConfig appConfig, KeymapLoader keymapLoader, ObjectMapper objectMapper) {
        this.appConfig = appConfig;
        this.keymapLoader = keymapLoader;
        this.objectMapper = objectMapper;
    }

    /**
     * RM1201 发起接口
     * entName + moduleCode → RM1201.json
     */
    public KnowledgeResponse handleRM1201(KnowledgeRequest req) {
        String entName = req.getParams().getEntName();
        String moduleCode = req.getParams().getModuleCode();

        if (isBlank(entName) || isBlank(moduleCode)) {
            log.warn("RM1201 参数缺失: entName={}, moduleCode={}", entName, moduleCode);
            return error("1000", "请求参数不合法: entName/moduleCode 不能为空");
        }

        String safeName = sanitizePath(entName);
        Path filePath = Paths.get(appConfig.getDataPath())
                .resolve(safeName)
                .resolve(moduleCode)
                .resolve("RM1201.json");

        log.info("RM1201 查询: entName={}, moduleCode={}, 路径={}", entName, moduleCode, filePath);

        return readAndWrap(filePath);
    }

    /**
     * RM1202 内容获取接口
     * key → keymap 查路径 → RM1202.json
     */
    public KnowledgeResponse handleRM1202(KnowledgeRequest req) {
        String key = req.getParams().getKey();

        if (isBlank(key)) {
            log.warn("RM1202 参数缺失: key 为空");
            return error("1000", "请求参数不合法: key 不能为空");
        }

        String relativePath = keymapLoader.getPath(key);
        if (relativePath == null) {
            log.warn("RM1202 key 未注册: key={}", key);
            return error("9999", "无匹配数据");
        }

        Path filePath = Paths.get(appConfig.getDataPath())
                .resolve(relativePath)
                .resolve("RM1202.json");

        log.info("RM1202 查询: key={}, 路径={}", key, filePath);

        return readAndWrap(filePath);
    }

    // ────────────── 内部方法 ──────────────

    /**
     * 读取 JSON 文件，包装统一返参
     */
    private KnowledgeResponse readAndWrap(Path filePath) {
        if (!Files.exists(filePath)) {
            log.warn("文件不存在: {}", filePath.toAbsolutePath());
            return error("9999", "无匹配数据");
        }
        try {
            byte[] bytes = Files.readAllBytes(filePath);
            String content = new String(bytes, StandardCharsets.UTF_8);
            // 文件内容即 data[0]，包装成数组
            JsonNode dataNode = objectMapper.readTree(content);
            ArrayNode dataArray = objectMapper.createArrayNode();
            dataArray.add(dataNode);

            return new KnowledgeResponse(
                    appConfig.getSuccessCode(),
                    appConfig.getSuccessMsg(),
                    dataArray
            );
        } catch (IOException e) {
            log.error("文件读取失败: {}, error={}", filePath, e.getMessage());
            return error("9999", "内部错误: 数据文件解析失败");
        }
    }

    private KnowledgeResponse error(String code, String msg) {
        ArrayNode emptyData = objectMapper.createArrayNode();
        return new KnowledgeResponse(code, msg, emptyData);
    }

    /**
     * 企业名 → 安全目录名（替换文件系统非法字符为全角）
     */
    static String sanitizePath(String name) {
        if (name == null) return "";
        return name.replace("/", "／")
                   .replace("\\", "＼")
                   .replace(":", "：")
                   .replace("*", "＊")
                   .replace("?", "？")
                   .replace("\"", "＂")
                   .replace("<", "＜")
                   .replace(">", "＞")
                   .replace("|", "｜");
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
