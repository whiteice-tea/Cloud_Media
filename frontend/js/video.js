const qs = new URLSearchParams(location.search);
const id = qs.get("id");
const signedUrl = qs.get("u");
const msg = document.getElementById("msg");
const video = document.getElementById("player");
const playerWrap = document.getElementById("playerWrap");
const resetSizeBtn = document.getElementById("resetSizeBtn");
const backBtn = document.getElementById("backBtn");
const VIDEO_SIZE_KEY = "video_player_size_v1";

if (!id) {
  msg.textContent = t("video.msg.missingId", "Missing video id");
} else {
  init().catch((err) => {
    msg.textContent = err.message || t("video.msg.loadFailed", "Load failed");
  });
}

backBtn.addEventListener("click", () => {
  location.href = "index.html";
});

setupResizableBox();

let timer = null;

async function init() {
  const progressData = await request(`/video/progress/${id}`);
  const seekAt = progressData.progressSeconds || 0;
  const rawUrl = signedUrl || `/api/media/stream/video/${id}`;
  video.src = toApiAbsoluteUrl(rawUrl);

  video.addEventListener("loadedmetadata", () => {
    if (seekAt > 0 && seekAt < video.duration) {
      video.currentTime = seekAt;
    }
  });

  const report = async () => {
    const seconds = Math.floor(video.currentTime || 0);
    try {
      await request(`/video/progress/${id}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ progressSeconds: seconds })
      });
    } catch (err) {
      console.warn("progress report failed", err.message);
    }
  };

  timer = setInterval(report, 10000);
  video.addEventListener("pause", report);
  window.addEventListener("beforeunload", report);
}

function setupResizableBox() {
  const saved = localStorage.getItem(VIDEO_SIZE_KEY);
  if (saved) {
    try {
      const size = JSON.parse(saved);
      if (size.width) playerWrap.style.width = `${size.width}px`;
      if (size.height) playerWrap.style.height = `${size.height}px`;
    } catch (_) {}
  }

  const observer = new ResizeObserver(() => {
    localStorage.setItem(VIDEO_SIZE_KEY, JSON.stringify({
      width: Math.round(playerWrap.offsetWidth),
      height: Math.round(playerWrap.offsetHeight)
    }));
  });
  observer.observe(playerWrap);

  resetSizeBtn.addEventListener("click", () => {
    playerWrap.style.width = "";
    playerWrap.style.height = "";
    localStorage.removeItem(VIDEO_SIZE_KEY);
  });
}
