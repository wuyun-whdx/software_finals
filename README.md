# 性格雷达·生活指南

基于大五人格模型 + 生活方式偏好的综合测评系统。完成测试后获得 10 维度画像，系统据此生成个性化餐饮、旅行、社交推荐，并支持双人匹配和邀请码授权的关系分析。

## 核心功能

- **10 维度人格+生活画像**：开放性、尽责性、外向性、宜人性、情绪稳定性 + 饮食探索、饮食社交、旅行探索、旅行计划、社交能量
- **个性化推荐**：基于画像 + 场景偏好 + 用户反馈的加权推荐排序
- **双人适配**：邀请码授权机制，维度差异对比 + 契合度 + 相处建议
- **数据隐私**：测试数据仅本人可见，双人匹配只展示差异不暴露完整答题
- **管理后台**：题库管理、推荐项管理、推荐规则管理、用户管理、操作日志

## 架构

```
浏览器 → Caddy (:8090) → 前端静态文件 + /api 反向代理 → Spring Boot (:8080) → MySQL + Redis
                                    ↕
                              Cloudflare Tunnel → https://app.reflectstars.dev
```

## 目录

| 目录 | 说明 |
|------|------|
| `backend/` | Spring Boot 3 后端，Java 17+ |
| `frontend/` | Vue 3 + Vite 前端，TypeScript |
| `infra/` | Docker Compose（MySQL 8、Redis 7、后端） |
| `docs/` | 部署、接口和测试说明 |

## 快速开始

```powershell
# 一键启动（包含前端构建、Docker、默认用户、API验证、公网隧道）
cd infra
powershell -ExecutionPolicy Bypass -File .\reactivate-default-users.ps1

# 或手动分步启动
cd infra
copy .env.example .env
docker compose up -d
cd ../frontend
npm install && npm run dev
```

默认访问：`http://127.0.0.1:5173`

## 默认账号

| 角色 | 手机号 | 密码 |
|------|--------|------|
| 管理员 | `13800000000` | `Admin@123456` |
| 用户 A | `13900000001` | `User123456` |
| 用户 B | `13900000002` | `User123456` |

## 环境变量

| 变量 | 默认值 | 说明 |
|------|--------|------|
| `JWT_SECRET` | 启动脚本自动生成 | 生产环境必须显式设置 |
| `DB_URL` | `jdbc:mysql://localhost:3306/personality_radar` | 数据库连接 |
| `DB_USERNAME` | `radar` | 数据库用户 |
| `DB_PASSWORD` | `radar123` | 数据库密码 |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,...` | 允许的跨域来源 |
| `PUBLIC_BASE_URL` | `http://localhost:5173` | 公网访问地址 |
| `SPRING_JPA_HIBERNATE_DDL_AUTO` | `update` | 生产用 `validate` |
| `AI_PROVIDER` | `mock` | 本地默认使用模拟推荐，接入外部服务时设为 `real` |
| `AMAP_API_KEY` | 空 | 高德地图 API Key，`AI_PROVIDER=real` 时配置 |
| `DEEPSEEK_API_KEY` | 空 | DeepSeek API Key，`AI_PROVIDER=real` 时配置 |

## 文档

- [部署说明](docs/deployment.md)
- [接口说明](docs/api.md)
- [测试说明](docs/testing.md)
