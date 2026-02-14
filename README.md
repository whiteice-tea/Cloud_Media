# Cloud Media Temporary Upload Site

Guest users can upload video/doc files without login.  
Each file has a hard TTL (default 20 minutes) and is cleaned from both storage and database when expired.

## 1. Strong secrets (must set in project root `.env`)

Create `.env` beside `docker-compose.yml` (do not commit it):

```env
MYSQL_ROOT_PASSWORD=CHANGE_ME_TO_A_STRONG_PASSWORD_MIN_16
SPRING_DATASOURCE_USERNAME=root
SPRING_DATASOURCE_PASSWORD=CHANGE_ME_TO_A_STRONG_PASSWORD_MIN_16
JWT_SECRET=CHANGE_ME_TO_A_RANDOM_SECRET_MIN_32
ADMIN_TOKEN=CHANGE_ME_TO_A_RANDOM_TOKEN_MIN_32
STORAGE_ROOT_PATH=/data/storage
APP_MODE=PUBLIC
CORS_ALLOWED_ORIGINS=http://localhost:80,http://127.0.0.1:80,http://localhost:5500,http://127.0.0.1:5500
GUEST_TTL_MINUTES=20
GUEST_CLEANUP_INTERVAL_MS=60000
GUEST_MAX_FILES=10
GUEST_MAX_TOTAL_SIZE_BYTES=1073741824
TZ=Asia/Shanghai
```

Rules:
- `MYSQL_ROOT_PASSWORD`: strong password, at least 16 chars.
- `SPRING_DATASOURCE_PASSWORD`: strong password, at least 16 chars.
- `JWT_SECRET`: random secret, at least 32 chars.
- `ADMIN_TOKEN`: random token, at least 32 chars.
- Commit `.env.example`, never commit `.env`.

## 2. Security and behavior

- Guest identity:
  - Header required: `X-Guest-Id`
  - Allowed format: UUID or ULID
  - Missing/invalid header returns `400`
- File whitelist:
  - Extensions: `.mp4 .m4v .pdf .doc .docx`
  - MIME: `video/*`, `application/pdf`, `application/msword`,
    `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- Upload limits:
  - Backend multipart limit: `500MB`
  - Nginx: `client_max_body_size 500m`
- Rate limit:
  - Nginx upload endpoint limit per IP: `1r/s`, burst `3`
  - Exceeded requests return `429`
- Guest quota (backend):
  - Max active files per guest: `GUEST_MAX_FILES` (default `10`)
  - Max active bytes per guest: `GUEST_MAX_TOTAL_SIZE_BYTES` (default `1GB`)
  - Quota exceeded returns `429`
- TTL:
  - Every file stores `created_at`, `expires_at`, `status`
  - Read/list only returns active and unexpired files
  - Expired file access returns `410`
  - Scheduled cleanup removes expired files from disk and marks DB rows `DELETED`

## 3. Start locally

```powershell
docker compose --env-file .env config
docker compose --env-file .env up -d --build
```

If Docker is not running, start Docker Desktop first.

## 4. API summary

- `POST /api/media/upload/video`
- `POST /api/media/upload/doc`
- `POST /api/media/upload` (`type=VIDEO|DOC`)
- `GET /api/media/list?type=VIDEO|DOC`
- `GET /api/media/stream/video/{id}`
- `GET /api/media/view/doc/{id}`
- `DELETE /api/media/{id}`
- `GET /api/video/progress/{videoId}`
- `POST /api/video/progress/{videoId}`

All guest endpoints require `X-Guest-Id`.

## 5. Manual acceptance checklist

1. `docker compose --env-file .env config` succeeds.
2. `docker compose --env-file .env up -d --build` succeeds.
3. Upload works for guest without login (`mp4/pdf`).
4. Guest A cannot view Guest B files (list/stream/view isolation).
5. Set `GUEST_TTL_MINUTES=1`, wait 1+ minute, file disappears automatically.
6. Expired access returns `410`, and disk file is removed.
7. Upload rate limit and guest quota return `429`.
