// Auth guard helpers for Braculink static pages. Requires js/api.js to be loaded first.

function requireAuth() {
  if (!getToken()) {
    window.location.href = "index.html";
  }
}

function logout() {
  clearToken();
  window.location.href = "index.html";
}
