# 知识服务挡板 — 数据库方案设计

> 版本 1.0.0 | 2026-06-08

---

## 目录

1. [方案概述](#1-方案概述)
2. [核心设计：TransKeyWord 两种形态](#2-核心设计transkeyword-两种形态)
3. [表结构设计](#3-表结构设计)
4. [造数据指南](#4-造数据指南)
5. [接口实现](#5-接口实现)
6. [关键决策与注意事项](#6-关键决策与注意事项)

---

## 1. 方案概述

挡板数据存储在 MySQL 表 `app_api_transcode_and_data` 中，程序通过 SQL 查询直接返回预制的 JSON 响应。

核心理念：**RM1201 发起查询，RM1202 获取内容**。TransKeyWord 字段承担双重角色，天然解决 key→数据的关联问题，**不需要额外的映射表**。

### 一张表，两条 SQL

```
RM1201 → SELECT data WHERE TransKeyWord = 'entName@moduleCode' AND transcode = 'RM1201'
RM1202 → SELECT data WHERE TransKeyWord = '{key}'              AND transcode = 'RM1202'
```

### 适用场景

- 公司内部测试环境，挡板数据集中管理
- 多个测试实例需要共享同一份挡板数据
- 需要批量造数、快速上下线的场景

---

## 2. 核心设计：TransKeyWord 两种形态

这是整个方案的精髓。TransKeyWord 字段承担两种角色：

```
             ┌─────────────────────────────────┐
             │     app_api_transcode_and_data   │
             │                                 │
RM1201 ───→ │ TransKeyWord = "entName@module"  │
             │ transcode    = "RM1201"         │
             │ data         = 完整响应JSON      │
             │                                 │
             │    （data 内部包含 key 字段）      │
             │                                 │
RM1202 ───→ │ TransKeyWord = "{key}"           │
             │ transcode    = "RM1202"         │
             │ data         = 完整响应JSON      │
             └─────────────────────────────────┘
```

### 形态一：`entName@moduleCode`

- 用于 **RM1201** 查询
- 分隔符 `@` 连接企业名和 moduleCode
- 一对一命中：一个企业的一个 service 只有一条记录
- 程序运行时拼接：`entName + "@" + moduleCode`

### 形态二：`{key}`

- 用于 **RM1202** 查询
- key 是 RM1201 返回 data 中的 `key` 字段值
- RM1201 和 RM1202 通过这个 key 值天然关联
- 造数据时：RM1202 的 TransKeyWord = RM1201 data 中的 key 值
- **不需要额外的映射表**

### 两种类型接口的 TransKeyWord 全景

| 接口类型 | RM1201 TransKeyWord | RM1202 TransKeyWord | 是否造 RM1202 |
|----------|---------------------|---------------------|---------------|
| 指标类 | `entName@moduleCode` | — | 不需要 |
| 内容分析类 | `entName@moduleCode` | `{key}`（= RM1201 返回的 key） | 必须 |

---

## 3. 表结构设计

### 3.1 建表语句

```sql
CREATE TABLE app_api_transcode_and_data (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    TransKeyWord VARCHAR(500)  NOT NULL COMMENT '查询关键词：
                                                   RM1201 → entName@moduleCode
                                                   RM1202 → key',
    transcode   VARCHAR(20)   NOT NULL COMMENT '接口号：RM1201 或 RM1202',
    IsValid     CHAR(1)       NOT NULL DEFAULT 'Y' COMMENT '是否有效：Y=有效 N=无效',
    data        LONGTEXT      NOT NULL COMMENT '完整响应JSON（含code/msg/data外层）',
    InputTime   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '录入时间',
    UpdateTime  DATETIME      ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

    UNIQUE KEY uk_trans (TransKeyWord, transcode),
    KEY idx_transcode (transcode),
    KEY idx_isvalid (IsValid)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识服务挡板数据表';
```

### 3.2 字段说明

| 字段 | 类型 | 说明 |
|------|------|------|
| `TransKeyWord` | VARCHAR(500) | 联合唯一键。两种值：`entName@moduleCode` 或 `{key}` |
| `transcode` | VARCHAR(20) | `RM1201` 或 `RM1202` |
| `IsValid` | CHAR(1) | `Y` = 在线，`N` = 下线（软删除） |
| `data` | LONGTEXT | 完整 JSON 响应字符串，包含 `code`/`msg`/`data` 三层，程序取出直接返回 |
| `InputTime` | DATETIME | 录入时间 |

### 3.3 唯一约束

`UNIQUE(TransKeyWord, transcode)` 保证：

- 同一 entName+moduleCode 只能有一条 RM1201 记录 → 防止重复造数
- 同一 key 只能有一条 RM1202 记录 → 防止 key 冲突

可利用 `INSERT ... ON DUPLICATE KEY UPDATE` 实现「有则更新，无则插入」的幂等造数。

### 3.4 data 字段内容

与接口真实返回完全一致，**不做裁剪**：

```json
{
    "code": "0000",
    "msg": "数据响应正确!",
    "data": [
        {
            "key": "eb8f7479bd2f...",
            "prompt": "",
            "inputIndexContent": { "basicList": [...] }
        }
    ]
}
```

> 程序从数据库取出 `data` 后直接返回，不做二次加工。

---

## 4. 造数据指南

### 4.1 指标类（以企业工商信息为例）

只需一条 INSERT，**不需要 RM1202 记录**。客户调 RM1201 一步拿全数据。

```sql
INSERT INTO app_api_transcode_and_data
(TransKeyWord, transcode, IsValid, data, InputTime)
VALUES(
    '北大方正集团有限公司@Service-BusinessBasicList-Data-Customer-POC',
    'RM1201',
    'Y',
    '{
        "code": "0000",
        "msg": "数据响应正确!",
        "data": [{
            "key": "eb8f7479bd2ff109fbbdeb100bf3d83d",
            "prompt": "",
            "inputIndexContent": {
                "basicList": [{
                    "enterpriseName": "北大方正集团有限公司",
                    "creditCode": "911100001019838370",
                    "frName": "刘建",
                    "enterpriseStatus": "存续",
                    "regCap": "1000000"
                }]
            }
        }]
    }',
    '2026-06-08'
);
```

### 4.2 内容分析类（以行业概况为例）

需要**两条** INSERT。**关键：RM1202 的 TransKeyWord 必须等于 RM1201 data 中的 key 值**。

**Step 1：造 RM1201 记录**

```sql
INSERT INTO app_api_transcode_and_data
(TransKeyWord, transcode, IsValid, data, InputTime)
VALUES(
    '北大方正集团有限公司@Industry-Overview-Data-Customer-POC',
    'RM1201',
    'Y',
    '{
        "code": "0000",
        "msg": "数据响应正确!",
        "data": [{
            "key": "a1b2c3d4e5f6789012345678abcdef01",
            "prompt": "根据建筑行业在江苏省的发展情况分析，得出以下行业概况",
            "inputIndexContent": {}
        }]
    }',
    '2026-06-08'
);
```

**Step 2：造 RM1202 记录**

```sql
-- TransKeyWord 必须等于上一步 data 中的 key 值！
INSERT INTO app_api_transcode_and_data
(TransKeyWord, transcode, IsValid, data, InputTime)
VALUES(
    'a1b2c3d4e5f6789012345678abcdef01',
    'RM1202',
    'Y',
    '{
        "code": "0000",
        "msg": "数据响应正确!",
        "data": [{
            "isFinish": "Y",
            "content": "江苏省建筑业行业概况分析：\n\n一、行业基本情况\n江苏省作为全国建筑业大省..."
        }]
    }',
    '2026-06-08'
);
```

### 4.3 造数据关键规则

| # | 规则 | 说明 |
|---|------|------|
| 1 | TransKeyWord 用 `@` 分隔 | 左边 entName，右边 moduleCode。如企业名不含 `@` 则绝对安全 |
| 2 | key 全局唯一 | 所有 RM1201 的 data 中 key 值必须互不相同 |
| 3 | RM1202 的 TransKeyWord = key | 内容分析类造完 RM1201 后，取其 data 中的 key 值，作为 RM1202 的 TransKeyWord |
| 4 | 指标类只造 RM1201 | 不需要 RM1202 记录 |
| 5 | data 存完整 JSON | 程序取出直接返回，不拼装 |

### 4.4 数据下线 / 上线

不删记录，通过 `IsValid` 软开关控制：

```sql
-- 下线
UPDATE app_api_transcode_and_data SET IsValid = 'N'
WHERE TransKeyWord = '北大方正集团有限公司@Service-BusinessBasicList-Data-Customer-POC'
  AND transcode = 'RM1201';

-- 重新上线
UPDATE app_api_transcode_and_data SET IsValid = 'Y'
WHERE TransKeyWord = '北大方正集团有限公司@Service-BusinessBasicList-Data-Customer-POC'
  AND transcode = 'RM1201';
```

---

## 5. 接口实现

### 5.1 RM1201

```
handleRM1201(request):
    entName    = request.params.entName
    moduleCode = request.params.moduleCode

    keyword = entName + "@" + moduleCode

    sql = "SELECT data FROM app_api_transcode_and_data
           WHERE TransKeyWord = ? AND transcode = 'RM1201' AND IsValid = 'Y'"

    result = db.query(sql, keyword)

    if result is None:
        return {"code": "9999", "msg": "无匹配数据", "data": []}

    return result.data   // 直接返回，零加工
```

### 5.2 RM1202

```
handleRM1202(request):
    key = request.params.key

    sql = "SELECT data FROM app_api_transcode_and_data
           WHERE TransKeyWord = ? AND transcode = 'RM1202' AND IsValid = 'Y'"

    result = db.query(sql, key)

    if result is None:
        return {"code": "9999", "msg": "无效key", "data": []}

    return result.data   // 直接返回，零加工
```

### 5.3 汇总

| | RM1201 | RM1202 |
|---|---|---|
| 请求入参 | `entName` + `moduleCode` | `key` |
| TransKeyWord 拼接 | `entName@moduleCode` | `key`（原值） |
| SQL | 一条 SELECT | 一条 SELECT |
| 返回 | 直接返回 data 字段 | 直接返回 data 字段 |

---

## 6. 关键决策与注意事项

### 6.1 `@` 分隔符的安全性

企业名称几乎不可能包含 `@` 字符。如果极端情况下出现，可改用更生僻的分隔符如 `||` 或 `|#|`。

### 6.2 data 字段存完整 JSON 的理由

- **最简单**：程序取出直接返回，零加工
- **自描述**：每条记录自己包含 code/msg，不同记录可以有不同的 msg（如区分具体错误原因）
- **可扩展**：未来 data 结构变化，不需要改表结构

### 6.3 唯一索引与幂等造数

`UNIQUE(TransKeyWord, transcode)` 配合 `ON DUPLICATE KEY UPDATE` 可实现幂等：

```sql
INSERT INTO app_api_transcode_and_data
(TransKeyWord, transcode, IsValid, data, InputTime)
VALUES('北大方正@Service-...', 'RM1201', 'Y', '...', NOW())
ON DUPLICATE KEY UPDATE data = VALUES(data), IsValid = 'Y', UpdateTime = NOW();
```

### 6.4 IsValid 软删除的优势

- 不物理删除记录，保留造数历史，可追溯
- `IsValid='N'` 的查询自动跳过，无需额外逻辑
- 上线/下线无需重建索引，秒级生效
- 适合批量开关：`UPDATE ... SET IsValid='N' WHERE InputTime < '2026-01-01'`

### 6.5 性能预估

- 单表百万级记录，`(TransKeyWord, transcode)` 联合索引查询 < 5ms
- data 字段可能较大（分析文本可达数千字），LONGTEXT 最大 4GB，足够承载
- 无联表查询，无复杂 SQL，性能瓶颈在磁盘 IO 而非 CPU

### 6.6 与真实接口的透明切换

| | 真实知识服务 | 数据库挡板 |
|---|---|---|
| RM1201 | 调用后端服务，动态生成 key | 查表，返回预制的 key |
| RM1202 | 调用大模型，流式生成内容 | 查表，返回预制的 content |
| 数据来源 | 实时计算 | 预置 JSON |

调用方感知不到差异——URL 相同、请求格式相同、返回格式相同。切换只需改数据库连接配置。
