package com.mock.knowledge.compare;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.*;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * 调用630生产环境接口 (HttpURLConnection，精确控制Content-Type)
 */
@Component
public class RealtimeApiClient {

    private static final Logger log = LoggerFactory.getLogger(RealtimeApiClient.class);
    private static final String PROD_URL = "https://hub.amardata.com/credit/api/gateway";

    private final ObjectMapper mapper;

    static {
        // 信任所有SSL证书
        try {
            TrustManager[] trustAll = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAll, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);
        } catch (Exception e) {
            log.error("SSL init failed", e);
        }
    }

    public RealtimeApiClient(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * 直接传完整 params（用于二步调用等场景，跳过企业名自动适配）
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> callWithParams(String transcode, Map<String, Object> params) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("transcode", transcode);
            body.put("source", "EDS");
            body.put("userid", "EDS");
            body.put("orgid", "EDS");
            body.put("account", "POC");
            body.put("params", params != null ? params : Collections.emptyMap());

            return doPost(body, transcode);
        } catch (Exception e) {
            log.error("{} callWithParams failed: {}", transcode, e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", "ERROR");
            err.put("msg", e.getMessage());
            return err;
        }
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> call(String transcode, String enterpriseName, String nameType,
                                     Map<String, Object> extraParams) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("transcode", transcode);
            body.put("source", "EDS");
            body.put("userid", "EDS");
            body.put("orgid", "EDS");
            body.put("account", "POC");

            Map<String, Object> params = new LinkedHashMap<>(extraParams != null ? extraParams : Collections.emptyMap());
            adjustParams(transcode, params, enterpriseName);
            body.put("params", params);
            return doPost(body, transcode);
        } catch (Exception e) {
            log.error("{} call failed: {}", transcode, e.getMessage());
            Map<String, Object> err = new LinkedHashMap<>();
            err.put("code", "ERROR"); err.put("msg", e.getMessage());
            return err;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> doPost(Map<String, Object> body, String transcode) throws Exception {
        String jsonBody = mapper.writeValueAsString(body);
        log.debug(">> {} body.len={}", transcode, jsonBody.length());

        URL url = new URL(PROD_URL);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.setConnectTimeout(30000);
        conn.setReadTimeout(30000);
        conn.setRequestProperty("Content-Type", "application/json;charset=utf-8");
        conn.setRequestProperty("Accept", "application/json");

        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(bytes);
            os.flush();
        }

        int status = conn.getResponseCode();
        String responseBody;
        if (status >= 200 && status < 300) {
            responseBody = readStream(conn.getInputStream());
        } else {
            responseBody = readStream(conn.getErrorStream());
        }

        Map<String, Object> result = mapper.readValue(responseBody, Map.class);
        String code = String.valueOf(result.getOrDefault("code", "?"));
        log.info("<< {} code={}", transcode, code);
        return result;
    }

    private String readStream(java.io.InputStream is) {
        try (Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A")) {
            return s.hasNext() ? s.next() : "";
        }
    }

    private void adjustParams(String transcode, Map<String, Object> params, String entName) {
        // 默认加 nameType + name
        String nameType = "1";
        // 移除默认参数，由各接口自行设定
        if (params.containsKey("nameType")) nameType = String.valueOf(params.get("nameType"));

        // 统一清理
        params.remove("nameType"); params.remove("name");

        switch (transcode) {
            case "R11C72": params.put("entname", entName); break;
            case "RA403": params.put("companyName", entName); break;
            case "R705V5": params.put("TaxPayerName", entName); break;
            case "R4G04V2": params.put("name", entName); params.put("nameType", nameType); break;
            case "R4G05": params.put("entName", entName); break;
            case "R1201V3": params.put("entName", entName); break;
            case "R354V2": params.put("entName", entName); break;
            case "R11101": case "R1199": params.put("applicantName", entName); break;
            case "R1184": params.put("entName", entName); break;
            case "R1617": case "R1618": params.put("entName", entName); break;
            case "R255": params.put("entName", entName); break;
            case "R11Z02": params.put("entName", entName); break;
            case "R301V2": params.put("EntName", entName); break;
            case "R11110": params.put("relatedName", entName); break;
            case "R115": params.put("prjName", entName); break;
            case "R11133": params.put("entName", entName); break;
            case "R227V2": params.put("entname", entName); break;
            case "R217V2": params.put("name", entName); params.put("NameType", nameType); break;
            case "R241V2": case "R243V2":
                params.put("name", entName); params.put("nameType", nameType); break;
            case "R1167": params.put("entName", entName); break;
            default:
                params.put("name", entName); params.put("nameType", nameType);
        }
    }
}
