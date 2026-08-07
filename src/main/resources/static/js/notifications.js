// Shared notification bell for Braculink static pages. Requires js/api.js to be loaded first.
// Loaded on every authenticated page — this is the ONE place notification rendering logic lives,
// even though the nav markup itself is copy-pasted per page.

const NOTIF_POLL_INTERVAL_MS = 30000;
const NOTIF_LIST_LIMIT = 10;

function notifEscapeHtml(value) {
  return String(value == null ? "" : value)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#39;");
}

function notifMessage(notification) {
  const payload = notification.payload || {};
  switch (notification.type) {
    case "FRIEND_REQUEST_RECEIVED":
      return `${payload.requesterName || "Someone"} sent you a friend request`;
    case "FRIEND_ACCEPTED":
      return `${payload.addresseeName || "Someone"} accepted your friend request`;
    case "SWAP_PROPOSED":
      return `You were proposed into a ${payload.courseCode || ""} swap group`.trim();
    case "SWAP_CONFIRMED":
      return `Your ${payload.courseCode || ""} swap group is fully confirmed`.trim();
    case "SWAP_DECLINED":
      return `Your ${payload.courseCode || ""} swap group fell through`.trim();
    case "SWAP_EXPIRED":
      return `Your ${payload.courseCode || ""} swap group proposal expired`.trim();
    default:
      return (notification.type || "notification").replaceAll("_", " ").toLowerCase();
  }
}

function notifFormatTime(isoString) {
  if (!isoString) return "";
  const date = new Date(isoString);
  if (Number.isNaN(date.getTime())) return "";
  return date.toLocaleString(undefined, {
    month: "short",
    day: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function notifRenderList(notifications) {
  const list = document.getElementById("notif-list");
  if (!list) return;

  if (!notifications || notifications.length === 0) {
    list.innerHTML = `<p class="mono muted text-sm" style="padding: 4px;">no notifications yet</p>`;
    return;
  }

  list.innerHTML = notifications
    .map(
      (notification) => `
      <div class="notif-row ${notification.read ? "" : "unread"}" data-id="${notification.id}">
        <p class="notif-row-message">${notifEscapeHtml(notifMessage(notification))}</p>
        <div class="notif-row-meta">
          <span class="mono muted text-sm">${notifFormatTime(notification.createdAt)}</span>
          ${notification.read ? "" : `<a href="#" class="notif-mark-read mono">mark read</a>`}
        </div>
      </div>
    `
    )
    .join("");

  list.querySelectorAll(".notif-mark-read").forEach((link) => {
    link.addEventListener("click", async (event) => {
      event.preventDefault();
      const id = event.target.closest(".notif-row").dataset.id;
      try {
        await apiFetch(`/api/notifications/${id}/read`, { method: "POST" });
        await notifRefresh();
      } catch (error) {
        // Leave the row as-is; the next open/poll will retry.
      }
    });
  });
}

async function notifRefresh() {
  const countBadge = document.getElementById("notif-count");
  const list = document.getElementById("notif-list");
  if (!countBadge || !list) return;

  try {
    const notifications = await apiFetch("/api/notifications");
    const unreadCount = notifications.filter((notification) => !notification.read).length;

    if (unreadCount > 0) {
      countBadge.textContent = unreadCount > 99 ? "99+" : String(unreadCount);
      countBadge.classList.remove("hidden");
    } else {
      countBadge.classList.add("hidden");
    }

    notifRenderList(notifications.slice(0, NOTIF_LIST_LIMIT));
  } catch (error) {
    list.innerHTML = `<p class="mono muted text-sm" style="padding: 4px;">${notifEscapeHtml(error.message)}</p>`;
  }
}

function initNotifications() {
  const button = document.getElementById("notif-bell-button");
  const panel = document.getElementById("notif-panel");
  if (!button || !panel) return;

  button.addEventListener("click", (event) => {
    event.stopPropagation();
    const opening = panel.classList.contains("hidden");
    panel.classList.toggle("hidden");
    if (opening) {
      notifRefresh();
    }
  });

  document.addEventListener("click", (event) => {
    if (!panel.classList.contains("hidden") && !panel.contains(event.target) && event.target !== button) {
      panel.classList.add("hidden");
    }
  });

  notifRefresh();
  setInterval(notifRefresh, NOTIF_POLL_INTERVAL_MS);
}
