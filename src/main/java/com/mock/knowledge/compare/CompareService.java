package com.mock.knowledge.compare;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mock.knowledge.compare.dto.CompareResponse;
import com.mock.knowledge.compare.dto.CompareResponse.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 核心对比：实时API(嵌套JSON) ↔ 离线xlsx(多sheet扁平)
 *
 * 流程：
 * 1. 调实时API → 得到 data[0] = {basicList:{...}, shareholderList:{...}, ...}
 * 2. 加载离线数据 → 得到 {sheet_name: [row_dict, ...]}
 * 3. 通过 subnode_mapping.json 将 API的JSON key 映射到 离线sheet名
 * 4. 对每对(key, sheet)，展开字段逐一对比（离线中文列名→英文 via 字典）
 */
@Service
public class CompareService {

    private static final Logger log = LoggerFactory.getLogger(CompareService.class);
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final InterfaceRegistry registry;
    private final RealtimeApiClient apiClient;
    private final OfflineDataLoader offlineLoader;
    private final DictionaryService dictService;
    private final ObjectMapper mapper;

    /** subnode → sheet中文名 映射 */
    private Map<String, Map<String, String>> subnodeMapping = new LinkedHashMap<>();

    public CompareService(InterfaceRegistry registry, RealtimeApiClient apiClient,
                          OfflineDataLoader offlineLoader, DictionaryService dictService,
                          ObjectMapper mapper) {
        this.registry = registry;
        this.apiClient = apiClient;
        this.offlineLoader = offlineLoader;
        this.dictService = dictService;
        this.mapper = mapper;
    }

    @PostConstruct
    public void loadMapping() {
        try {
            String json = new String(Files.readAllBytes(
                    Paths.get("./offline-data/subnode_mapping.json")),
                    java.nio.charset.StandardCharsets.UTF_8);
            subnodeMapping = mapper.readValue(json,
                    new TypeReference<Map<String, Map<String, String>>>() {});
            log.info("子表映射加载完成: {} 个接口", subnodeMapping.size());
        } catch (Exception e) {
            log.warn("子表映射加载失败: {}", e.getMessage());
        }
    }

    // ========== 入口 ==========

    public CompareResponse compare(String enterpriseName, List<String> filterInterfaces) {
        CompareResponse resp = new CompareResponse();
        resp.setEnterpriseName(enterpriseName);
        resp.setCompareTime(LocalDateTime.now().format(DTF));

        List<InterfaceRegistry.Def> defs = (filterInterfaces != null && !filterInterfaces.isEmpty())
                ? resolveDefs(filterInterfaces) : registry.getAll();

        // 加载离线数据
        Map<String, Map<String, List<Map<String, Object>>>> offlineData = offlineLoader.loadEnterprise(enterpriseName);

        List<InterfaceResult> results = new ArrayList<>();
        Summary sum = new Summary();
        sum.setTotalInterfaces(defs.size());

        for (InterfaceRegistry.Def def : defs) {
            InterfaceResult ir = compareOne(def, enterpriseName, offlineData);
            results.add(ir);

            if ("OK".equals(ir.getStatus())) {
                sum.setSuccessInterfaces(sum.getSuccessInterfaces() + 1);
            } else if ("NO_PERM".equals(ir.getStatus())) {
                sum.setNoPermissionInterfaces(sum.getNoPermissionInterfaces() + 1);
            } else {
                sum.setErrorInterfaces(sum.getErrorInterfaces() + 1);
            }
            sum.setTotalFieldsCompared(sum.getTotalFieldsCompared() + ir.getMatched()
                    + ir.getOnlyRealtime() + ir.getOnlyOffline() + ir.getValueMismatch());
            sum.setMatched(sum.getMatched() + ir.getMatched());
            sum.setOnlyRealtime(sum.getOnlyRealtime() + ir.getOnlyRealtime());
            sum.setOnlyOffline(sum.getOnlyOffline() + ir.getOnlyOffline());
            sum.setValueMismatch(sum.getValueMismatch() + ir.getValueMismatch());
        }

        resp.setSummary(sum);
        resp.setResults(results);
        return resp;
    }

    // ========== 单接口对比 ==========

    @SuppressWarnings("unchecked")
    private InterfaceResult compareOne(InterfaceRegistry.Def def, String enterpriseName,
                                        Map<String, Map<String, List<Map<String, Object>>>> offlineData) {
        InterfaceResult ir = new InterfaceResult();
        ir.setIfaceCode(def.code);
        ir.setIfaceName(def.name);

        if (!def.pocPerm) {
            ir.setStatus("NO_PERM");
            ir.setMessage("POC账号无此接口权限(模拟)");
            return ir;
        }

        // 1. 调实时接口
        Map<String, Object> apiResp;
        try {
            apiResp = apiClient.call(def.code, enterpriseName, def.nameType, def.extraParams);
        } catch (Exception e) {
            ir.setStatus("ERROR"); ir.setMessage(e.getMessage()); return ir;
        }

        String code = String.valueOf(apiResp.getOrDefault("code", "?"));
        if (!"0000".equals(code)) {
            ir.setStatus("API_ERROR");
            ir.setMessage("code=" + code + " " + apiResp.getOrDefault("msg", ""));
            return ir;
        }

        ir.setStatus("OK");

        // 2. 解析实时data
        Object dataObj = apiResp.get("data");
        Map<String, Object> rtRoot = null;
        if (dataObj instanceof List) {
            List<?> list = (List<?>) dataObj;
            if (!list.isEmpty() && list.get(0) instanceof Map) {
                rtRoot = (Map<String, Object>) list.get(0);
            }
        }

        // 3. 离线数据
        Map<String, List<Map<String, Object>>> offlineSheets = offlineData.get(def.code);

        if (rtRoot == null) {
            ir.setOfflineNote("实时无data");
            return ir;
        }
        if (offlineSheets == null || offlineSheets.isEmpty()) {
            ir.setOfflineNote("无离线数据");
            return ir;
        }

        // 4. 子表映射对比
        Map<String, String> mapping = subnodeMapping.getOrDefault(def.code, Collections.emptyMap());

        int totalMatched = 0, totalRt = 0, totalOff = 0, totalVd = 0;
        List<FieldDiff> allDiffs = new ArrayList<>();
        int pairsFound = 0;

        for (Map.Entry<String, String> entry : mapping.entrySet()) {
            String jsonKey = entry.getKey();        // e.g., "basicList"
            String offlineSheetName = entry.getValue(); // e.g., "企业照面信息"

            // 从实时JSON中取对应子表
            Object rtSub = rtRoot.get(jsonKey);
            if (rtSub == null) continue;

            // 从离线中找匹配的sheet
            List<Map<String, Object>> matchedOffRows = findSheet(offlineSheets, offlineSheetName);
            if (matchedOffRows == null || matchedOffRows.isEmpty()) continue;

            // 展开为行列表
            List<Map<String, Object>> rtRows = toRowList(rtSub);

            // 逐行对比（取第一行做详细对比）
            if (!rtRows.isEmpty() && !matchedOffRows.isEmpty()) {
                Map<String, Object> rtRow = rtRows.get(0);
                Map<String, Object> offRow = dictService.normalizeOfflineRow(matchedOffRows.get(0));

                DiffResult dr = compareMaps(rtRow, offRow, jsonKey + ".");
                totalMatched += dr.matched;
                totalRt += dr.onlyRealtime;
                totalOff += dr.onlyOffline;
                totalVd += dr.valueMismatch;
                allDiffs.addAll(dr.diffs);
                pairsFound++;
            }
        }

        // 如果映射没覆盖，fallback：用实时data[0]直接对比离线第一个sheet第一行
        if (pairsFound == 0 && !offlineSheets.isEmpty()) {
            String firstSheet = offlineSheets.keySet().iterator().next();
            List<Map<String, Object>> offRows = offlineSheets.get(firstSheet);
            if (!offRows.isEmpty()) {
                Map<String, Object> offRow = dictService.normalizeOfflineRow(offRows.get(0));
                DiffResult dr = compareMaps(rtRoot, offRow, "");
                totalMatched += dr.matched;
                totalRt += dr.onlyRealtime;
                totalOff += dr.onlyOffline;
                totalVd += dr.valueMismatch;
                allDiffs.addAll(dr.diffs);
                pairsFound = 1;
            }
        }

        ir.setMatched(totalMatched);
        ir.setOnlyRealtime(totalRt);
        ir.setOnlyOffline(totalOff);
        ir.setValueMismatch(totalVd);
        ir.setDiffs(allDiffs);
        ir.setOfflineNote(String.format("%d sheets, %d subtable pairs matched", offlineSheets.size(), pairsFound));
        return ir;
    }

    // ========== helpers ==========

    /** 在离线sheet中找匹配名称的 */
    private List<Map<String, Object>> findSheet(Map<String, List<Map<String, Object>>> sheets, String targetName) {
        // 精确
        if (sheets.containsKey(targetName)) return sheets.get(targetName);
        // 模糊
        for (Map.Entry<String, List<Map<String, Object>>> e : sheets.entrySet()) {
            if (e.getKey().contains(targetName) || targetName.contains(e.getKey())) {
                return e.getValue();
            }
        }
        return null;
    }

    /** JSON对象或数组 → List<Map> */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> toRowList(Object obj) {
        if (obj instanceof List) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<?>) obj) {
                if (item instanceof Map) result.add((Map<String, Object>) item);
            }
            return result;
        }
        if (obj instanceof Map) {
            return Collections.singletonList((Map<String, Object>) obj);
        }
        return Collections.emptyList();
    }

    /** 两个Map逐字段对比 */
    static class DiffResult {
        int matched, onlyRealtime, onlyOffline, valueMismatch;
        List<FieldDiff> diffs = new ArrayList<>();
    }

    private DiffResult compareMaps(Map<String, Object> rt, Map<String, Object> off, String prefix) {
        DiffResult dr = new DiffResult();
        if (rt == null && off == null) return dr;
        if (rt == null) { dr.onlyOffline++; return dr; }
        if (off == null) { dr.onlyRealtime++; return dr; }

        // 跳过元数据
        Set<String> skipKeys = new HashSet<>(Arrays.asList(
                "code", "msg", "totalCount", "totalPage", "pageSize", "pageIndex",
                "请求主体名称", "序号", "首次入库日期", "更新日期", "记录唯一标识", "原始查询关键词"));

        // 建 lower-case key → original-key 索引，实现大小写不敏感的key匹配
        Map<String, String> rtLowerKeys = new LinkedHashMap<>();
        for (String k : rt.keySet()) {
            if (!skipKeys.contains(k)) rtLowerKeys.putIfAbsent(k.toLowerCase(), k);
        }
        Map<String, String> offLowerKeys = new LinkedHashMap<>();
        for (String k : off.keySet()) {
            if (!skipKeys.contains(k)) offLowerKeys.putIfAbsent(k.toLowerCase(), k);
        }

        Set<String> allLowerKeys = new LinkedHashSet<>();
        allLowerKeys.addAll(rtLowerKeys.keySet());
        allLowerKeys.addAll(offLowerKeys.keySet());

        for (String lk : allLowerKeys) {
            String rtOrigKey = rtLowerKeys.get(lk);
            String offOrigKey = offLowerKeys.get(lk);
            // 显示名用实时key优先
            String displayKey = rtOrigKey != null ? rtOrigKey : offOrigKey;
            String fullKey = prefix.isEmpty() ? displayKey : prefix + displayKey;

            Object rtVal = rtOrigKey != null ? rt.get(rtOrigKey) : null;
            Object offVal = offOrigKey != null ? off.get(offOrigKey) : null;

            if (rtVal == null && offVal == null) continue;

            String rtStr = rtVal != null ? stringify(rtVal) : null;
            String offStr = offVal != null ? stringify(offVal) : null;

            if (rtStr == null) {
                dr.onlyOffline++;
                dr.diffs.add(makeDiff(fullKey, "-", truncate(offStr), "OFFLINE_ONLY"));
            } else if (offStr == null) {
                dr.onlyRealtime++;
                dr.diffs.add(makeDiff(fullKey, truncate(rtStr), "-", "REALTIME_ONLY"));
            } else if (rtStr.equals(offStr)) {
                dr.matched++;
            } else if (isStringOnly(rtVal) && isStringOnly(offVal) && rtStr.equalsIgnoreCase(offStr)) {
                // 纯字符串值忽略大小写
                dr.matched++;
            } else {
                dr.valueMismatch++;
                dr.diffs.add(makeDiff(fullKey, truncate(rtStr), truncate(offStr), "VALUE_DIFF"));
            }
        }
        return dr;
    }

    private boolean isStringOnly(Object obj) {
        return obj instanceof String && !((String) obj).startsWith("{") && !((String) obj).startsWith("[");
    }

    private String stringify(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String) return (String) obj;
        if (obj instanceof Number || obj instanceof Boolean) return String.valueOf(obj);
        try { return mapper.writeValueAsString(obj); } catch (Exception e) { return String.valueOf(obj); }
    }

    private String truncate(String s) {
        return s != null && s.length() > 150 ? s.substring(0, 147) + "..." : s;
    }

    private FieldDiff makeDiff(String field, String rt, String off, String type) {
        FieldDiff fd = new FieldDiff();
        fd.setField(field); fd.setRealtime(rt); fd.setOffline(off); fd.setDiffType(type);
        return fd;
    }

    private List<InterfaceRegistry.Def> resolveDefs(List<String> codes) {
        List<InterfaceRegistry.Def> result = new ArrayList<>();
        for (String c : codes) {
            List<InterfaceRegistry.Def> resolved = registry.resolveFromDemand(c);
            result.addAll(resolved);
        }
        if (result.isEmpty()) return registry.getAll();
        return result;
    }
}
