# Test Report: Knowledge Service Mock API

**Test Date:** 2026-06-08
**API Endpoint:** `POST http://10.3.10.200:18080/api/knowledge`
**Server:** mock-knowledge-service (Spring Boot 2.7.18)

---

## Summary

| # | Test Case | transcode | Expected code | Actual code | Result |
|---|-----------|-----------|---------------|-------------|--------|
| C1 | RM1201 - Enterprise Info Query (Normal) | RM1201 | 0000 | 0000 | PASS |
| C2 | RM1201 - Industry Overview Init (Normal) | RM1201 | 0000 | 0000 | PASS |
| C3 | RM1202 - Content Retrieval with Key (Normal) | RM1202 | 0000 | 0000 | PASS |
| C4 | RM1201 - Enterprise Not Found (Error) | RM1201 | 9999 | 9999 | PASS |
| C5 | RM1202 - Invalid Key (Error) | RM1202 | 9999 | 9999 | PASS |

**Total: 5 | Passed: 5 | Failed: 0**

---

## C1: RM1201 - Enterprise Info Query (Normal)

> **Result:** PASS | code=`0000` | expected=`0000`

### Request

```json
{
    "account": "TEST_ACCOUNT_00000000000000000000000000000000",
    "secretKey": "TEST_SECRET_0000000000000000000000000000000000000000000000000000000000000000",
    "transcode": "RM1201",
    "userid": "EDS",
    "orgid": "EDS",
    "source": "EDS",
    "params": {
        "entName": "北大方正集团有限公司",
        "moduleCode": "Service-BusinessBasicList-Data-Customer-POC",
        "stream": "Y",
        "reqParams": {
            "nameType": "1",
            "indexFlag": true
        }
    }
}
```

### Response

- **key:** `eb8f7479bd2ff109fbbdeb100bf3d83d`
- **prompt:** 
- **basicList entries:** 1
- **Fields per entry:** 43
- **Sample fields:** enterpriseName=北大方正集团有限公司, creditCode=911100001019838370, enterpriseStatus=存续, regCap=1000000

```json
{
    "code": "0000",
    "msg": "数据响应正确!",
    "data": [
        {
            "key": "eb8f7479bd2ff109fbbdeb100bf3d83d",
            "prompt": "",
            "inputIndexContent": {
                "basicList": [
                    {
                        "regNo": "450000000004866",
                        "cancelDate": "",
                        "townCode": "",
                        "revokeDate": "",
                        "regOrgDistrict": "海淀区",
                        "frName": "刘建",
                        "industryCode": "6621",
                        "regOrgCode": "450108",
                        "creditCode": "911100001019838370",
                        "regCapCur": "人民币元",
                        "empNum": "5796",
                        "orgCode": "198376184",
                        "regOrgCity": "北京市",
                        "enterpriseStatus": "存续",
                        "regCap": "1000000",
                        "industryPhyCode": "J",
                        "enterpriseName": "北大方正集团有限公司",
                        "isBoard": "是",
                        "email": "founder@founder.com",
                        "industryName": "商业银行服务",
                        "industryPhyName": "金融业",
                        "holdUnit": "",
                        "address": "北京市海淀区成府路298号方正大厦",
                        "lastAnnCheckDate": "2025-06-09",
                        "town": "",
                        "openTo": "长期",
                        "enterpriseEngName": "Peking University Founder Group Co., Ltd.",
                        "recCap": "1000000",
                        "oprPlace": "",
                        "entScale": "大型",
                        "telephone": "010-82531188",
                        "abuItem": "吸收公众存款；发放短期、中期和长期贷款；办理国内结算；办理票据承兑贴现；发行金融债券；代理发行、代理兑付、承销政府债券；买卖政府债券；从事同业拆借；提供担保；代理收付款项及代理保险业务；提供保管箱业务；经中国银行业监督管理部门批准的其他业务。（依法须经批准的项目，经相关部门批准后方可开展经营活动。）",
                        "apprDate": "2026-01-29",
                        "enterpriseType": "其他股份有限公司(非上市)",
                        "insurePerCnt": "5796",
                        "openFrom": "1997-05-27",
                        "esDate": "1997-05-27",
                        "anCheYear": "2024",
                        "operateScope": "吸收公众存款；发放短期、中期和长期贷款；办理国内结算；办理票据承兑贴现；发行金融债券；代理发行、代理兑付、承销政府债券；买卖政府债券；从事同业拆借；提供担保；代理收付款项及代理保险业务；提供保管箱业务；经中国银行业监督管理部门批准的其他业务。（依法须经批准的项目，经相关部门批准后方可开展经营活动。）",
                        "regOrg": "北京市市场监督管理局",
                        "addrDistric
// ... (truncated, total 2705 chars)
```

## C2: RM1201 - Industry Overview Init (Normal)

> **Result:** PASS | code=`0000` | expected=`0000`

### Request

```json
{
    "account": "TEST_ACCOUNT_00000000000000000000000000000000",
    "secretKey": "TEST_SECRET_0000000000000000000000000000000000000000000000000000000000000000",
    "transcode": "RM1201",
    "userid": "EDS",
    "orgid": "EDS",
    "source": "EDS",
    "params": {
        "entName": "北大方正集团有限公司",
        "moduleCode": "Industry-Overview-Data-Customer-POC",
        "stream": "Y",
        "reqParams": {
            "industry": "建筑业",
            "prov": "江苏省",
            "item": [
                "行业概况"
            ]
        }
    }
}
```

### Response

- **key:** `a1b2c3d4e5f6789012345678abcdef01`
- **prompt:** 根据建筑行业在江苏省的发展情况分析，得出以下行业概况
- **inputIndexContent:** `{}`

```json
{
    "code": "0000",
    "msg": "数据响应正确!",
    "data": [
        {
            "key": "a1b2c3d4e5f6789012345678abcdef01",
            "prompt": "根据建筑行业在江苏省的发展情况分析，得出以下行业概况",
            "inputIndexContent": {}
        }
    ]
}
```

## C3: RM1202 - Content Retrieval with Key (Normal)

> **Result:** PASS | code=`0000` | expected=`0000`

### Request

```json
{
    "account": "TEST_ACCOUNT_00000000000000000000000000000000",
    "secretKey": "TEST_SECRET_0000000000000000000000000000000000000000000000000000000000000000",
    "transcode": "RM1202",
    "userid": "EDS",
    "orgid": "EDS",
    "source": "EDS",
    "params": {
        "key": "a1b2c3d4e5f6789012345678abcdef01"
    }
}
```

### Response

- **isFinish:** `Y`
- **content length:** 509 chars

```json
{
    "code": "0000",
    "msg": "数据响应正确!",
    "data": [
        {
            "isFinish": "Y",
            "content": "江苏省建筑业行业概况分析：\n\n一、行业基本情况\n江苏省作为全国建筑业大省，建筑业总产值连续多年位居全国前列。2025年全省建筑业总产值突破4.5万亿元，同比增长约5.8%。行业增加值占全省GDP比重稳定在6%以上，是江苏省国民经济的重要支柱产业之一。\n\n二、市场规模与结构\n目前江苏省拥有特级资质企业超过100家，一级资质企业超过2000家。行业集中度较高，前50强企业总产值占比超过全省的40%。南通、扬州、泰州三市为传统建筑强市，贡献了全省超过50%的建筑业产值。\n\n三、政策环境\n江苏省持续推进建筑业转型升级，出台了《关于促进建筑业高质量发展的实施意见》等政策文件，重点支持装配式建筑、绿色建筑和建筑产业现代化发展。\n\n四、发展趋势\n1. 数字化、智能化转型加速，BIM技术应用率超过60%\n2. 装配式建筑占比持续提升，2025年新建建筑中装配式占比达到40%\n3. 绿色建筑认证面积稳步增长，绿色建筑占新建建筑比例超过90%\n4. 企业“走出去”步伐加快，省外产值占比超过45%\n\n五、风险与挑战\n主要面临原材料价格波动、劳动力成本上升、行业利润空间收窄等挑战。同时房地产调控政策对住宅建筑需求产生一定影响。"
        }
    ]
}
```

## C4: RM1201 - Enterprise Not Found (Error)

> **Result:** PASS | code=`9999` | expected=`9999`

### Request

```json
{
    "account": "TEST_ACCOUNT_00000000000000000000000000000000",
    "secretKey": "TEST_SECRET_0000000000000000000000000000000000000000000000000000000000000000",
    "transcode": "RM1201",
    "userid": "EDS",
    "orgid": "EDS",
    "source": "EDS",
    "params": {
        "entName": "不存在的企业名称有限公司",
        "moduleCode": "Service-BusinessBasicList-Data-Customer-POC",
        "stream": "Y",
        "reqParams": {
            "nameType": "1",
            "indexFlag": true
        }
    }
}
```

### Response

```json
{
    "code": "9999",
    "msg": "无匹配数据",
    "data": []
}
```

## C5: RM1202 - Invalid Key (Error)

> **Result:** PASS | code=`9999` | expected=`9999`

### Request

```json
{
    "account": "TEST_ACCOUNT_00000000000000000000000000000000",
    "secretKey": "TEST_SECRET_0000000000000000000000000000000000000000000000000000000000000000",
    "transcode": "RM1202",
    "userid": "EDS",
    "orgid": "EDS",
    "source": "EDS",
    "params": {
        "key": "invalid-key-12345"
    }
}
```

### Response

```json
{
    "code": "9999",
    "msg": "无匹配数据",
    "data": []
}
```

---

## Data Flow Diagram

```
Client Request
    |
    |-- RM1201 (initiate)
    |      |
    |      +-- Indicator type (indexFlag=true)
    |      |      |-> returns data directly in inputIndexContent
    |      |      |-> NO need to call RM1202
    |      |
    |      +-- Content Analysis type (no indexFlag)
    |             |-> returns key + prompt
    |             |-> MUST call RM1202 to get content
    |
    |-- RM1202 (retrieve)
           |
           |-> key lookup in keymap.json
           |-> returns content + isFinish flag
```

---

## File Structure (Data Mapping)

```
offline-data/
  +-- keymap.json                    # key -> entName/moduleCode
  +-- {entName}/                      # enterprise name as directory
       +-- {moduleCode}/              # module code as subdirectory
            +-- RM1201.json           # data[0] for RM1201 response
            +-- RM1202.json           # data[0] for RM1202 response
```

> Note: RM1202.json only exists for content-analysis type (non-indicator).
> Indicator type returns all data in RM1201.json, no RM1202 needed.