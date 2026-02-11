const loginForm = document.getElementById("loginForm");
const msg = document.getElementById("msg");

if (isLoggedIn()) {
  location.href = "dashboard.html";
}

function setMessage(text, ok = false) {
  msg.textContent = text || "";
  msg.className = ok ? "ok" : "error";
}

loginForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const username = document.getElementById("loginUsername").value.trim();
  const password = document.getElementById("loginPassword").value;
  try {
    await login(username, password);
    location.href = "dashboard.html";
  } catch (err) {
    setMessage(err.message || "login failed");
  }
});
