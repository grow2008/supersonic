# Headless BI 指标体系 JSON 规范（Supersonic）

## 1. 目标与范围

本规范用于将“指标体系设计”文档与 Supersonic 语义层实体之间进行 JSON 交换（导入/导出）。

- 仅包含**业务必要字段**（不包含 createdBy/updatedBy/createdAt/updatedAt 等 RecordInfo 字段）。
- 覆盖语义层实体：数据库（Database）、主题域（Domain）、模型（Model）、维度（Dimension）、指标（Metric）、数据集（DataSet）。
- 合并策略：按 `name` 匹配，导入覆盖（update 优先）。
- 异常处理：出现任何异常或错误，**收集并打印全部错误后直接退出**。

## 2. 顶层结构

```json
{
  "meta": {
    "version": "1.0",
    "exportedAt": "2026-02-10T01:31:00+08:00",
    "source": "supersonic",
    "mergeStrategy": {
      "matchKey": "name",
      "onConflict": "overwrite"
    },
    "onError": "abort",
    "notes": "仅业务必要字段"
  },
  "databases": [],
  "domains": [],
  "models": [],
  "dimensions": [],
  "metrics": [],
  "dataSets": []
}
```

说明：
- 顶层数组可为空，但建议显式保留字段。
- `meta` 必填，作为版本与导入策略声明。

## 3. 实体字段定义

### 3.1 Database

**对应 API 请求**：`DatabaseReq`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 数据库名称（合并匹配 key） |
| type | string | 是 | 引擎类型（如 postgresql/mysql/clickhouse） |
| databaseType | string | 否 | 当 type=OTHER 时使用实际引擎名 |
| url | string | 是 | JDBC URL（推荐作为主连接字段） |
| username | string | 是 | 用户名 |
| password | string | 是 | 密码（样例用占位符） |
| database | string | 否 | DB 名称（部分引擎需要） |
| host | string | 否 | 主机（可选） |
| port | string | 否 | 端口（可选） |
| version | string | 否 | 版本（可选） |
| schema | string | 否 | Schema（如 public） |
| description | string | 否 | 描述 |
| admins | string[] | 否 | 管理员用户列表 |
| viewers | string[] | 否 | 只读用户列表 |

### 3.2 Domain

**对应 API 请求**：`DomainReq` / `DomainUpdateReq`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 主题域名称（合并匹配 key） |
| bizName | string | 是 | 业务名（英文/标识） |
| description | string | 否 | 描述 |
| parentName | string | 否 | 父主题域 name（导入时解析为 parentId） |
| isOpen | number | 否 | 1=公开，0=非公开 |
| viewers | string[] | 否 | 可见用户 |
| viewOrgs | string[] | 否 | 可见组织 |
| admins | string[] | 否 | 管理员 |
| adminOrgs | string[] | 否 | 管理员组织 |
| sensitiveLevel | number | 否 | 敏感等级（SchemaItem） |

### 3.3 Model

**对应 API 请求**：`ModelReq`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 模型名称（合并匹配 key） |
| bizName | string | 是 | 业务名/标识 |
| description | string | 否 | 描述 |
| domainName | string | 是 | 主题域 name（导入时解析为 domainId） |
| databaseName | string | 是 | 数据库 name（导入时解析为 databaseId） |
| filterSql | string | 否 | 默认过滤 SQL |
| isOpen | number | 否 | 1=公开，0=非公开 |
| alias | string | 否 | 别名 |
| sourceType | string | 否 | 来源类型（透传） |
| viewers | string[] | 否 | 可见用户 |
| viewOrgs | string[] | 否 | 可见组织 |
| admins | string[] | 否 | 管理员 |
| adminOrgs | string[] | 否 | 管理员组织 |
| drillDownDimensions | object[] | 否 | 下钻维度（按维度 name 解析为 id） |
| ext | object | 否 | 扩展字段 |
| modelDetail | object | 是 | 结构定义（见下） |

**modelDetail**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| queryType | string | 是 | `sql_query` 或 `table_query`（ModelDefineType） |
| dbType | string | 否 | 数据库类型（可选） |
| sqlQuery | string | 否 | SQL 查询（sql_query 时使用） |
| tableQuery | string | 否 | 表名（table_query 时使用，如 `db.table`） |
| identifiers | Identify[] | 否 | 标识字段 |
| dimensions | Dimension[] | 否 | 维度字段 |
| measures | Measure[] | 否 | 度量字段 |
| fields | Field[] | 否 | 字段列表（可选） |
| sqlVariables | SqlVariable[] | 否 | SQL 变量（可选） |

**Identify**（模型内标识字段）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 显示名 |
| type | string | 是 | `primary` / `foreign` |
| bizName | string | 是 | 字段名 |
| isCreateDimension | number | 否 | 1=自动生成维度 |

**Dimension**（模型内维度字段）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 显示名 |
| bizName | string | 是 | 字段名 |
| type | string | 是 | `categorical`/`time`/`partition_time`/`primary_key`/`foreign_key` |
| expr | string | 否 | 表达式（为空时默认 bizName） |
| dateFormat | string | 否 | 时间格式（默认 yyyy-MM-dd） |
| typeParams | object | 否 | 时间维度参数（isPrimary/timeGranularity） |
| isCreateDimension | number | 否 | 1=自动生成维度 |
| description | string | 否 | 描述 |

**Measure**（模型内度量字段）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 显示名 |
| bizName | string | 是 | 字段名 |
| agg | string | 是 | 聚合函数（sum/avg/count 等） |
| expr | string | 否 | 表达式（为空时默认 bizName） |
| isCreateMetric | number | 否 | 1=自动生成原子指标 |
| constraint | string | 否 | 约束/过滤 |
| alias | string | 否 | 别名 |

### 3.4 Dimension

**对应 API 请求**：`DimensionReq`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 维度名称（合并匹配 key） |
| bizName | string | 是 | 字段名 |
| description | string | 否 | 描述 |
| modelName | string | 是 | 归属模型 name（导入时解析为 modelId） |
| type | string | 是 | 同 DimensionType 枚举 |
| expr | string | 是 | 表达式/字段名 |
| semanticType | string | 否 | `CATEGORY`/`DATE` |
| alias | string | 否 | 别名 |
| defaultValues | string[] | 否 | 默认值 |
| dimValueMaps | DimValueMap[] | 否 | 维度值映射 |
| dataType | string | 否 | DataTypeEnums（如 VARCHAR/DATE/BIGINT） |
| ext | object | 否 | 扩展字段（时间维度建议使用 `time_format`） |
| typeParams | object | 否 | DimensionTimeTypeParams |
| sensitiveLevel | number | 否 | 敏感等级 |

### 3.5 Metric

**对应 API 请求**：`MetricReq`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 指标名称（合并匹配 key） |
| bizName | string | 是 | 业务名 |
| description | string | 否 | 描述 |
| modelName | string | 是 | 归属模型 name（导入时解析为 modelId） |
| alias | string | 否 | 别名 |
| dataFormatType | string | 否 | `percent`/`decimal` |
| dataFormat | object | 否 | DataFormat（needMultiply100/decimalPlaces） |
| classifications | string[] | 否 | 分类标签 |
| relateDimension | object | 否 | 下钻维度配置 |
| isTag | number | 否 | 是否标签 |
| ext | object | 否 | 扩展字段 |
| metricDefineType | string | 是 | `FIELD`/`MEASURE`/`METRIC` |
| metricDefineByMeasureParams | object | 否 | MEASURE 定义参数 |
| metricDefineByFieldParams | object | 否 | FIELD 定义参数 |
| metricDefineByMetricParams | object | 否 | METRIC 定义参数 |

**MetricDefineByMeasureParams**
- expr: string
- filterSql: string
- measures: Measure[]（与 ModelDetail.measure 结构一致）

**MetricDefineByFieldParams**
- expr: string
- filterSql: string
- fields: FieldParam[]（字段名列表）

**MetricDefineByMetricParams**
- expr: string
- filterSql: string
- metrics: MetricParam[]（依赖指标，按 bizName 解析）

**relateDimension.drillDownDimensions**
- dimensionName: string（导入时按维度 name 解析为 dimensionId）
- necessary: boolean
- inheritedFromModel: boolean

### 3.6 DataSet

**对应 API 请求**：`DataSetReq`

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| name | string | 是 | 数据集名称（合并匹配 key） |
| bizName | string | 是 | 业务名 |
| description | string | 否 | 描述 |
| domainName | string | 是 | 主题域 name（导入时解析为 domainId） |
| alias | string | 否 | 别名 |
| admins | string[] | 否 | 管理员 |
| adminOrgs | string[] | 否 | 管理员组织 |
| dataSetDetail | object | 是 | DataSetDetail（见下） |
| queryConfig | object | 否 | QueryConfig（默认时间/limit 等） |

**DataSetDetail**
- dataSetModelConfigs: DataSetModelConfig[]

**DataSetModelConfig**
- modelName: string（导入时解析为 modelId）
- includesAll: boolean
- metrics: string[]（指标 name 列表，导入时解析为 id）
- dimensions: string[]（维度 name 列表，导入时解析为 id）

## 4. 字段 ↔ API 映射清单

> 以下为导入/导出时推荐使用的 API 列表（以当前代码定义为准）。

### 导入（Create/Update）

| 实体 | 创建 API | 更新 API | 关键请求对象 |
|---|---|---|---|
| Database | POST /api/semantic/database/createOrUpdateDatabase | 同左 | DatabaseReq |
| Domain | POST /api/semantic/domain/createDomain | POST /api/semantic/domain/updateDomain | DomainReq / DomainUpdateReq |
| Model | POST /api/semantic/model/createModel | POST /api/semantic/model/updateModel | ModelReq |
| Dimension | POST /api/semantic/dimension/createDimension | POST /api/semantic/dimension/updateDimension | DimensionReq |
| Metric | POST /api/semantic/metric/createMetric | POST /api/semantic/metric/updateMetric | MetricReq |
| DataSet | POST /api/semantic/dataSet | PUT /api/semantic/dataSet | DataSetReq |

### 导出（Get/List）

| 实体 | 获取 API | 说明 |
|---|---|---|
| Database | GET /api/semantic/database/getDatabaseList | 获取全部数据库 |
| Domain | GET /api/semantic/domain/getDomainList | 管理权限视角 |
| Model | GET /api/semantic/model/getModelList/{domainId} | 按域获取模型 |
| Dimension | GET /api/semantic/dimension/getDimensionList/{modelId} | 按模型获取维度 |
| Metric | GET /api/semantic/metric/getMetricList/{modelId} | 按模型获取指标 |
| DataSet | GET /api/semantic/dataSet/getDataSetList?domainId= | 按域获取数据集 |

## 5. 导入流程（建议顺序）

1. 读取 JSON 并校验 meta/version。
2. 读取现有实体列表（用于 name 匹配）。
3. 依次导入：Database → Domain → Model → Dimension/Metric → DataSet。
4. 对每个实体：
   - 若 name 已存在：取现有 id，调用更新 API。
   - 若 name 不存在：调用创建 API。
5. Model 导入注意事项：
   - 若 `modelDetail.*.isCreateDimension/isCreateMetric=1`，模型创建会自动生成维度/指标。
   - 若希望完全由 `dimensions/metrics` 数组控制，建议将上述标记置为 0，避免重复。

## 6. 导出流程（建议顺序）

1. 获取 Database/Domain 列表。
2. 按 Domain 获取 Model；按 Model 获取 Dimension 与 Metric。
3. 按 Domain 获取 DataSet，并补齐 DataSetModelConfig 的引用信息。
4. 输出 JSON，补齐 meta。

## 7. 合并策略（按 name 覆盖）

- **matchKey = name**：所有实体以 `name` 为唯一合并键。
- **overwrite**：导入字段覆盖系统已有定义。
- 关联字段在导入时解析为 id：
  - domainName → domainId
  - databaseName → databaseId
  - modelName → modelId
  - metrics/dimensions name → id

## 8. 错误处理

- 任意步骤失败：收集错误信息（实体类型/名称/异常堆栈），统一打印后退出。
- 建议返回非 0 退出码，避免部分写入造成不一致。

## 9. 版本与兼容

- `meta.version` 用于 JSON 规范版本控制。
- 如未来字段增加，保持向后兼容：新增字段默认可选。

## 10. 样例

- 参见根目录 `headless_bi_metrics_example.json`（字段全覆盖示例）。
