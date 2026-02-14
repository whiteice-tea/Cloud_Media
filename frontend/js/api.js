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
  if (guestId) return guestId;

  if (window.crypto && typeof window.crypto.randomUUID === "function") {
    guestId = window.crypto.randomUUID().replaceAll("-", "");
  } else {
    guestId = `${Date.now()}_${Math.random().toString(36).slice(2, 14)}`;
  }
  localStorage.setItem(key, guestId);
  return guestId;
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
