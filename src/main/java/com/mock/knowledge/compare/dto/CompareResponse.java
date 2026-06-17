package com.mock.knowledge.compare.dto;

import java.util.List;
import java.util.Map;

/**
 * 630接口对比响应
 */
public class CompareResponse {

    private String enterpriseName;
    private String compareTime;
    private Summary summary;
    private List<InterfaceResult> results;

    public String getEnterpriseName() { return enterpriseName; }
    public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }

    public String getCompareTime() { return compareTime; }
    public void setCompareTime(String compareTime) { this.compareTime = compareTime; }

    public Summary getSummary() { return summary; }
    public void setSummary(Summary summary) { this.summary = summary; }

    public List<InterfaceResult> getResults() { return results; }
    public void setResults(List<InterfaceResult> results) { this.results = results; }

    // ---- inner ----

    public static class Summary {
        private int totalInterfaces;
        private int successInterfaces;
        private int noPermissionInterfaces;
        private int errorInterfaces;
        private int totalFieldsCompared;
        private int matched;
        private int onlyRealtime;
        private int onlyOffline;
        private int valueMismatch;

        public int getTotalInterfaces() { return totalInterfaces; }
        public void setTotalInterfaces(int totalInterfaces) { this.totalInterfaces = totalInterfaces; }
        public int getSuccessInterfaces() { return successInterfaces; }
        public void setSuccessInterfaces(int successInterfaces) { this.successInterfaces = successInterfaces; }
        public int getNoPermissionInterfaces() { return noPermissionInterfaces; }
        public void setNoPermissionInterfaces(int noPermissionInterfaces) { this.noPermissionInterfaces = noPermissionInterfaces; }
        public int getErrorInterfaces() { return errorInterfaces; }
        public void setErrorInterfaces(int errorInterfaces) { this.errorInterfaces = errorInterfaces; }
        public int getTotalFieldsCompared() { return totalFieldsCompared; }
        public void setTotalFieldsCompared(int totalFieldsCompared) { this.totalFieldsCompared = totalFieldsCompared; }
        public int getMatched() { return matched; }
        public void setMatched(int matched) { this.matched = matched; }
        public int getOnlyRealtime() { return onlyRealtime; }
        public void setOnlyRealtime(int onlyRealtime) { this.onlyRealtime = onlyRealtime; }
        public int getOnlyOffline() { return onlyOffline; }
        public void setOnlyOffline(int onlyOffline) { this.onlyOffline = onlyOffline; }
        public int getValueMismatch() { return valueMismatch; }
        public void setValueMismatch(int valueMismatch) { this.valueMismatch = valueMismatch; }
    }

    public static class InterfaceResult {
        private String ifaceCode;
        private String ifaceName;
        /** OK / NO_PERM / ERROR */
        private String status;
        private String message;
        private int matched;
        private int onlyRealtime;
        private int onlyOffline;
        private int valueMismatch;
        private List<FieldDiff> diffs;
        /** 实时接口原始响应 */
        private Map<String, Object> realtimeData;
        /** 科大讯飞离线数据概要 */
        private String offlineNote;

        public String getIfaceCode() { return ifaceCode; }
        public void setIfaceCode(String ifaceCode) { this.ifaceCode = ifaceCode; }
        public String getIfaceName() { return ifaceName; }
        public void setIfaceName(String ifaceName) { this.ifaceName = ifaceName; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getMatched() { return matched; }
        public void setMatched(int matched) { this.matched = matched; }
        public int getOnlyRealtime() { return onlyRealtime; }
        public void setOnlyRealtime(int onlyRealtime) { this.onlyRealtime = onlyRealtime; }
        public int getOnlyOffline() { return onlyOffline; }
        public void setOnlyOffline(int onlyOffline) { this.onlyOffline = onlyOffline; }
        public int getValueMismatch() { return valueMismatch; }
        public void setValueMismatch(int valueMismatch) { this.valueMismatch = valueMismatch; }
        public List<FieldDiff> getDiffs() { return diffs; }
        public void setDiffs(List<FieldDiff> diffs) { this.diffs = diffs; }
        public Map<String, Object> getRealtimeData() { return realtimeData; }
        public void setRealtimeData(Map<String, Object> realtimeData) { this.realtimeData = realtimeData; }
        public String getOfflineNote() { return offlineNote; }
        public void setOfflineNote(String offlineNote) { this.offlineNote = offlineNote; }
    }

    public static class FieldDiff {
        private String field;
        private String realtime;
        private String offline;
        private String diffType; // MATCH / REALTIME_ONLY / OFFLINE_ONLY / VALUE_DIFF

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getRealtime() { return realtime; }
        public void setRealtime(String realtime) { this.realtime = realtime; }
        public String getOffline() { return offline; }
        public void setOffline(String offline) { this.offline = offline; }
        public String getDiffType() { return diffType; }
        public void setDiffType(String diffType) { this.diffType = diffType; }
    }
}
