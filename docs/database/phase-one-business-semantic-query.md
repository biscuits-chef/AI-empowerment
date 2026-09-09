# 一期产品主题语义查询与 GoldenDB 表契约

## 1. 目标

本契约把产品自然语言问题转换为受控 Query Plan，再通过固定参数化 MyBatis 语句直接查询 GoldenDB 表 `dws_product_info_d`。它不是通用 Text-to-SQL，不允许模型提供可直接执行的表名、列名、Join 或 SQL。

## 2. 元数据职责

《语义层配置_产品主题数据集_v1.0》包含三类辅助模型理解的数据：

- 数据集注册：描述有哪些表、每张表的数据范围、粒度和更新方式。
- 字段注册：解释字段含义、类型、筛选/分组能力、同义词和别名。
- 枚举注册：描述枚举字段的合法值，帮助模型产生标准筛选条件。

模型只能依据已发布元数据输出标准意图、业务字段和结构化条件。系统随后验证数据集、字段能力、操作符、枚举、结果上限和快照策略，再由 SQL Compiler 选择固定 Statement。

## 3. 执行链路

```text
自然语言问题
  -> 意图识别、实体提取和上下文消解
  -> 产品代码/名称/简称/全称候选解析
  -> BusinessSemanticQuery
  -> PhaseOneBusinessSemanticCatalog
  -> BusinessQueryPlan
  -> CompiledBusinessQuery
  -> BusinessQueryMapper 固定参数化 SQL
  -> dws_product_info_d
  -> BusinessFact
```

一期产品计划如下：

| 逻辑计划 | 固定语句 | 用途 |
|---|---|---|
| `PRODUCT_LOOKUP` | `SELECT_PRODUCT_FACTS` | 查询指定产品的产品经理或两种投资经理口径 |
| `PRODUCT_REFERENCE_DATE_LIST` | `SELECT_REFERENCE_DATE_PRODUCTS` | 查询指定业务日期命中定开基准日或到期日的产品 |

既有 `TRADE_LOOKUP` 暂时保留兼容，但不属于本次产品数据表范围。

## 4. 快照表契约

`dws_product_info_d` 是产品主题每日全量快照表：

- 复合主键：`PRDC_CD + DT`。
- 产品解析字段：`PRDC_CD`、`PRDC_NM`、`PRDC_ABBR`、`PRDC_FLL_NM`。
- 产品经理：`PRDC_MNGR_NM`。
- 监管口径投资经理：`INVS_MNGR_NM`。
- 产品部口径投资经理：`PRDC_INVS_MNGR`。
- 产品到期日期：`PROD_MAT_DT`。
- 定开基准日期：`PERI_OPEN_BASE_DT`。
- 快照分区日期：`DT`。

`PROD_MAT_DT`、`PERI_OPEN_BASE_DT` 和 `DT` 均为 `VARCHAR(10)`，唯一允许格式为 `yyyy-MM-dd`。数据中台同步任务必须拒绝长度错误、格式错误和不存在的日期，并以完整快照为单位原子发布。

## 5. 查询规则

### 5.1 最新有效快照

所有产品查询先执行：

```sql
SELECT MAX(DT)
FROM dws_product_info_d
WHERE DT <= #{businessDate}
```

“今日”使用 Asia/Shanghai 业务时区。查询不得读取晚于业务日期的未来快照。

### 5.2 产品实体解析

在最新快照中按产品代码、名称、简称和全称匹配。精确匹配优先；包含匹配只用于形成候选。未找到时要求用户补充信息；多个候选时返回产品名称和代码让用户选择，不得进入知识库查询或模型生成。

### 5.3 投资经理

投资经理问题必须同时返回监管口径和产品部口径。即使两个值相同也分别展示；字段为空时展示“暂未维护”；两个口径不同属于业务口径差异，不直接标记为知识库与数据库冲突。

### 5.4 今日基准日或到期产品

固定条件为：

```text
PERI_OPEN_BASE_DT = businessDate OR PROD_MAT_DT = businessDate
```

查询分别返回 `periodicOpenBaseDateMatched` 和 `productMaturityDateMatched`。同一产品同时命中时，答案必须保留“定开基准日、产品到期日”两个原因。列表必须有稳定排序和结果数量上限。

## 6. 安全与权限

- Mapper XML 禁止 `${}`、动态表名、动态列名和 `SELECT *`。
- 所有用户值使用 `#{}` 参数绑定。
- 结果数量同时受问答上下文上限和 `app.business-query.maximum-rows` 限制。
- 生产使用独立只读账号，禁止 DDL 和 DML。
- 日志不记录完整问题、业务结果或 SQL 参数。
- 当前产品表没有行级用户权限字段。生产启用前必须确认产品数据是否对所有一期用户可见；如需行级权限，必须提供能进入 SQL 的服务端权限映射，不能查询全量后在 Java 中过滤。

## 7. 配置

```text
app.business-query.enabled=false
app.business-query.maximum-rows=100
app.business-query.semantic-model-version=phase1-v1
```

开发环境默认启用本地产品表查询，测试和生产仍由环境变量显式控制。生产开关必须在真实 GoldenDB 迁移、数据同步、权限和性能门禁完成后开启。

## 8. 生产启用门禁

- 数据负责人签署三类元数据、字段类型、日期质量规则和投资经理口径。
- 在真实 GoldenDB 验证迁移、字符集、复合主键、索引长度和 SQL 方言。
- 对最新快照、实体解析、经理查询和日期列表 SQL 保存 `EXPLAIN` 证据。
- 明确完整快照发布、重复批次、失败重试和数据回滚方式。
- 使用独立只读账号并验证产品数据权限。
- 验证语句超时、连接池预算、最大结果行数以及 10 QPS/50 并发目标。
