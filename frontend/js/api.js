function resolveApiBase() {
  if (window.API_BASE) return window.API_BASE;
  if (location.protocol === "file:") return "http://localhost:8080/api";

  const host = location.hostname || "localhost";
  const port = location.port;
  if (!port || port === "80" || port === "443") {
    return `${location.protocol}//${host}/api`;
  }
  return `${location.protocol}//${host}:8080/api`;
}

function getOrCreateGuestId() {
  const key = "guestId";
  let guestId = localStorage.getItem(key);
  if (isValidGuestId(guestId)) return guestId;

  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    guestId = window.crypto.randomUUID();
  } else {
    guestId = fallbackUuidV4();
  }
  localStorage.setItem(key, guestId);
  return guestId;
}

function isValidGuestId(guestId) {
  if (!guestId) return false;
  const uuidV4Like = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
  const ulidLike = /^[0-9A-HJKMNP-TV-Z]{26}$/;
  return uuidV4Like.test(guestId) || ulidLike.test(guestId);
}

function fallbackUuidV4() {
  const bytes = new Uint8Array(16);
  if (window.crypto && typeof window.crypto.getRandomValues === "function") {
    window.crypto.getRandomValues(bytes);
  } else {
    for (let i = 0; i < 16; i += 1) {
      bytes[i] = Math.floor(Math.random() * 256);
    }
  }
  bytes[6] = (bytes[6] & 0x0f) | 0x40;
  bytes[8] = (bytes[8] & 0x3f) | 0x80;

  const hex = Array.from(bytes, (b) => b.toString(16).padStart(2, "0")).join("");
  return `${hex.slice(0, 8)}-${hex.slice(8, 12)}-${hex.slice(12, 16)}-${hex.slice(16, 20)}-${hex.slice(20)}`;
}

const API_BASE = resolveApiBase();
const API_ORIGIN = API_BASE.replace(/\/api\/?$/, "");

function toApiAbsoluteUrl(pathOrUrl) {
  if (!pathOrUrl) return pathOrUrl;
  if (/^https?:\/\//i.test(pathOrUrl)) return pathOrUrl;
  if (pathOrUrl.startsWith("/")) return `${API_ORIGIN}${pathOrUrl}`;
  return `${API_ORIGIN}/${pathOrUrl}`;
}

window.toApiAbsoluteUrl = toApiAbsoluteUrl;

async function request(path, options = {}) {
  const headers = new Headers(options.headers || {});
  headers.set("X-Guest-Id", getOrCreateGuestId());
  const adminToken = localStorage.getItem("adminToken");
  if (adminToken) {
    headers.set("X-Admin-Token", adminToken);
  }

  const resp = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  const contentType = resp.headers.get("content-type") || "";
  if (contentType.includes("application/json")) {
    const body = await resp.json();
    if (!resp.ok || body.code !== 0) {
      const err = new Error(body.message || `request failed (${resp.status})`);
      err.code = body.code;
      err.httpStatus = resp.status;
      throw err;
    }
    return body.data;
  }

  if (!resp.ok) {
    const errText = await resp.text();
    const err = new Error(errText || `request failed (${resp.status})`);
    err.httpStatus = resp.status;
    throw err;
  }

  return resp;
}
