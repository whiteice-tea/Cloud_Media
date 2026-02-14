# Cloud Media (Local-First Hardened Setup)

本项目已按“本地开发优先”完成上线前硬化，目标是：
- `docker compose --env-file .env up -d --build` 可直接启动
- 敏感配置全部来自 `.env` / 系统环境变量
- 去掉弱默认值（密码、JWT、Admin Token）

## 1. Sensitive Keys Where To Set

在项目根目录创建 `.env`（不要提交）：

```env
MYSQL_ROOT_PASSWORD=YOUR_STRONG_MYSQL_ROOT_PASSWORD
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=YOUR_STRONG_DB_PASSWORD
JWT_SECRET=YOUR_STRONG_JWT_SECRET_MIN_32_CHARS
ADMIN_TOKEN=YOUR_STRONG_ADMIN_TOKEN_MIN_32_CHARS
APP_MODE=PUBLIC
CORS_ALLOWED_ORIGINS=http://localhost:80,http://127.0.0.1:80,http://localhost:5500,http://127.0.0.1:5500
GUEST_TTL_MINUTES=20
GUEST_CLEANUP_INTERVAL_MS=60000
TZ=Asia/Shanghai
```

说明：
- `MYSQL_ROOT_PASSWORD`：MySQL root 密码
- `SPRING_DATASOURCE_PASSWORD`：后端连接 MySQL 密码（本地建议与 root 密码一致）
- `JWT_SECRET`：JWT 签名密钥（>=32 位随机）
- `ADMIN_TOKEN`：管理员写操作令牌（>=32 位随机）

`.env` 已被 `.gitignore` 忽略；可提交模板是 `.env.example`。

## 2. One-Click Local Start (3 commands)

```powershell
cd E:\network\cloud-media
copy .env.example .env   # 首次执行；然后手动替换为强密码/密钥
docker compose --env-file .env up -d --build
```

## 3. Validation

### 3.1 Check compose interpolation

```powershell
docker compose --env-file .env config
```

### 3.2 Check containers / logs

```powershell
docker ps
docker compose --env-file .env logs -f mysql
docker compose --env-file .env logs -f backend
```

### 3.3 Smoke API

```powershell
curl.exe -i "http://127.0.0.1/api/media/list?type=VIDEO" -H "X-Guest-Id: demo_guest_123456"
```

## 4. Hardening Done

- `docker-compose.yml`
  - MySQL 使用 `${MYSQL_ROOT_PASSWORD}`
  - MySQL 增加 `healthcheck`
  - backend `depends_on.mysql.condition=service_healthy`
  - backend 密钥类配置来自 `.env`：`SPRING_DATASOURCE_PASSWORD` / `JWT_SECRET` / `ADMIN_TOKEN`
  - 加入 `CORS_ALLOWED_ORIGINS`、访客过期参数
- `backend/src/main/resources/application.yml`
  - 移除敏感弱默认值：`SPRING_DATASOURCE_PASSWORD`、`JWT_SECRET`、`ADMIN_TOKEN`
  - CORS 默认改为 localhost 白名单，不再 `*`
- `nginx.conf`
  - `client_max_body_size 500m;`（避免大文件 413）
- `.env.example`
  - 补齐所有必要键（无真实 secret）
- `.gitignore` / `.gitattributes`
  - `.env` 忽略
  - 行尾规则放到 `.gitattributes`

## 5. Known Limitation

- `doc/docx -> pdf` 依赖 `LibreOffice(soffice)`。
- 当前 backend Docker 镜像未内置 LibreOffice；容器内对 `doc/docx` 转换会失败。
- MVP 本地建议先使用 PDF；若要容器内转换，需在 backend 镜像安装 LibreOffice。
