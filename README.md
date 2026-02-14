# Cloud Media Tools (Public Guest Mode)

Cloud Media 现在是公开工具站：
- 无需注册/登录，任何访客可直接上传视频和文档并在线使用
- 每个访客自动分配独立 Guest ID，只能看到/删除自己的内容
- 访客无操作 20 分钟后，数据会自动清理

## 1. 环境要求

- Docker Desktop
- Java 17
- Maven 3.9+
- （可选）LibreOffice（用于 doc/docx 转 pdf）

## 2. 配置 `.env`

```env
MYSQL_ROOT_PASSWORD=root
JWT_SECRET=change_this_jwt_secret_at_least_32_chars
APP_MODE=PUBLIC
GUEST_TTL_MINUTES=20
GUEST_CLEANUP_INTERVAL_MS=60000
TZ=Asia/Shanghai
```

## 3. 启动

### 3.1 MySQL（Docker）

```powershell
cd E:\network\cloud-media
docker compose -f docker-compose.yml -f docker-compose.dev.yml up -d mysql
```

### 3.2 后端（本机）

```powershell
cd E:\network\cloud-media\backend
$env:SPRING_DATASOURCE_URL='jdbc:mysql://127.0.0.1:3306/cloud_media?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true'
$env:SPRING_DATASOURCE_USERNAME='root'
$env:SPRING_DATASOURCE_PASSWORD='root'
$env:APP_MODE='PUBLIC'
$env:GUEST_TTL_MINUTES='20'
mvn spring-boot:run
```

### 3.3 前端

使用 Live Server 打开：
- `http://127.0.0.1:5500/index.html`

## 4. 当前行为（关键）

- 上传/列表/删除：不需要管理员 Token
- 前端会自动创建并发送 `X-Guest-Id`
- 每个访客只能操作自己的文件
- 视频流和文档阅读继续支持 signed url + Range

## 5. 自测命令

```powershell
curl.exe -i "http://127.0.0.1:8080/api/media/list?type=VIDEO" -H "X-Guest-Id: demo_guest_123456"
```

如果返回 `code=0`，说明访客模式正常。
