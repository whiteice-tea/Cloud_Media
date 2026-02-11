if (isLoggedIn()) {
  location.href = "dashboard.html";
} else {
  document.getElementById("goRegister").href = "register.html";
}
