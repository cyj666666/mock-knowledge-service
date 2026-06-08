# 知识服务挡板系统 — 工程介绍文档

> 版本 1.0.0 | 2026-06-08

---

## 目录

1. [项目概述](#1-项目概述)
2. [业务背景：RM1201 / RM1202 是什么](#2-业务背景rm1201--rm1202-是什么)
3. [系统架构](#3-系统架构)
4. [项目结构](#4-项目结构)
5. [接口规范](#5-接口规范)
6. [离线数据包](#6-离线数据包)
7. [构建与部署](#7-构建与部署)
8. [配置说明](#8-配置说明)
9. [运维指南](#9-运维指南)

---

## 1. 项目概述

**知识服务挡板系统**（mock-knowledge-service）是一个 Spring Boot 程序，用于在客户现场模拟知识服务平台的 RM1201 / RM1202 接口。它不调用真实的知识服务后端，而是从预制的「离线数据包」中读取数据直接返回。

### 为什么需要挡板？

| 场景 | 说明 |
|------|------|
| 客户未开通知识服务 API | 提供挡板数据让客户先对接，验证调用链路 |
| 网络隔离环境 | 客户现场无法访问外网，数据需离线提供 |
| 数据脱敏 | 真实数据不便直接给客户，挡板数据可控 |
| 接口联调 | 客户开发阶段无需等待真实数据，降低联调阻塞 |

### 技术栈

| 组件 | 版本 |
|------|------|
| Java | 1.8 |
| Spring Boot | 2.7.18 |
| Jackson | 2.13.x（Spring Boot 内置） |
| Maven | 3.6+ |

---

## 2. 业务背景：RM1201 / RM1202 是什么

知识服务平台定义了统一的调用框架，所有服务通过两个 transcode 完成：

```
RM1201（发起）──→ 提交查询条件，获取结果或返回 key
RM1202（获取）──→ 凭 key 获取异步生成的内容（如大模型分析文本）
```

### 两类接口

知识服务接口分为两种类型，**由 moduleCode 决定**：

| | 指标类（Indicator） | 内容分析类（Content Analysis） |
|---|---|---|
| **识别标志** | `reqParams.indexFlag = true` | 无 indexFlag |
| **RM1201 返回** | 完整结构化数据（如工商信息） | 只有 `key` + `prompt` |
| **RM1202 是否需要** | **不需要** | **必须**，用 key 轮询获取内容 |
| **典型 moduleCode** | `Service-BusinessBasicList-*` | `Industry-Overview-*` |
| **数据在哪个文件** | `RM1201.json` | `RM1201.json` + `RM1202.json` |

### 调用流程

```
客户 ──RM1201(entName + moduleCode)──→ 挡板
                                        │
                    ┌───────────────────┤
                    ↓                   ↓
               指标类                 内容分析类
           inputIndexContent        inputIndexContent = {}
           有完整数据 ←─ 直接返回    只有 key + prompt
               结束                    │
                                      ↓
                          客户 ──RM1202(key)──→ 挡板
                                                  │
                                          读取 RM1202.json
                                          返回 content
                                                  │
                                              结束
```

---

## 3. 系统架构

### 运行时架构

```
┌──────────────┐      POST /api/knowledge      ┌─────────────────────────┐
│              │ ─────────────────────────────→ │  mock-knowledge-service │
│  客户系统     │                                │                         │
│              │ ←───────────────────────────── │  ├─ KnowledgeController  │
└──────────────┘    {code, msg, data}           │  ├─ RoutingService      │
                                                │  ├─ KeymapLoader        │
                                                │  └─ offline-data/*.json  │
                                                └─────────────────────────┘
```

### 请求处理流程

```
KnowledgeController.handle(request)
    │
    ├─ transcode = "RM1201"
    │     │
    │     └─ RoutingService.handleRM1201()
    │           │
    │           ├─ 提取 entName（安全化 → 目录名）
    │           ├─ 提取 moduleCode
    │           ├─ 路径: {dataPath}/{entName}/{moduleCode}/RM1201.json
    │           ├─ 文件存在 → 读取，套入 {code:"0000", msg:"...", data:[...]}
    │           └─ 文件不存在 → {code:"9999", msg:"无匹配数据", data:[]}
    │
    └─ transcode = "RM1202"
          │
          └─ RoutingService.handleRM1202()
                │
                ├─ 提取 key
                ├─ KeymapLoader.getPath(key) → "{entName}/{moduleCode}"
                ├─ 路径: {dataPath}/{entName}/{moduleCode}/RM1202.json
                ├─ 文件存在 → 读取，套入统一返参
                └─ key 未注册或文件不存在 → {code:"9999", ...}
```

---

## 4. 项目结构

```
mock-knowledge-service/
│
├── pom.xml                                  # Maven 配置（Spring Boot 2.7.18）
│
├── src/main/java/com/mock/knowledge/
│   ├── MockApplication.java                 # 启动类（含启动日志）
│   ├── controller/
│   │   └── KnowledgeController.java         # POST /api/knowledge 统一入口
│   ├── service/
│   │   ├── RoutingService.java              # 核心路由 + 文件读取 + 返回封装
│   │   └── KeymapLoader.java                # keymap.json 加载与查询
│   ├── model/
│   │   ├── KnowledgeRequest.java            # 请求体（含嵌套 Params）
│   │   └── KnowledgeResponse.java           # 返回体（code + msg + data）
│   └── config/
│       └── AppConfig.java                   # 可配置项（dataPath / 鉴权等）
│
├── src/main/resources/
│   └── application.yml                      # 配置（端口、数据路径等）
│
├── offline-data/                            # 离线数据包（示例）
│   ├── keymap.json                          # key → 路径映射（只注册内容分析类）
│   ├── gen_keymap.py                        # keymap 自动生成脚本
│   └── 北大方正集团有限公司/
│       ├── Service-BusinessBasicList-Data-Customer-POC/
│       │   └── RM1201.json                  # 指标类：完整工商数据
│       └── Industry-Overview-Data-Customer-POC/
│           ├── RM1201.json                  # 内容分析类：只有 key + prompt
│           └── RM1202.json                  # 内容分析类：实际分析文本
│
├── test_rm1201_indicator.json               # 测试用例：指标类
├── test_rm1201_content.json                 # 测试用例：内容分析类发起
├── test_rm1201_notfound.json                # 测试用例：企业不存在
├── test_rm1202.json                         # 测试用例：内容获取
├── test_rm1202_invalid.json                 # 测试用例：无效 key
│
├── TEST_REPORT.md                           # 测试报告
└── PROJECT_GUIDE.md                         # 本文档
```

---

## 5. 接口规范

### 统一入口

```
POST /api/knowledge
Content-Type: application/json; charset=UTF-8
```

### 统一请求体

```json
{
    "account": "string",           // 鉴权账号（挡板默认不校验）
    "secretKey": "string",         // 鉴权密钥（挡板默认不校验）
    "transcode": "RM1201",         // 接口号：RM1201 或 RM1202
    "userid": "string",            // 用户标识
    "orgid": "string",             // 机构标识
    "source": "string",            // 来源标识
    "params": {
        // ... 以下字段因 transcode 和 moduleCode 而异
    }
}
```

### 统一返回体

```json
{
    "code": "0000",                // 响应码
    "msg": "数据响应正确!",          // 响应描述
    "data": [
        { /* 业务数据 */ }
    ]
}
```

### 响应码

| code | 含义 | 说明 |
|------|------|------|
| 0000 | 正常响应 | 数据匹配成功 |
| 1000 | 请求参数不合法 | entName / moduleCode / key 为空 |
| 9999 | 无匹配数据 | 文件不存在或 key 未注册 |
| 9999 | 内部错误 | 数据文件解析失败 |

### RM1201 — 发起接口

**入参关键字段：**

| 字段 | 必填 | 说明 |
|------|------|------|
| `transcode` | 是 | 固定 `"RM1201"` |
| `params.entName` | 是 | 企业名称全称 |
| `params.moduleCode` | 是 | 知识服务 CODE，决定数据来源 |
| `params.stream` | 是 | 是否流式 (`"Y"` / `"N"`) |
| `params.reqParams` | 是 | 业务参数，字段由 moduleCode 决定 |

**返回 data[0] 结构：**

```json
{
    "key": "string",
    "prompt": "string",
    "inputIndexContent": { /* 由 moduleCode 决定结构 */ }
}
```

### RM1202 — 内容获取接口

**入参关键字段：**

| 字段 | 必填 | 说明 |
|------|------|------|
| `transcode` | 是 | 固定 `"RM1202"` |
| `params.key` | 是 | RM1201 返回的 key |

**返回 data[0] 结构：**

```json
{
    "isFinish": "Y",
    "content": "大模型分析文本..."
}
```

---

## 6. 离线数据包

离线数据包是整个系统的数据来源，采用「**路径即索引**」的设计。

### 6.1 目录结构

```
offline-data/                        # 数据包根目录（与 jar 同级）
├── keymap.json                      # key → 路径映射
└── {企业名称}/                       # 企业名 = 目录名
    └── {moduleCode}/                # 知识服务 CODE = 子目录名
        ├── RM1201.json              # RM1201 返回的 data[0] 内容
        └── RM1202.json              # RM1202 返回的 data[0] 内容（如需）
```

### 6.2 文件内容规范

**RM1201.json** — 即 RM1201 接口返回中 `data[0]` 的内容：

```json
{
    "key": "unique-key-string",
    "prompt": "简短提示或空字符串",
    "inputIndexContent": {
        // 指标类：此处包含完整业务数据
        // 内容分析类：此处为 {}（空对象）
    }
}
```

**RM1202.json** — 即 RM1202 接口返回中 `data[0]` 的内容：

```json
{
    "isFinish": "Y",
    "content": "大模型生成的分析文本内容"
}
```

### 6.3 keymap.json

用于 RM1202 的 key → 路径查找。由 `gen_keymap.py` 自动生成，也可手动维护。

```json
{
    "unique-key-string-1": "企业名A/moduleCode-A",
    "unique-key-string-2": "企业名B/moduleCode-B"
}
```

> **规则：** 只注册内容分析类的 key（inputIndexContent 为空对象）。指标类的 key 不注册，因为客户不会调用 RM1202。

### 6.4 造数据指南

#### 步骤 1：确定 moduleCode 和接口类型

与产品确认要提供哪些 moduleCode 的数据，判断是指标类还是内容分析类。

#### 步骤 2：创建目录

```bash
# 假设企业名 "XX科技有限公司", moduleCode "Your-Service-Code"
mkdir -p offline-data/XX科技有限公司/Your-Service-Code/
```

#### 步骤 3：编写 RM1201.json

从真实知识服务接口的返回中，提取 `data[0]` 的内容，写入 RM1201.json。

- 指标类：`inputIndexContent` 包含完整数据，保留真实 `key`
- 内容分析类：`inputIndexContent` 设为 `{}`，`key` 必须与 RM1202 文件名对应

#### 步骤 4：编写 RM1202.json（内容分析类）

从真实接口的 RM1202 返回中，提取 `data[0]` 的内容写入。

#### 步骤 5：生成 keymap.json

```bash
python gen_keymap.py
# 或指定路径
python gen_keymap.py /path/to/offline-data
```

> **脚本自动识别**：inputIndexContent 为空的 → 注册到 keymap；不为空的（指标类）→ 跳过。

#### 步骤 6：验证

启动程序后，用 curl 测试 RM1201 + RM1202 是否能正确返回。

### 6.5 重要注意事项

| 注意点 | 说明 |
|--------|------|
| **企业名作目录名** | 企业名含 `/` `\` `:` `*` `?` `"` `<` `>` `|` 时会自动替换为全角字符 |
| **不可手写 JSON** | 必须用 `json.dump` 生成文件，否则可能产生非法 JSON |
| **特殊字符** | `content` 中如有中文引号 `""`，序列化时需用 `json.dump` 确保正确转义 |
| **key 唯一性** | 所有 RM1201.json 中的 key 必须全局唯一 |
| **只有内容分析类需要 RM1202.json** | 指标类只需 RM1201.json |

---

## 7. 构建与部署

### 7.1 构建

```bash
cd mock-knowledge-service
mvn clean package -DskipTests
```

产物：`target/mock-knowledge-service.jar`

### 7.2 部署目录结构

```
/opt/app/
├── mock-knowledge-service.jar      # 程序 jar
└── offline-data/                    # 离线数据包（与 jar 同级）
    ├── keymap.json
    ├── 企业A/
    │   └── moduleCode-A/
    │       ├── RM1201.json
    │       └── RM1202.json
    └── 企业B/
        └── ...
```

### 7.3 启动

```bash
# 默认配置（读取 ./offline-data/）
java -jar mock-knowledge-service.jar

# 指定数据路径
java -jar mock-knowledge-service.jar --mock.data-path=/data/custom-offline

# 指定端口
java -jar mock-knowledge-service.jar --server.port=18080
```

### 7.4 启动验证

启动成功日志：

```
========================================
  知识服务已启动
  API: POST http://10.3.10.200:18080/api/knowledge
  RM1201 → entName + moduleCode
  RM1202 → key
========================================
```

健康检查：

```bash
curl -X POST http://localhost:18080/api/knowledge \
  -H "Content-Type: application/json" \
  -d '{"transcode":"RM1201","params":{"entName":"test","moduleCode":"test"}}'

# 期望返回: {"code":"9999","msg":"无匹配数据","data":[]}
```

---

## 8. 配置说明

`application.yml`：

```yaml
server:
  port: 18080                          # 服务端口

mock:
  data-path: ./offline-data            # 离线数据包根目录
  success-code: "0000"                 # 成功时的 code
  success-msg: "数据响应正确!"          # 成功时的 msg
  auth-check: false                    # 鉴权开关（默认关闭）
  # valid-account: xxx                 # auth-check=true 时生效
  # valid-secret-key: xxx              # auth-check=true 时生效
```

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `mock.data-path` | `./offline-data` | 数据包路径，支持相对/绝对路径 |
| `mock.success-code` | `0000` | 成功响应码 |
| `mock.success-msg` | `数据响应正确!` | 成功响应消息 |
| `mock.auth-check` | `false` | 是否校验 account/secretKey |
| `mock.valid-account` | 无 | 合法 account（auth-check=true 时生效） |
| `mock.valid-secret-key` | 无 | 合法 secretKey（auth-check=true 时生效） |

---

## 9. 运维指南

### 9.1 更新数据包（不停机）

数据包文件每次请求时实时读取，**无需重启**：

```bash
# 直接替换/新增 offline-data 目录下的文件即可
cp new-RM1201.json offline-data/企业名/moduleCode/RM1201.json
```

如需更新 keymap：

```bash
python gen_keymap.py
# keymap.json 也是每次请求时读取，无需重启
```

### 9.2 日志

日志输出到控制台（stdout）。关键日志：

```
INFO  - 收到请求: transcode=RM1201, entName=..., moduleCode=...
INFO  - RM1201 查询: entName=..., moduleCode=..., 路径=...
WARN  - 文件不存在: ...
WARN  - RM1202 key 未注册: key=...
ERROR - 文件读取失败: ...
```

### 9.3 常见问题

| 问题 | 原因 | 解决 |
|------|------|------|
| 返回 `9999 无匹配数据` | 数据文件不存在 | 检查 `{entName}/{moduleCode}/RM1201.json` 路径是否正确 |
| RM1202 返回 `9999 无匹配数据` | key 未注册到 keymap | 运行 `python gen_keymap.py` 重新生成 |
| 启动时提示 `keymap.json 不存在` | 首次部署 | 运行 `python gen_keymap.py` 生成，或正常（无内容分析类数据时） |
| 返回 `9999 内部错误` | JSON 文件格式非法 | 用 Python `json.load()` 验证文件合法性 |
| 中文乱码 | 文件编码不是 UTF-8 | 确保所有 JSON 文件为 UTF-8 编码 |
