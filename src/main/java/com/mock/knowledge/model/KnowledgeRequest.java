package com.mock.knowledge.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * 知识服务统一请求体
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class KnowledgeRequest {

    private String account;
    private String secretKey;
    private String transcode;
    private String userid;
    private String orgid;
    private String source;
    private Params params;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }

    public String getTranscode() { return transcode; }
    public void setTranscode(String transcode) { this.transcode = transcode; }

    public String getUserid() { return userid; }
    public void setUserid(String userid) { this.userid = userid; }

    public String getOrgid() { return orgid; }
    public void setOrgid(String orgid) { this.orgid = orgid; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public Params getParams() { return params; }
    public void setParams(Params params) { this.params = params; }

    /**
     * params 内层
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Params {
        private String entName;
        private String moduleCode;
        private String stream;
        private String key;
        private Map<String, Object> reqParams;

        public String getEntName() { return entName; }
        public void setEntName(String entName) { this.entName = entName; }

        public String getModuleCode() { return moduleCode; }
        public void setModuleCode(String moduleCode) { this.moduleCode = moduleCode; }

        public String getStream() { return stream; }
        public void setStream(String stream) { this.stream = stream; }

        public String getKey() { return key; }
        public void setKey(String key) { this.key = key; }

        public Map<String, Object> getReqParams() { return reqParams; }
        public void setReqParams(Map<String, Object> reqParams) { this.reqParams = reqParams; }
    }
}
