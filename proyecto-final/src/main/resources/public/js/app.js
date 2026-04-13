/**
 * app.js — Shared utilities used on every page.
 * Load this after storage.js and api.js.
 */

/* ── TOAST ───────────────────────────────────── */
let _toastTimer;
function toast(msg) {
  let el = document.getElementById('toast');
  if (!el) {
    el = document.createElement('div');
    el.id = 'toast';
    document.body.appendChild(el);
  }
  el.textContent = msg;
  el.classList.add('show');
  clearTimeout(_toastTimer);
  _toastTimer = setTimeout(() => el.classList.remove('show'), 2800);
}

/* ── AUTH GUARD ──────────────────────────────── */
/**
 * Call at the top of any protected page.
 * Redirects to login.html if no session found.
 * Returns the current user object.
 */
function requireAuth() {
  const user = SurveyStorage.loadUser();
  if (!user) { window.location.href = '/login.html'; return null; }
  return user;
}

/* ── USER MENU (top bar) ─────────────────────── */
function initUserMenu() {
  const user = SurveyStorage.loadUser();
  if (!user) return;

  const avatarBtn  = document.getElementById('avatar-btn');
  const userMenu   = document.getElementById('user-menu');
  const menuName   = document.getElementById('menu-name');
  const menuRole   = document.getElementById('menu-role');

  if (avatarBtn)  avatarBtn.textContent = user.username.slice(0, 2).toUpperCase();
  if (menuName)   menuName.textContent  = user.username;
  if (menuRole)   menuRole.textContent  = user.role === 'ADMIN' ? 'Administrator' : 'Field Surveyor';

  avatarBtn?.addEventListener('click', e => {
    e.stopPropagation();
    userMenu?.classList.toggle('open');
  });

  document.addEventListener('click', e => {
    if (userMenu && !userMenu.contains(e.target) && e.target !== avatarBtn) {
      userMenu.classList.remove('open');
    }
  });
}

/* ── LOGOUT ──────────────────────────────────── */
function logout() {
  SurveyStorage.clearUser();
  window.location.href = '/login.html';
}

/* ── SYNC PILL ───────────────────────────────── */
function initSyncPill() {
  const pill = document.getElementById('sync-pill');
  const text = document.getElementById('sync-text');
  if (!pill) return;

  function update() {
    const online = navigator.onLine;
    pill.className = 'sync-pill ' + (online ? 'online' : 'offline');
    text.textContent = online ? 'Online' : 'Offline';
  }
  update();
  window.addEventListener('online',  update);
  window.addEventListener('offline', update);
}

/* ── GREETING ────────────────────────────────── */
function greeting(username) {
  const hr = new Date().getHours();
  const word = hr < 12 ? 'Good morning' : hr < 18 ? 'Good afternoon' : 'Good evening';
  return `${word}, ${username}`;
}

/* ── AUTO-SYNC ON RECONNECT ──────────────────── */
window.addEventListener('online', async () => {
  const queue = SurveyStorage.getPendingQueue();
  if (!queue.length) return;
  const result = await API.syncQueue();
  if (result.synced > 0) {
    toast(`${result.synced} survey${result.synced > 1 ? 's' : ''} synced!`);
  }
});
