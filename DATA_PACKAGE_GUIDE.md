# 离线数据包生成指南

> 下次有新数据包，把这篇文档 + 输入文件给我，一句话就能开工。

---

## 提需求模板

```
帮我生成新的离线数据包，输入材料如下：

1. 【原始数据】          datajson.json    —— 数据库导出的批次调用数据
2. 【接口映射文档】       xxx.xlsx         —— Excel，含 B客户模块/C分块/D模块/E数据内容/I接口编号 列
3. 【接口文档目录】       接口文档/         —— PDF文件，文件名格式：{中文名}（{transcode}）.pdf
4. 【目标工程】          mock-knowledge-service/
```

---

## 处理流程（5 步）

### Step 1：拆分原始数据，追溯 hash 调用链

**输入：** `datajson.json`

**处理：**
1. 解析顶层 key 下的数组，每条记录有 `transcode`、`reqKey`、`jsondata`
2. reqKey 分两类：
   - **中文名**（企业名、行业分类）→ 直接调用
   - **hash 值**（30-32 位 hex）→ 由上游接口返回，被下游接口消费
3. 对每个 hash 值，在所有记录中搜索它的 jsondata，找到"谁产出了这个 hash"→ 确定上游接口
4. 递归追踪 R427 自循环链，直到所有 hash 追溯到命名 reqKey

**输出：** 按企业名拆分的目录，每个企业下按 `{transcode}__{reqKey}__{来源}.json` 命名

### Step 2：确定企业 → 行业映射

**输入：** Step 1 的企业数据 + Excel 文档

**处理：**
1. 从 R11103V3 或类似的工商信息接口的 `basicList` 中提取 `industryPhyName` 和 `industryName`
2. 将行业维度（如"采矿业-煤炭开采"）的数据归入对应企业

**关键代码逻辑：**
```
R426(reqKey=行业分类) → 产出 hash key
R427(reqKey=hash)     → 消费 hash key
两者通过 R11103V3.basicList.industryPhyName 关联到具体企业
```

### Step 3：从 Excel 确定 moduleCode 设计

**输入：** Excel 映射文档

**处理：**
1. 读 E 列（数据内容），每一项 = 一个 moduleCode
2. D 列（模块）= 分组维度，但不是 moduleCode 粒度
3. 每个 E 列项列出所需的接口编号（I 列），排除删除线行
4. 分类：
   - **单接口来源**（如"基本信息"只来自 R11103V3）→ 1 个 moduleCode
   - **多接口聚合**（如"股权与实际控制人"来自 R11103V3+R11Z03+R11C72）→ 1 个 moduleCode，聚合多个接口

**命名规范：** `Service-{BusinessConcept}-Data`
- Customer 前缀 = 客户画像类
- Financial 前缀 = 财务分析类
- Comprehensive 前缀 = 综合分析类

### Step 4：从 PDF 文件名提取接口英文名

**输入：** 接口文档 PDF 目录

**处理：**
1. 文件名格式：`{中文名}（{transcode}）.pdf`
2. 提取中文名，翻译为简短英文 key（如 企业工商查询服务 → EnterpriseInfo）
3. 建立 transcode → EnglishName 映射表

### Step 5：聚合生成离线数据文件

**输入：** Step 1-4 的结果

**处理：**
1. 删除工程 `offline-data/` 下旧的企业目录
2. 对每个企业 × 每个 moduleCode：
   - 收集 moduleCode 所需的所有 transcode 的数据
   - key 用英文名替换原始 transcode
   - 拼成 `{"key": md5, "prompt": "", "inputIndexContent": {EnglishName: [...]}}` 格式
3. 写入 `offline-data/{企业名}/{moduleCode}/RM1201.json`

**输出格式示例：**
```json
{
  "key": "1c2e750a15dcbd27011461dcc93fd441",
  "prompt": "",
  "inputIndexContent": {
    "EnterpriseInfo": [...],
    "ActualController": [...],
    "UltimateBeneficiary": [...]
  }
}
```

---

## 常用 transcode ↔ 英文名速查表

| 原始 | 英文 | 中文 |
|------|------|------|
| R11103V3 | EnterpriseInfo | 企业工商查询服务 |
| R11101 | PatentInfo | 专利基本信息查询 |
| R11110 | BiddingInfo | 企业招投标信息 |
| R11133 | RelatedPartyQuery | 关联关系查询配置化 |
| R115 | ConstructionProject | 在建工程实时接口 |
| R1167 | CapitalChainGraph | 企业资金链图谱挖掘 |
| R1184 | SoftwareCopyright | 软件著作权实时接口 |
| R1199 | TrademarkInfo | 商标基本信息查询 |
| R11C72 | UltimateBeneficiary | 企业最终受益人挖掘 |
| R11Z02 | ExecutiveInvestPosition | 高管投资任职信息 |
| R11Z03 | ActualController | 企业实际控制人信息 |
| R1201V3 | RiskBlacklist | 企业风险失信名单 |
| R1212V2 | AdminPenalty | 企业行政处罚信息 |
| R1617 | QualificationInfo | 资质数据实时查询 |
| R1618 | CustomsRecord | 海关备案数据实时接口 |
| R217V2 | DishonestyExecutee | 失信被执行人 |
| R227V2 | JudicialNotice | 企业司法公告信息 |
| R241V2 | ExecuteeInfo | 被执行人信息 |
| R243V2 | ConsumptionLimit | 限制高消费信息 |
| R255 | JudicialSeizure | 企业司法查封信息 |
| R301V2 | RiskPublicOpinion | 企业风险舆情信息 |
| R354V2 | NewsFlash | 企业舆情快讯 |
| R4G04V2 | ListedCompanyInfo | 上市公司基础信息 |
| R4G05 | FinancialReportList | 公众企业财务报表列表 |
| R4G06V2 | FinancialReportDetail | 公众企业财务报表详情 |
| R426 | PolicyList | 政策列表信息 |
| R427 | PolicyDetail | 政策详情信息 |
| R705V5 | TaxCreditRank | 企业税务纳税信用排名 |
| RA1216 | MarketingEnterprise | 营销名录企业查询 |
| RA403 | ExecutiveResume | 高管履历信息 |
