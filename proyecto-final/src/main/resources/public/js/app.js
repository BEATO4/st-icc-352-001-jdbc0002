
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

function requireAuth() {
  const user = SurveyStorage.loadUser();
  if (!user) { window.location.href = '/login.html'; return null; }
  return user;
}

function initUserMenu() {
  const user = SurveyStorage.loadUser();
  if (!user) return;

  const avatarBtn = document.getElementById('avatar-btn');
  const userMenu = document.getElementById('user-menu');
  const menuName = document.getElementById('menu-name');
  const menuRole = document.getElementById('menu-role');

  if (avatarBtn) avatarBtn.textContent = user.username.slice(0, 2).toUpperCase();
  if (menuName) menuName.textContent = user.username;
  if (menuRole) menuRole.textContent = user.role === 'ADMIN' ? 'Administrator' : 'Field Surveyor';

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

function logout() {
  SurveyStorage.clearUser();
  window.location.href = '/login.html';
}

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
  window.addEventListener('online', update);
  window.addEventListener('offline', update);
}

function greeting(username) {
  const hr = new Date().getHours();
  const word = hr < 12 ? 'Buenos días' : hr < 18 ? 'Buenas tardes' : 'Buenas noches';
  return `${word}, ${username}`;
}

window.syncWorker = null;

if (window.Worker) {
  window.syncWorker = new Worker('/js/sync-worker.js');

  window.syncWorker.onmessage = function (event) {
    if (event.data.type === 'SYNC_SUCCESS') {
      toast('Encuesta pendiente sincronizada en segundo plano.');
      // Si estamos en el dashboard, recargar
      if (window.location.pathname.includes('dashboard.html') && typeof window.loadDashboard === 'function') {
        window.loadDashboard();
      }
    }
  };
} else {
  console.warn('Web Workers no soportados. La sincronización en segundo plano fallará.');
}

window.addEventListener('online', () => {
  if (window.syncWorker) {
    window.syncWorker.postMessage({ type: 'SYNC_NOW' });
  }
});

if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js').then(reg => {
    console.log('[SW] Registered, scope:', reg.scope);
  }).catch(err => {
    console.warn('[SW] Registration failed:', err);
  });
}
