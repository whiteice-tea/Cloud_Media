const qs = new URLSearchParams(location.search);
const id = qs.get("id");
const signedUrl = qs.get("u");
const frame = document.getElementById("docFrame");
const docWrap = document.getElementById("docWrap");
const resetSizeBtn = document.getElementById("resetSizeBtn");
const msg = document.getElementById("msg");
const backBtn = document.getElementById("backBtn");
const DOC_SIZE_KEY = "doc_viewer_size_v1";

setupResizableBox();

if (!id) {
  msg.textContent = t("doc.msg.missingId", "Missing doc id");
} else {
  const rawUrl = signedUrl || `/api/media/view/doc/${id}`;
  const fileUrl = toApiAbsoluteUrl(rawUrl);
  frame.src = `vendor/pdfjs/web/viewer.html?file=${encodeURIComponent(fileUrl)}`;
}

backBtn.addEventListener("click", () => {
  location.href = "index.html";
});

function setupResizableBox() {
  const saved = localStorage.getItem(DOC_SIZE_KEY);
  if (saved) {
    try {
      const size = JSON.parse(saved);
      if (size.width) docWrap.style.width = `${size.width}px`;
      if (size.height) docWrap.style.height = `${size.height}px`;
    } catch (_) {}
  }

  const observer = new ResizeObserver(() => {
    localStorage.setItem(DOC_SIZE_KEY, JSON.stringify({
      width: Math.round(docWrap.offsetWidth),
      height: Math.round(docWrap.offsetHeight)
    }));
  });
  observer.observe(docWrap);

  resetSizeBtn.addEventListener("click", () => {
    docWrap.style.width = "";
    docWrap.style.height = "";
    localStorage.removeItem(DOC_SIZE_KEY);
  });
}
