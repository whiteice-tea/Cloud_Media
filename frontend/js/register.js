const registerForm = document.getElementById("registerForm");
const msg = document.getElementById("msg");

if (isLoggedIn()) {
  location.href = "dashboard.html";
}

function setMessage(text, ok = false) {
  msg.textContent = text || "";
  msg.className = ok ? "ok" : "error";
}

registerForm.addEventListener("submit", async (e) => {
  e.preventDefault();
  const username = document.getElementById("regUsername").value.trim();
  const password = document.getElementById("regPassword").value;
  const email = document.getElementById("regEmail").value.trim();
  try {
    await register(username, password, email);
    setMessage("register success, redirecting to login...", true);
    setTimeout(() => {
      location.href = "login.html";
    }, 900);
  } catch (err) {
    setMessage(err.message || "register failed");
  }
});
