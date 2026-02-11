if (!isLoggedIn()) {
  location.href = "login.html";
}

document.getElementById("username").textContent = localStorage.getItem("username") || "";
document.getElementById("logoutBtn").addEventListener("click", logout);

const msg = document.getElementById("msg");

function setMessage(text, ok = false) {
  msg.textContent = text || "";
  msg.className = ok ? "ok" : "error";
}

async function loadList(type) {
  return await request(`/media/list?type=${encodeURIComponent(type)}`);
}

function bytesToText(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  if (bytes < 1024 * 1024 * 1024) return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  return `${(bytes / (1024 * 1024 * 1024)).toFixed(2)} GB`;
}

async function refreshLists() {
  const [videos, docs] = await Promise.all([loadList("VIDEO"), loadList("DOC")]);
  renderList("videoList", videos, "video");
  renderList("docList", docs, "doc");
}

function renderList(containerId, items, kind) {
  const el = document.getElementById(containerId);
  if (!items.length) {
    el.innerHTML = `<div class="muted">No ${kind === "video" ? "videos" : "documents"} yet</div>`;
    return;
  }

  el.innerHTML = items.map((it) => {
    const mediaUrl = kind === "video" ? it.playUrl : it.viewUrl;
    const absoluteUrl = toApiAbsoluteUrl(mediaUrl);
    return `
      <div class="list-item">
        <div>
          <a href="${kind}.html?id=${it.id}&u=${encodeURIComponent(absoluteUrl)}">${it.originalName}</a>
          <div class="muted">${bytesToText(it.sizeBytes)} · ${it.createdAt}</div>
        </div>
        <button class="danger" data-id="${it.id}" data-kind="${kind}">Delete</button>
      </div>
    `;
  }).join("");
}

document.addEventListener("click", async (e) => {
  const btn = e.target.closest("button[data-id]");
  if (!btn) return;

  const id = btn.dataset.id;
  try {
    await request(`/media/${id}`, { method: "DELETE" });
    setMessage("Deleted", true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || "Delete failed");
  }
});

document.getElementById("uploadVideoBtn").addEventListener("click", async () => {
  const file = document.getElementById("videoFile").files[0];
  if (!file) return setMessage("Please choose a video file");

  const form = new FormData();
  form.append("file", file);

  try {
    await request("/media/upload/video", { method: "POST", body: form });
    setMessage("Video uploaded", true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || "Upload failed");
  }
});

document.getElementById("uploadDocBtn").addEventListener("click", async () => {
  const file = document.getElementById("docFile").files[0];
  if (!file) return setMessage("Please choose a document file");

  const form = new FormData();
  form.append("file", file);

  try {
    await request("/media/upload/doc", { method: "POST", body: form });
    setMessage("Document uploaded", true);
    await refreshLists();
  } catch (err) {
    setMessage(err.message || "Upload failed");
  }
});

refreshLists().catch((err) => setMessage(err.message || "Load failed"));
