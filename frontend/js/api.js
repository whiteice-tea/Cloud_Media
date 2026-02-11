function resolveApiBase() {
  if (window.API_BASE) return window.API_BASE;

  if (location.protocol === "file:") {
    return "http://localhost:8080/api";
  }

  const host = location.hostname || "localhost";
  const port = location.port;

  if (!port || port === "80" || port === "443") {
    return `${location.protocol}//${host}/api`;
  }

  return `${location.protocol}//${host}:8080/api`;
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
  const token = localStorage.getItem("token");
  const headers = new Headers(options.headers || {});
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const resp = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers
  });

  const contentType = resp.headers.get("content-type") || "";
  if (!contentType.includes("application/json")) {
    return resp;
  }

  const body = await resp.json();
  if (body.code === 401) {
    localStorage.removeItem("token");
    localStorage.removeItem("username");
    location.href = "login.html";
    throw new Error("unauthorized");
  }
  if (body.code !== 0) {
    throw new Error(body.message || "request failed");
  }
  return body.data;
}
