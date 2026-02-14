const msg = document.getElementById("msg");
const tokenMsg = document.getElementById("tokenMsg");
const adminTokenInput = document.getElementById("adminTokenInput");

function setMessage(text, ok = false) {
  msg.textContent = text || "";
  msg.className = ok ? "ok" : "error";
}

function setTokenMessage() {
  const token = localStorage.getItem("adminToken");
  tokenMsg.textContent = token ? "管理员 Token 已设置" : "未设置管理员 Token";
  adminTokenInput.value = token || "";
}

document.getElementById("saveAdminTokenBtn").addEventListener("click", () => {
  const token = adminTokenInput.value.trim();
  if (!token) {
    setMessage("请先输入管理员 Token");
    return;
  }
  localStorage.setItem("adminToken", token);
  setTokenMessage();
  setMessage("Token 保存成功", true);
});

document.getElementById("clearAdminTokenBtn").addEventListener("click", () => {
  localStorage.removeItem("adminToken");
  setTokenMessage();
  setMessage("Token 已清除", true);
});

function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll("\"", "&quot;")
    .replaceAll("'", "&#39;");
}

function bytesToText(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

async function loadList(type) {
  return request(`/media/list?type=${encodeURIComponent(type)}`);
}

function renderList(containerId, items, kind) {
  const el = document.getElementById(containerId);
  if (!items.length) {
    el.innerHTML = '<div class="muted">暂无内容</div>';
    return;
  }
  el.innerHTML = items.map((it) => {
    const streamUrl = kind === "video" ? it.playUrl : it.viewUrl;
    return `
      <div class="list-item">
        <div class="item-main">
          <a class="item-title" title="${escapeHtml(it.originalName)}" href="${kind}.html?id=${it.id}&u=${encodeURIComponent(toApiAbsoluteUrl(streamUrl))}">${escapeHtml(it.originalName)}</a>
          <div class="muted">${bytesToText(it.sizeBytes)} · ${it.createdAt}</div>
        </div>
        <div class="item-actions">
          <button class="danger" data-id="${it.id}">删除</button>
        </div>
      </div>
    `;
  }).join("");
}

async function refreshLists() {
  const [videos, docs] = await Promise.all([loadList("VIDEO"), loadList("DOC")]);
  renderList("videoList", videos, "video");
  renderList("docList", docs, "doc");
}

document.addEventListener("click", async (e) => {
  const btn = e.target.closest("button[data-id]");
  if (!btn) return;
  try {
    await request(`/media/${btn.dataset.id}`, { method: "DELETE" });
    setMessage("删除成功", true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || "删除失败");
  }
});

document.getElementById("uploadVideoBtn").addEventListener("click", async () => {
  const fileInput = document.getElementById("videoFile");
  const file = fileInput.files[0];
  if (!file) {
    setMessage("请选择视频文件");
    return;
  }

  const form = new FormData();
  form.append("file", file);

  try {
    await request("/media/upload/video", { method: "POST", body: form });
    fileInput.value = "";
    setMessage("视频上传成功", true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || "上传失败");
  }
});

document.getElementById("uploadDocBtn").addEventListener("click", async () => {
  const fileInput = document.getElementById("docFile");
  const file = fileInput.files[0];
  if (!file) {
    setMessage("请选择文档文件");
    return;
  }

  const form = new FormData();
  form.append("file", file);

  try {
    await request("/media/upload/doc", { method: "POST", body: form });
    fileInput.value = "";
    setMessage("文档上传成功", true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || "上传失败");
  }
});

setTokenMessage();
refreshLists().catch((err) => setMessage(err.message || "加载失败"));
