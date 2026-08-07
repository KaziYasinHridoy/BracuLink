// Shared API helper for Braculink static pages.

const API_BASE_URL = "http://localhost:8080";
const TOKEN_KEY = "braculink_jwt";

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
}

/**
 * Calls the Braculink API and returns the parsed `data` field of the
 * standard ApiResponse envelope. Throws an Error with the server's message
 * on failure. On 401 (no/invalid token) it clears the stored token and
 * redirects to the login page. 403 (authenticated but forbidden, e.g. the
 * friend-gate on a routine view) is left for the caller to handle.
 */
async function apiFetch(path, options = {}) {
  const token = getToken();
  const headers = {
    "Content-Type": "application/json",
    ...(options.headers || {}),
  };

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  let response;
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers,
    });
  } catch (networkError) {
    throw new Error("Could not reach the server. Is it running?");
  }

  if (response.status === 401) {
    clearToken();
    window.location.href = "index.html";
    throw new Error("Session expired, please log in again");
  }

  let body = null;
  const text = await response.text();
  if (text) {
    try {
      body = JSON.parse(text);
    } catch (parseError) {
      throw new Error("Unexpected response from server");
    }
  }

  if (!response.ok || (body && body.success === false)) {
    const message = (body && body.message) || `Request failed (${response.status})`;
    const error = new Error(message);
    error.status = response.status;
    throw error;
  }

  return body ? body.data : null;
}
