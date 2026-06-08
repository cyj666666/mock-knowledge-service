package com.mock.knowledge.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 全局配置
 */
@Configuration
@ConfigurationProperties(prefix = "mock")
public class AppConfig {

    /** 离线数据包根目录，默认 jar 同级 offline-data/ */
    private String dataPath = "./offline-data";

    /** 统一返参 code */
    private String successCode = "0000";

    /** 统一返参 msg */
    private String successMsg = "数据响应正确!";

    /** 统一鉴权校验开关（false=不校验account/secretKey） */
    private boolean authCheck = false;

    /** 合法 account */
    private String validAccount;

    /** 合法 secretKey */
    private String validSecretKey;

    public String getDataPath() { return dataPath; }
    public void setDataPath(String dataPath) { this.dataPath = dataPath; }

    public String getSuccessCode() { return successCode; }
    public void setSuccessCode(String successCode) { this.successCode = successCode; }

    public String getSuccessMsg() { return successMsg; }
    public void setSuccessMsg(String successMsg) { this.successMsg = successMsg; }

    public boolean isAuthCheck() { return authCheck; }
    public void setAuthCheck(boolean authCheck) { this.authCheck = authCheck; }

    public String getValidAccount() { return validAccount; }
    public void setValidAccount(String validAccount) { this.validAccount = validAccount; }

    public String getValidSecretKey() { return validSecretKey; }
    public void setValidSecretKey(String validSecretKey) { this.validSecretKey = validSecretKey; }
}
