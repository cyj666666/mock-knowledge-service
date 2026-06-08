package com.mock.knowledge.model;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * 知识服务统一返回体（保持原格式 code + msg + data）
 */
public class KnowledgeResponse {

    private String code;
    private String msg;
    private JsonNode data;

    public KnowledgeResponse() {}

    public KnowledgeResponse(String code, String msg, JsonNode data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getMsg() { return msg; }
    public void setMsg(String msg) { this.msg = msg; }

    public JsonNode getData() { return data; }
    public void setData(JsonNode data) { this.data = data; }
}
