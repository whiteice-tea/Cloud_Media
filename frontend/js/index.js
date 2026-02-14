const msg = document.getElementById("msg");

function setMessage(text, ok = false) {
  msg.textContent = text || "";
  msg.className = ok ? "ok" : "error";
}

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

let latestVideos = [];
let latestDocs = [];

function setupMenu() {
  const buttons = Array.from(document.querySelectorAll(".menu-btn"));
  const panels = Array.from(document.querySelectorAll(".tool-panel"));

  buttons.forEach((btn) => {
    btn.addEventListener("click", () => {
      buttons.forEach((b) => b.classList.remove("active"));
      panels.forEach((p) => p.classList.remove("active"));
      btn.classList.add("active");
      const target = document.getElementById(btn.dataset.target);
      if (target) target.classList.add("active");
    });
  });
}

function renderList(containerId, items, kind) {
  const el = document.getElementById(containerId);
  if (!items.length) {
    el.innerHTML = `<div class="muted">${escapeHtml(t("index.noContent", "No content"))}</div>`;
    return;
  }

  el.innerHTML = items.map((it) => {
    const url = kind === "video" ? it.playUrl : it.viewUrl;
    const absoluteUrl = toApiAbsoluteUrl(url);
    return `
      <div class="list-item">
        <div class="item-main">
          <a class="item-title" title="${escapeHtml(it.originalName)}" href="${kind}.html?id=${it.id}&u=${encodeURIComponent(absoluteUrl)}">${escapeHtml(it.originalName)}</a>
          <div class="muted">${bytesToText(it.sizeBytes)} · ${it.createdAt}</div>
        </div>
        <div class="item-actions">
          <button class="danger" data-id="${it.id}">${escapeHtml(t("index.action.delete", "Delete"))}</button>
        </div>
      </div>
    `;
  }).join("");
}

async function refreshLists() {
  const [videos, docs] = await Promise.all([
    request("/media/list?type=VIDEO"),
    request("/media/list?type=DOC")
  ]);
  latestVideos = videos || [];
  latestDocs = docs || [];
  renderList("videoList", latestVideos, "video");
  renderList("docList", latestDocs, "doc");
}

document.getElementById("uploadVideoBtn").addEventListener("click", async () => {
  const input = document.getElementById("videoFile");
  const file = input.files[0];
  if (!file) {
    setMessage(t("index.msg.chooseVideo", "Please choose a video file"));
    return;
  }

  const form = new FormData();
  form.append("file", file);

  try {
    await request("/media/upload/video", { method: "POST", body: form });
    input.value = "";
    setMessage(t("index.msg.videoUploaded", "Video uploaded"), true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || t("index.msg.videoUploadFailed", "Video upload failed"));
  }
});

document.getElementById("uploadDocBtn").addEventListener("click", async () => {
  const input = document.getElementById("docFile");
  const file = input.files[0];
  if (!file) {
    setMessage(t("index.msg.chooseDoc", "Please choose a document file"));
    return;
  }

  const form = new FormData();
  form.append("file", file);

  try {
    await request("/media/upload/doc", { method: "POST", body: form });
    input.value = "";
    setMessage(t("index.msg.docUploaded", "Document uploaded"), true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || t("index.msg.docUploadFailed", "Document upload failed"));
  }
});

document.addEventListener("click", async (e) => {
  const btn = e.target.closest("button[data-id]");
  if (!btn) return;

  try {
    await request(`/media/${btn.dataset.id}`, { method: "DELETE" });
    setMessage(t("index.msg.deleted", "Deleted"), true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || t("index.msg.deleteFailed", "Delete failed"));
  }
});

document.getElementById("openVideoToolBtn").addEventListener("click", () => {
  if (!latestVideos.length) {
    alert(t("index.alert.noVideo", "No video yet"));
    return;
  }
  const v = latestVideos[0];
  location.href = `video.html?id=${v.id}&u=${encodeURIComponent(toApiAbsoluteUrl(v.playUrl))}`;
});

document.getElementById("openDocToolBtn").addEventListener("click", () => {
  if (!latestDocs.length) {
    alert(t("index.alert.noDoc", "No doc yet"));
    return;
  }
  const d = latestDocs[0];
  location.href = `doc.html?id=${d.id}&u=${encodeURIComponent(toApiAbsoluteUrl(d.viewUrl))}`;
});

refreshLists().catch((err) => setMessage(err.message || t("index.msg.loadFailed", "Load failed")));
setupMenu();
window.addEventListener("languagechange", () => {
  refreshLists().catch(() => {});
});
