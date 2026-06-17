package com.mock.knowledge.compare;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 630接口注册表：transcode → 接口名、参数模板、POC权限
 */
@Component
public class InterfaceRegistry {

    public static class Def {
        public final String code;
        public final String name;
        public final String nameType;
        public final boolean pocPerm;
        public final Map<String, Object> extraParams;

        Def(String code, String name, String nameType, boolean pocPerm) {
            this(code, name, nameType, pocPerm, null);
        }

        Def(String code, String name, String nameType, boolean pocPerm, Map<String, Object> extraParams) {
            this.code = code; this.name = name; this.nameType = nameType;
            this.pocPerm = pocPerm; this.extraParams = extraParams != null ? extraParams : Collections.emptyMap();
        }
    }

    private final List<Def> interfaces;

    public InterfaceRegistry() {
        List<Def> list = new ArrayList<>();

        list.add(new Def("R11103V3", "企业工商查询服务", "1", true,
                mkMap("nameType","1", "entType","8", "entStatus","1", "listName", Arrays.asList("basicList"))));
        list.add(new Def("R11Z03", "企业实际控制人信息", "1", true, mkMap("nameType","1")));
        list.add(new Def("R11C72", "企业最终受益人挖掘", "1", true));
        list.add(new Def("RA403", "高管履历信息", "1", true));
        list.add(new Def("R1618", "海关备案数据实时接口", "1", false));
        list.add(new Def("R1617", "资质数据实时查询接口", "1", false,
                mkMap("nameType","1","pageIndex",1,"pageSize",10)));
        list.add(new Def("RA1216", "营销名录企业查询", "1", true,
                mkMap("pageIndex",1,"pageSize",20)));
        list.add(new Def("R11101", "专利基本信息查询接口", "1", true,
                mkMap("pageIndex",1)));
        list.add(new Def("R1184", "软件著作权实时接口", "1", false,
                mkMap("pageIndex",1,"pageSize",10)));
        list.add(new Def("R1199", "商标基本信息查询接口", "1", true,
                mkMap("pageIndex",1,"pageSize",10)));
        list.add(new Def("R1167", "企业资金链图谱挖掘", "1", false,
                mkMap("labelSpecific", Arrays.asList("Enterprise","Person"), "query","all")));
        list.add(new Def("R227V2", "企业司法公告信息", "1", true));
        list.add(new Def("R217V2", "企业失信被执行人信息", "1", true,
                mkMap("isRelationHisName","true","NameType","1")));
        list.add(new Def("R4G04V2", "上市公司基础信息V2", "1", true));
        list.add(new Def("R11110", "企业招投标信息", "1", false, mkMap("pageIndex",1)));
        list.add(new Def("R115", "在建工程实时接口", "1", false,
                mkMap("pageIndex",1,"pageSize",10)));
        list.add(new Def("R241V2", "企业被执行人信息", "1", true,
                mkMap("nameType","1","isRelationHisName","true","pageIndex",1,"pageSize",10)));
        list.add(new Def("R243V2", "企业限制高消费信息", "1", true,
                mkMap("nameType","1","isRelationHisName","true","pageIndex",1,"pageSize",10,"isNeedHisData","true")));
        list.add(new Def("R1212V2", "企业行政处罚信息", "1", true, mkMap("pageIndex",1)));
        list.add(new Def("R255", "企业司法查封信息", "1", true,
                mkMap("queryType",Arrays.asList(0),"pageIndex",1,"pageSize",10)));
        list.add(new Def("R705V5", "企业税务-纳税信用排名信息", "1", true));
        list.add(new Def("R1201V3", "企业风险失信名单", "1", true));
        list.add(new Def("R426", "政策列表信息", "1", true,
                mkMap("field","printDate","order","asc","priority",1,"pageIndex",1,"pageSize",10)));
        list.add(new Def("R427", "政策详情信息", "1", true));
        list.add(new Def("R354V2", "企业舆情快讯V2", "1", true));
        list.add(new Def("R301V2", "企业风险舆情信息", "1", false, mkMap("IsContent","1")));
        list.add(new Def("R11133", "关联关系查询配置化", "1", false,
                mkMap("investRatio",0,"level",1)));
        list.add(new Def("R11Z02", "高管投资任职信息", "1", false));
        list.add(new Def("R4G05", "公众企业财务报表列表", "1", true,
                mkMap("pageSize",20,"pageIndex",1)));
        list.add(new Def("R4G06V2", "公众企业财务报表详情", "1", true));

        this.interfaces = Collections.unmodifiableList(list);
    }

    @SafeVarargs
    private static <K, V> Map<K, V> mkMap(Object... pairs) {
        Map<K, V> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked")
            K key = (K) pairs[i];
            @SuppressWarnings("unchecked")
            V val = (V) pairs[i + 1];
            m.put(key, val);
        }
        return m;
    }

    public List<Def> getAll() { return interfaces; }

    public List<Def> getPermitted() {
        List<Def> result = new ArrayList<>();
        for (Def d : interfaces) { if (d.pocPerm) result.add(d); }
        return result;
    }

    public Def getByCode(String code) {
        for (Def d : interfaces) { if (d.code.equals(code)) return d; }
        return null;
    }

    public List<Def> resolveFromDemand(String demandCode) {
        if (demandCode == null || demandCode.isEmpty()) return Collections.emptyList();
        String[] parts = demandCode.split("/");
        List<Def> result = new ArrayList<>();
        for (String part : parts) {
            Def def = getByCode(part.trim());
            if (def != null) result.add(def);
        }
        return result;
    }
}
