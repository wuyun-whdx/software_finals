# 地域定位 + AI 精准地点推荐功能 — 实现与变更说明

> 分支: `feature/ai-location-v3`  
> 基准: `main` (d447362)  
> 变更规模: 26 文件, +1642 行, -47 行  
> 日期: 2026-06-23

---

## 1. 功能概述

在现有推荐模块基础上新增两项能力：

- **省市区三级地址选择器**：用户可在推荐页选择省/市/区并保存，后续推荐携带地域信息
- **DeepSeek AI 精准地点推荐**：将「用户性格向量 + 地域 + 推荐场景」传给 DeepSeek，返回该城市下真实线下地点的名称、地址和适配理由，与原有通用推荐混合展示

无地域时自动退回原有通用推荐，不影响已有功能。

---

## 2. 技术架构

```
┌─────────────────────────────────────────────────────────┐
│  RecommendationsView.vue                                │
│    ├─ RegionSelector.vue  (省→市→区三级级联下拉)         │
│    │     └─ POST /api/user/region                       │
│    └─ 卡片列表                                          │
│         ├─ source="ai"     → 橙色左边框 + AI角标 + 地址  │
│         └─ source="general"→ 原有样式                    │
└──────────────────────┬──────────────────────────────────┘
                       │ GET /api/recommendations
                       │   ?scene= &province= &city= &district=
                       ▼
┌─────────────────────────────────────────────────────────┐
│  RecommendationController                               │
│    ├─ /api/regions/provinces|cities|districts           │
│    └─ /api/user/region  (GET|POST|/history)             │
└──────────────────────┬──────────────────────────────────┘
                       ▼
┌─────────────────────────────────────────────────────────┐
│  RecommendationService.recommendWithRegion()             │
│    ├─ 有地域 → AiRecommendationService                  │
│    │            ├─ AiClient  (DeepSeek Chat Completions) │
│    │            │    ├─ 3s连接超时 + 8s读取超时           │
│    │            │    ├─ 1次重试, 500ms间隔               │
│    │            │    └─ 无Key/超时 → 返回 null 降级      │
│    │            └─ Redis 缓存, TTL 24h                  │
│    │                 Key: ai:rec:{uid}:{scene}:{p}:{c}:{d}│
│    └─ 无地域 → 原有 RecommendationRanker 通用推荐        │
└─────────────────────────────────────────────────────────┘
```

---

## 3. 文件变更清单

### 3.1 新增后端 (12 文件)

| # | 文件路径 | 类型 | 说明 |
|---|----------|------|------|
| 1 | `backend/.../config/AiProperties.java` | Config | DeepSeek 配置类，`@ConfigurationProperties(prefix="app.ai.deepseek")`，字段: apiKey / baseUrl / model / timeoutSeconds / cacheTtlHours / hasKey() |
| 2 | `backend/.../domain/Province.java` | Entity | JPA 实体，`@Table(name="provinces")`，id + name(unique) |
| 3 | `backend/.../domain/City.java` | Entity | JPA 实体，`@Table(name="cities")`，id + name + province(ManyToOne) |
| 4 | `backend/.../domain/District.java` | Entity | JPA 实体，`@Table(name="districts")`，id + name + city(ManyToOne) |
| 5 | `backend/.../domain/UserRegion.java` | Entity | JPA 实体，`@Table(name="user_region")`，user(ManyToOne) + province/city/district + isCurrent + createdAt |
| 6 | `backend/.../repository/ProvinceRepository.java` | Repository | `JpaRepository<Province, Long>`，继承 `findAll()` |
| 7 | `backend/.../repository/CityRepository.java` | Repository | `JpaRepository<City, Long>` + `findByProvinceIdOrderByName(Long)` |
| 8 | `backend/.../repository/DistrictRepository.java` | Repository | `JpaRepository<District, Long>` + `findByCityIdOrderByName(Long)` |
| 9 | `backend/.../repository/UserRegionRepository.java` | Repository | `JpaRepository<UserRegion, Long>` + `findByUserAndIsCurrentTrue` + `findByUserOrderByCreatedAtDesc` |
| 10 | `backend/.../service/RegionService.java` | Service | 6 个方法: listProvinces / listCities / listDistricts / getCurrent / save / history。save() 自动将旧记录的 isCurrent 置 false |
| 11 | `backend/.../service/AiClient.java` | Component | DeepSeek Chat Completions 调用。`chat(systemPrompt, userMessage)` → String。构造: RestTemplateBuilder 设置连接 3s 读取 8s 超时。双重试(0/1)，间隔 500ms。无 Key 直接返回 null |
| 12 | `backend/.../service/AiRecommendationService.java` | Service | `recommend(user, scene, province, city, district)` → `List<LocationRecommendationResponse>`。流程: 查 Redis 缓存 → 调 getMergedScores → 拼 Prompt → AiClient.chat → 解析 JSON → 写 Redis。`@Lazy` 解决与 RecommendationService 的循环依赖 |

### 3.2 修改后端 (6 文件)

| # | 文件路径 | 改动说明 |
|---|----------|----------|
| 13 | `backend/.../dto/ApiDtos.java` | 新增 7 个 record: `SimpleRegion(Long id, String name)` / `RegionRequest` / `RegionResponse` / `RegionRecord` / `LocationRecommendationResponse`（扩展自 RecommendationResponse，加 address/aiReason/source） |
| 14 | `backend/.../controller/RecommendationController.java` | `@RequestMapping` 改为 `/api`。新增 6 个地域接口（见 4 节）。`GET /api/recommendations` 新增 `province/city/district` 可选参数，调用 `recommendWithRegion()` |
| 15 | `backend/.../service/RecommendationService.java` | 构造函数新增 `AiRecommendationService` 参数。新增 `recommendWithRegion(user, scene, province, city, district)` — 有地域时先调 AI 再将结果合并（AI 前置 + 通用，limit 15），无地域时走原逻辑。新增 `getMergedScores(user)` 公开方法。原有 `recommend()` 保留，内部转发到 `recommendWithRegion()` |
| 16 | `backend/.../service/DtoMapper.java` | 新增 `locationRecommendation(item, score, address, aiReason, source)` 静态方法 |
| 17 | `backend/.../resources/application.yml` | 新增 `app.ai.deepseek` 配置段，5 个属性均支持环境变量覆盖: `AI_API_KEY` / `AI_BASE_URL` / `AI_MODEL` / `AI_TIMEOUT_SECONDS` / `AI_CACHE_TTL_HOURS` |
| 18 | `backend/.../RadarApplication.java` | 新增 `import com.personality.radar.config.AiProperties` 和 `@EnableConfigurationProperties(AiProperties.class)` |

### 3.3 新增前端 (2 文件)

| # | 文件路径 | 说明 |
|---|----------|------|
| 19 | `frontend/src/components/RegionSelector.vue` | 三级级联下拉组件。Props: 无（通过 emit 通信）。Emits: `update:hasRegion(boolean)` / `regionChanged`。功能: onMounted 加载省份 + 查询已保存地域 → watch province 异步加载城市 → watch city 异步加载区县 → 点击"确认" save → 显示当前地域 / 引导提示 / 错误信息 |
| 20 | `frontend/src/services/regionService.ts` | 封装 6 个函数: `fetchProvinces` / `fetchCities` / `fetchDistricts` / `getMyRegion` / `saveMyRegion` / `regionHistory`，均调用 `api.ts` 中的 `regionApi` |

### 3.4 修改前端 (4 文件)

| # | 文件路径 | 改动说明 |
|---|----------|----------|
| 21 | `frontend/src/types.ts` | 新增 4 个 interface: `LocationRecommendation`（extends Recommendation，+address/aiReason/source）、`SimpleRegion`、`RegionInfo`、`RegionRecord` |
| 22 | `frontend/src/api.ts` | 导入新增类型。新增 `regionApi` 对象（6 个方法）。`recommendationApi.list()` 支持传入 `region?: RegionInfo`，展开到 query params |
| 23 | `frontend/src/services/recommendationService.ts` | `listRecommendations()` 新增可选参数 `region?: RegionInfo`，透传到 `recommendationApi.list()` |
| 24 | `frontend/src/views/RecommendationsView.vue` | 导入 `RegionSelector` / `LocationRecommendation` / `getMyRegion`。新增 `hasRegion` / `region` ref，`loadRegion()` / `onRegionChanged()` 方法。模板新增 `<RegionSelector>` 组件。卡片根据 `source==='ai'` 添加 `.ai-card` 样式类。新增 scoped style: `.ai-card`（橙色左边框）、`.ai-card::before`（"AI 精准推荐"角标）、`.address-line`、`.ai-reason` |

### 3.5 基础设施 (2 文件)

| # | 文件路径 | 说明 |
|---|----------|------|
| 25 | `infra/init-region-data.sql` | 建表 DDL（provinces/cities/districts/user_region）+ 31 省 340 市 + 省会及主要地级市区县 INSERT 数据。使用子查询 `SELECT id FROM cities WHERE name=...` 避免硬编码 city_id |
| 26 | `infra/docker-compose.yml` | backend 服务 environment 新增 `AI_API_KEY` / `AI_BASE_URL` / `AI_MODEL` 三个变量 |

---

## 4. API 接口详情

### 4.1 新增接口 (6 个)

| 方法 | 路径 | 请求/响应 | 说明 |
|------|------|-----------|------|
| GET | `/api/regions/provinces` | Response: `ApiResponse<List<SimpleRegion>>` | 返回全部省份 |
| GET | `/api/regions/cities` | `?provinceId=Long` → `ApiResponse<List<SimpleRegion>>` | 按省份查城市 |
| GET | `/api/regions/districts` | `?cityId=Long` → `ApiResponse<List<SimpleRegion>>` | 按城市查区县 |
| GET | `/api/user/region` | Response: `ApiResponse<RegionResponse>` (可为 null) | 当前用户已保存地域 |
| POST | `/api/user/region` | Body: `{province, city, district}` → `ApiResponse<RegionResponse>` | 保存/切换地域。自动将旧记录 isCurrent=false |
| GET | `/api/user/region/history` | Response: `ApiResponse<List<RegionRecord>>` | 地域修改历史 |

### 4.2 改造接口

**`GET /api/recommendations`**

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| scene | String | 是 | food / travel / social / outfit / career |
| province | String | 否 | 省份名称 |
| city | String | 否 | 城市名称 |
| district | String | 否 | 区县名称 |

- 不传地域 → 行为与改造前完全一致（纯通用推荐，top 10）
- 传入 province + city → 调用 DeepSeek 获取 5 个 AI 推荐 + 合并通用 top 10，最终返回 top 15
- 仅在 province 和 city 均非空时触发 AI

---

## 5. 数据库变更

### 5.1 新增表

```sql
CREATE TABLE provinces (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL UNIQUE
);

CREATE TABLE cities (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    province_id BIGINT NOT NULL,
    FOREIGN KEY (province_id) REFERENCES provinces(id)
);

CREATE TABLE districts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(40) NOT NULL,
    city_id BIGINT NOT NULL,
    FOREIGN KEY (city_id) REFERENCES cities(id)
);

CREATE TABLE user_region (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    province VARCHAR(40) NOT NULL,
    city VARCHAR(40) NOT NULL,
    district VARCHAR(40) DEFAULT '',
    is_current BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

> 注意: `provinces`/`cities`/`districts` 三张表设计了外键但不用于 Java 侧的直接加载（懒加载 + 级联查询用 Repository 方法），这样级联查询性能可控。

### 5.2 初始化数据

执行 `infra/init-region-data.sql`：

```bash
mysql -u radar -p personality_radar < infra/init-region-data.sql
```

数据覆盖: 31 省、约 340 地级市、省会及主要城市约 300 区县。

---

## 6. AI 调用策略

### 6.1 AiClient

```
AiClient.chat(systemPrompt, userMessage)
  ├─ hasKey() == false → return null (不发起HTTP请求)
  ├─ attempt 0: POST {baseUrl}/v1/chat/completions (连接3s + 读取8s超时)
  │   ├─ 成功 → 解析 choices[0].message.content → return
  │   └─ 异常 → Thread.sleep(500ms) → attempt 1
  └─ attempt 1: 同上
      ├─ 成功 → return
      └─ 异常 → log error → return null
```

### 6.2 AiRecommendationService

```
recommend(user, scene, province, city, district)
  ├─ 查 Redis: ai:rec:{uid}:{scene}:{province}:{city}:{district}
  │   └─ 命中 → 反序列化 JSON → return
  ├─ 调 getMergedScores(user) 获取 10 维度分数
  ├─ 拼装 Prompt (systemPrompt + userMessage)
  ├─ chat(systemPrompt, userMessage)
  │   ├─ null/blank → return [] (降级)
  │   └─ 解析 JSON (清理 ```json 标记)
  ├─ 写 Redis, TTL = app.ai.deepseek.cache-ttl-hours (默认 24h)
  └─ return List<LocationRecommendationResponse>
```

### 6.3 Prompt 示例

**System Prompt**:
> 你是一个精准的本地生活推荐助手。根据用户信息推荐5个位于指定城市的真实线下地点。请严格返回JSON数组，每个元素包含: title(地点名称), address(详细地址), reason(300字以内推荐理由), tags(标签数组)。只返回JSON，不要任何额外文字。

**User Message**:
> 用户性格画像（10维度分数）: OPENNESS:85, CONSCIENTIOUSNESS:60, EXTRAVERSION:70, ...
> 推荐场景: 饮食推荐
> 所在城市: 广东省 深圳市 南山区
> 请为该用户推荐5个广东省 深圳市 南山区的真实线下地点。

### 6.4 降级策略

| 场景 | 行为 |
|------|------|
| `AI_API_KEY` 未配置 | `AiClient.hasKey()` 返回 false → 不发起请求 → 退回通用推荐 |
| DeepSeek API 不可达/超时 | 2 次重试后返回 null → `RecommendationService` 只保留通用推荐 |
| AI 返回非 JSON | 解析异常 → log error → 返回空列表 → 只展示通用推荐 |
| Redis 不可用 | `StringRedisTemplate` 异常 → Spring Data Redis 自动处理，不影响请求 |

---

## 7. 前端交互流程

```
用户进入推荐页
  │
  ├─ onMounted
  │   ├─ RegionSelector: 加载省份列表
  │   └─ RegionSelector: GET /api/user/region 查询已保存地域
  │       ├─ 有地域 → 显示「当前地域: XX省 XX市 XX区」
  │       └─ 无地域 → 显示「填写所在地，获取 AI 精准推荐」
  │
  ├─ 选省份 → watch provinceId → GET /api/regions/cities?provinceId=X
  ├─ 选城市   → watch cityId     → GET /api/regions/districts?cityId=X
  ├─ 选区县(可选) → 点击「确认」→ POST /api/user/region
  │   └─ emit('regionChanged') → RecommendationsView 重新 load()
  │
  ├─ 切换 TAB (饮食/旅行/社交/穿搭/生涯)
  │   └─ watch(active) → load()
  │
  └─ load()
      └─ listRecommendations(active, region)
          └─ GET /api/recommendations?scene=X&province=X&city=X&district=X
              │
              └─ 渲染卡片:
                  ├─ item.source === "ai"
                  │   ├─ .ai-card (橙色3px左边框)
                  │   ├─ ::before "AI 精准推荐" (橙色小字角标)
                  │   ├─ .address-line (灰色地址行)
                  │   └─ .ai-reason (AI推荐理由, 斜体)
                  └─ item.source !== "ai"
                      └─ 原有卡片样式 (无特殊标记)
```

---

## 8. 配置说明

### 8.1 application.yml 新增配置段

```yaml
app:
  ai:
    deepseek:
      api-key: ${AI_API_KEY:}              # DeepSeek API Key, 为空时自动降级
      base-url: ${AI_BASE_URL:https://api.deepseek.com}
      model: ${AI_MODEL:deepseek-chat}
      timeout-seconds: ${AI_TIMEOUT_SECONDS:8}    # 读取超时秒数
      cache-ttl-hours: ${AI_CACHE_TTL_HOURS:24}   # Redis缓存有效期小时数
```

### 8.2 Docker Compose 环境变量

```yaml
backend:
  environment:
    AI_API_KEY: ${AI_API_KEY:-}
    AI_BASE_URL: ${AI_BASE_URL:-https://api.deepseek.com}
    AI_MODEL: ${AI_MODEL:-deepseek-chat}
```

在 `.env` 或部署平台配置 `AI_API_KEY=sk-xxxx` 即可启用 AI 推荐。

---

## 9. 手动部署步骤

| 步骤 | 操作 | 命令 |
|------|------|------|
| 1 | 申请 DeepSeek API Key | [platform.deepseek.com](https://platform.deepseek.com) |
| 2 | 配置环境变量 | 设置 `AI_API_KEY=sk-xxxx` |
| 3 | 导入地域数据 | `mysql -u radar -p personality_radar < infra/init-region-data.sql` |
| 4 | 确保 Redis 运行 | `docker-compose up -d redis` |
| 5 | 构建并启动后端 | `docker-compose up -d --build` |
| 6 | 启动前端 | `cd frontend && npm run dev` |

---

## 10. 测试要点

### 10.1 后端

- [ ] `AiClient.chat()` 正常返回 JSON 字符串
- [ ] `AiClient.chat()` 超时 8s 后重试 1 次，最终返回 null
- [ ] `AiProperties.hasKey()` 无 Key 时 AiClient 不发起 HTTP 请求
- [ ] `RegionService.save()` 正确维护 `is_current` 唯一性（旧记录置 false）
- [ ] `GET /api/recommendations?scene=food` (无地域) 行为与原接口一致
- [ ] `GET /api/recommendations?scene=food&province=广东省&city=深圳市` 返回混合列表
- [ ] Redis 缓存命中时第二次不调 AI
- [ ] 无效 scene 参数返回 BusinessException

### 10.2 前端

- [ ] 省市区三级下拉联动正常
- [ ] 无地域时显示引导文字
- [ ] 有地域时显示「当前地域」
- [ ] AI 推荐卡片有橙色左边框和角标
- [ ] 通用推荐卡片无特殊标记
- [ ] CAREER tab 正常切换并加载推荐
- [ ] 切换场景后地域信息不丢失
- [ ] Loading 状态下 RegionSelector 仍可操作

---

## 11. 兼容性说明

| 场景 | 行为 |
|------|------|
| 旧 API 调用方（不传地域） | 完全兼容，Controller 调用 `recommendWithRegion(user, scene, null, null, null)` 等价于原 `recommend()` |
| 未配置 AI Key | 自动降级，前端看到纯通用推荐，无感知 |
| Community tab | 不变，`switchTab('community')` 仍执行 `location.href='/community'` |
| 已有测试 | `RecommendationService.recommend()` 方法签名未变，内部转发到新方法 |

---

*文档生成时间: 2026-06-23*
