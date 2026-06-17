package com.mock.knowledge.compare.dto;

import java.util.List;

/**
 * 630接口对比请求
 */
public class CompareRequest {

    /** 企业名称 */
    private String enterpriseName;

    /** 查询关键字类型: 1-企业名 2-统一社会信用代码 3-工商注册号 */
    private String nameType = "1";

    /** 指定接口列表，为空则跑全部有权限的 */
    private List<String> interfaces;

    public String getEnterpriseName() { return enterpriseName; }
    public void setEnterpriseName(String enterpriseName) { this.enterpriseName = enterpriseName; }

    public String getNameType() { return nameType; }
    public void setNameType(String nameType) { this.nameType = nameType; }

    public List<String> getInterfaces() { return interfaces; }
    public void setInterfaces(List<String> interfaces) { this.interfaces = interfaces; }
}
