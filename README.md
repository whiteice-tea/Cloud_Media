# Cloud Media Platform

小型可上线项目，功能包含：
- 注册、登录、JWT 认证
- 上传 MP4 并在线播放（支持 HTTP Range / 206）
- 上传 PDF / DOC / DOCX，在线阅读
- DOC / DOCX 自动转 PDF（LibreOffice）
- 我的文件列表、删除、资源隔离（只能访问自己的资源）
- 视频进度保存与恢复

## 1. 技术栈

- 后端：Java 17, Spring Boot 3, MyBatis-Plus, MySQL 8, jjwt
- 前端：HTML / CSS / JavaScript（无框架）
- 文档阅读：`frontend/vendor/pdfjs/web/viewer.html`
- 部署：Docker Compose（MySQL + Backend + Nginx）

## 2. 目录

```text
cloud-media/
├── backend/
├── frontend/
├── storage/
├── docker-compose.yml
├── nginx.conf
└── README.md
```

## 3. 本地运行（推荐：Docker 跑 MySQL，本机跑后端）

### 3.1 启动 MySQL

```powershell
cd E:\network\cloud-media
docker compose up -d mysql
```

### 3.2 启动后端

```powershell
cd E:\network\cloud-media\backend
mvn -DskipTests clean package
mvn spring-boot:run
```

后端默认端口：`8080`

### 3.3 启动前端

建议通过 HTTP 服务访问前端文件，不要直接双击：

```powershell
cd E:\network\cloud-media\frontend
python -m http.server 8081
```

访问：`http://127.0.0.1:8081/login.html`

## 4. Docker 一键启动（网络正常时）

```powershell
cd E:\network\cloud-media
docker compose up -d --build
```

说明：
- `docker-compose.yml` 已移除 `version` 字段（避免 Compose v2 obsolete 警告）
- 若构建 backend 拉镜像失败（网络/Docker Hub 问题），请按第 3 节方式本机启动后端

## 5. 安全与认证设计

- `/api/auth/register` 与 `/api/auth/login` 放行
- 其他 `/api/**` 默认需认证
- Spring Security 已关闭 formLogin/basic，使用 stateless JWT
- 已实现 `JwtAuthenticationFilter`，不再依赖默认 generated password 流程
- 密码使用 BCrypt 哈希入库（`users.password_hash`）

## 6. 视频与文档访问方案（上线可用）

为了解决 `<video>` / `iframe` 无法自动携带 Authorization 头的问题，项目采用：

**方案 1：短期签名 URL（推荐）**
- 后端在列表接口返回 `playUrl/viewUrl`，携带短期 `accessToken`
- token 绑定 `userId + mediaId + 类型 + 过期时间 + HMAC 签名`
- 有效期默认 10 分钟
- 不暴露主 JWT 到 query 参数

## 7. Word 转 PDF

后端通过 LibreOffice headless 执行转换：

```bash
soffice --headless --convert-to pdf --outdir <outdir> <input.docx>
```

Windows/Linux 均需安装 LibreOffice 并确保 `soffice` 在 PATH 中。

Ubuntu 示例：

```bash
sudo apt-get update
sudo apt-get install -y libreoffice
```

## 8. 关键 API

- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/media/upload/video`
- `POST /api/media/upload/doc`
- `GET /api/media/list?type=VIDEO|DOC`
- `GET /api/media/stream/video/{id}`（支持 Range）
- `GET /api/media/view/doc/{id}`
- `DELETE /api/media/{id}`
- `GET /api/video/progress/{videoId}`
- `POST /api/video/progress/{videoId}`

统一响应体：

```json
{ "code": 0, "message": "ok", "data": {} }
```

同时返回合理 HTTP 状态码（401/403/404/405/413/415/500）。

## 9. 验收命令（Windows）

注意：PowerShell 的 `curl` 是别名，请使用 **`curl.exe`**。

### 9.1 注册

```powershell
curl.exe -i -X POST "http://localhost:8080/api/auth/register" `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"u1\",\"password\":\"p123456\",\"email\":\"u1@test.com\"}"
```

### 9.2 登录

```powershell
curl.exe -i -X POST "http://localhost:8080/api/auth/login" `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"u1\",\"password\":\"p123456\"}"
```

### 9.3 验证 GET login 不再 500

```powershell
curl.exe -i "http://localhost:8080/api/auth/login"
```

预期：`405 Method Not Allowed`（或明确错误），不应是 500。

### 9.4 Range 验收（核心）

```powershell
curl.exe -I "http://localhost:8080/api/media/stream/video/1" `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Range: bytes=0-1"
```

预期必须包含：
- `HTTP/1.1 206`
- `Accept-Ranges: bytes`
- `Content-Range: bytes 0-1/...`

## 10. 常见问题

- 上传超限：返回 413 + 统一错误体，限制默认 500MB
- 文档转换失败：确认 `soffice` 可执行并查看后端日志
- Docker 拉镜像失败：先用“Docker 跑 MySQL + 本机跑后端”模式

## 11. 生产部署建议

- 修改 `jwt.secret` 为高强度密钥（至少 32 字符）
- 收紧 `cors.allowed-origins`
- 使用 Nginx 托管前端并反代 `/api`
- 按需配置 HTTPS
