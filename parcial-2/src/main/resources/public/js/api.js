/* ============================================================
   EventPass — API Layer & Utilities
   ============================================================ */

const BASE = '';   // Same origin – Javalin serves both API and static files

/* ── Fetch wrapper ── */
async function apiRequest(method, url, body = null) {
  const opts = {
    method,
    headers: { 'Content-Type': 'application/json' },
    credentials: 'same-origin'
  };
  if (body) opts.body = JSON.stringify(body);
  const res = await fetch(BASE + url, opts);
  const data = res.status === 204 ? {} : await res.json();
  if (!res.ok) throw { status: res.status, message: data.error || 'Error desconocido' };
  return data;
}

const api = {
  get:    (url)         => apiRequest('GET',    url),
  post:   (url, body)   => apiRequest('POST',   url, body),
  put:    (url, body)   => apiRequest('PUT',    url, body),
  patch:  (url, body)   => apiRequest('PATCH',  url, body),
  delete: (url)         => apiRequest('DELETE', url),

  /* Auth */
  login:    (email, password)        => api.post('/api/auth/login',    { email, password }),
  register: (email, password, name)  => api.post('/api/auth/register', { email, password, name }),
  logout:   ()                       => api.post('/api/auth/logout'),
  me:       ()                       => api.get('/api/auth/me'),

  /* Events */
  getEvents:        ()            => api.get('/api/events'),
  getMyEvents:      ()            => api.get('/api/events/mine'),
  getEvent:         (id)          => api.get(`/api/events/${id}`),
  createEvent:      (data)        => api.post('/api/events', data),
  updateEvent:      (id, data)    => api.put(`/api/events/${id}`, data),
  publishEvent:     (id, publish) => api.patch(`/api/events/${id}/publish`, { publish }),
  cancelEvent:      (id)          => api.patch(`/api/events/${id}/cancel`),

  /* Registrations */
  getMyRegistrations: ()          => api.get('/api/registrations/my'),
  getEventAttendees:  (eventId)   => api.get(`/api/registrations/event/${eventId}`),
  registerForEvent:   (eventId)   => api.post(`/api/registrations/${eventId}`),
  cancelRegistration: (eventId)   => api.delete(`/api/registrations/${eventId}`),
  scanQR:             (qrToken)   => api.post('/api/registrations/scan', { qrToken }),
  qrImageUrl:         (token)     => `${BASE}/api/registrations/qr/${token}/image`,

  /* Stats */
  getStats: (eventId) => api.get(`/api/stats/${eventId}`),

  /* Admin */
  adminGetUsers:    ()              => api.get('/api/admin/users'),
  adminGetEvents:   ()              => api.get('/api/admin/events'),
  adminBlockUser:   (id, blocked)   => api.patch(`/api/admin/users/${id}/block`, { blocked }),
  adminSetRole:     (id, role)      => api.patch(`/api/admin/users/${id}/role`, { role }),
  adminDeleteUser:  (id)            => api.delete(`/api/admin/users/${id}`),
  adminDeleteEvent: (id)            => api.delete(`/api/admin/events/${id}`)
};

/* ── Session helpers ── */
const Auth = {
  save(user) { sessionStorage.setItem('ep_user', JSON.stringify(user)); },
  get()      { try { return JSON.parse(sessionStorage.getItem('ep_user')); } catch { return null; } },
  clear()    { sessionStorage.removeItem('ep_user'); },
  isLogged() { return !!this.get(); },
  role()     { return this.get()?.role || null; },
  isAdmin()      { return this.role() === 'ADMIN'; },
  isOrganizer()  { return ['ADMIN','ORGANIZER'].includes(this.role()); }
};

/* Redirect to login if not authenticated */
function requireAuth() {
  if (!Auth.isLogged()) { window.location.href = '/login.html'; return false; }
  return true;
}
/* Redirect away from login if already authenticated */
function requireGuest() {
  if (Auth.isLogged()) { window.location.href = '/dashboard.html'; }
}

/* ── Toast notifications ── */
function showToast(message, type = 'info', title = null, duration = 4000) {
  const icons = { success: 'bi-check-circle-fill', error: 'bi-x-circle-fill', warning: 'bi-exclamation-triangle-fill', info: 'bi-info-circle-fill' };
  const titles = { success: 'Éxito', error: 'Error', warning: 'Atención', info: 'Información' };
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.innerHTML = `
    <i class="bi ${icons[type]} toast-icon"></i>
    <div class="toast-body">
      <div class="toast-title">${title || titles[type]}</div>
      <div class="toast-msg">${message}</div>
    </div>
    <button class="toast-dismiss" onclick="this.closest('.toast').remove()">
      <i class="bi bi-x"></i>
    </button>`;
  container.appendChild(toast);
  setTimeout(() => { toast.classList.add('fade-out'); setTimeout(() => toast.remove(), 300); }, duration);
}

/* ── Modal helpers ── */
function openModal(id)  { document.getElementById(id)?.classList.add('open'); }
function closeModal(id) { document.getElementById(id)?.classList.remove('open'); }
function closeAllModals() { document.querySelectorAll('.modal-overlay.open').forEach(m => m.classList.remove('open')); }
document.addEventListener('click', e => { if (e.target.matches('.modal-overlay')) closeAllModals(); });
document.addEventListener('keydown', e => { if (e.key === 'Escape') closeAllModals(); });

/* ── Loading button state ── */
function setLoading(btn, loading) {
  if (!btn) return;
  if (loading) {
    btn.dataset.originalText = btn.innerHTML;
    btn.disabled = true;
    btn.innerHTML = `<span class="spinner spinner-sm"></span> Cargando...`;
  } else {
    btn.disabled = false;
    btn.innerHTML = btn.dataset.originalText || btn.innerHTML;
  }
}

/* ── Date/time utils ── */
function fmtDateTime(dtStr) {
  if (!dtStr) return '—';
  const d = new Date(dtStr);
  return d.toLocaleString('es-DO', { dateStyle: 'medium', timeStyle: 'short' });
}
function fmtDate(dtStr) {
  if (!dtStr) return '—';
  return new Date(dtStr).toLocaleDateString('es-DO', { dateStyle: 'medium' });
}
function isUpcoming(dtStr) { return new Date(dtStr) > new Date(); }

/* ── Status badge ── */
function eventStatusBadge(status) {
  const map = {
    DRAFT:      ['gray',  'bi-pencil',       'Borrador'],
    PUBLISHED:  ['green', 'bi-broadcast',    'Publicado'],
    CANCELLED:  ['red',   'bi-x-circle',     'Cancelado'],
    COMPLETED:  ['blue',  'bi-check-circle', 'Completado']
  };
  const [color, icon, label] = map[status] || ['gray', 'bi-question', status];
  return `<span class="badge badge-${color}"><i class="bi ${icon}"></i> ${label}</span>`;
}
function roleBadge(role) {
  return `<span class="role-tag role-${role}">${role}</span>`;
}

/* ── Capacity percent ── */
function capacityBar(registered, max) {
  const pct = Math.min(100, Math.round((registered / max) * 100)) || 0;
  const cls = pct >= 90 ? 'high' : pct >= 70 ? 'mid' : '';
  return `
    <div style="font-size:11px;color:var(--text-muted);margin-bottom:4px">${registered}/${max} (<span style="color:var(--text-secondary)">${pct}%</span>)</div>
    <div class="capacity-bar"><div class="capacity-fill ${cls}" style="width:${pct}%"></div></div>`;
}

/* ── Render sidebar user info ── */
function renderUserInfo() {
  const user = Auth.get();
  if (!user) return;
  const initials = (user.name || 'U').split(' ').map(w => w[0]).slice(0,2).join('').toUpperCase();
  const el = document.getElementById('sidebar-user');
  if (el) {
    el.querySelector('.user-avatar').textContent = initials;
    el.querySelector('.user-name').textContent = user.name || user.email;
    el.querySelector('.user-role').textContent = user.role;
  }
  /* Show/hide role-restricted nav items */
  document.querySelectorAll('[data-role]').forEach(el => {
    const allowed = el.dataset.role.split(',');
    el.style.display = allowed.includes(user.role) ? '' : 'none';
  });
}

/* ── Sidebar toggle (mobile) ── */
function initSidebar() {
  const toggle   = document.getElementById('sidebar-toggle');
  const sidebar  = document.getElementById('sidebar');
  const backdrop = document.getElementById('sidebar-backdrop');
  const close    = () => { sidebar?.classList.remove('open'); backdrop?.classList.remove('open'); };

  toggle?.addEventListener('click', () => {
    sidebar?.classList.toggle('open');
    backdrop?.classList.toggle('open');
  });
  backdrop?.addEventListener('click', close);
  /* Mark active nav item */
  const path = window.location.pathname.split('/').pop() || 'dashboard.html';
  document.querySelectorAll('.nav-item[href]').forEach(a => {
    if (a.getAttribute('href') === path) a.classList.add('active');
  });
}

/* ── Logout ── */
async function doLogout() {
  try { await api.logout(); } catch {}
  Auth.clear();
  window.location.href = '/login.html';
}

/* ── Counter animation ── */
function animateCount(el, target, duration = 1200) {
  const start = 0;
  const startTime = performance.now();
  const step = (now) => {
    const progress = Math.min((now - startTime) / duration, 1);
    const ease = 1 - Math.pow(1 - progress, 3);
    el.textContent = Math.round(start + (target - start) * ease).toLocaleString();
    if (progress < 1) requestAnimationFrame(step);
  };
  requestAnimationFrame(step);
}

/* ── Generic table skeleton ── */
function tableSkeletonRows(cols, rows = 4) {
  return Array.from({length: rows}, () =>
    `<tr>${Array.from({length: cols}, () =>
      `<td><div class="skeleton skeleton-line" style="width:${60+Math.random()*30}%"></div></td>`
    ).join('')}</tr>`
  ).join('');
}

/* ── Form validation helpers ── */
function validateField(input, rule) {
  const val = input.value.trim();
  const valid = rule(val);
  input.classList.toggle('is-invalid', !valid);
  input.classList.toggle('is-valid', valid && val.length > 0);
  return valid;
}

const Rules = {
  required:    v => v.length > 0,
  email:       v => /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v),
  minLen:  n => v => v.length >= n,
  maxLen:  n => v => v.length <= n,
  positiveInt: v => Number.isInteger(Number(v)) && Number(v) > 0,
  dateTime:    v => { try { return !isNaN(new Date(v)); } catch { return false; } }
};

/* ── Export CSV ── */
function downloadCSV(rows, filename) {
  const csv = rows.map(r => r.map(c => `"${String(c).replace(/"/g,'""')}"`).join(',')).join('\n');
  const a = document.createElement('a');
  a.href = 'data:text/csv;charset=utf-8,' + encodeURIComponent(csv);
  a.download = filename;
  a.click();
}

/* ── Custom confirm dialog (replaces browser confirm()) ── */
function confirmDialog({ title = '¿Estás seguro?', message = '', type = 'danger',
                          confirmText = 'Confirmar', cancelText = 'Cancelar' } = {}) {
  return new Promise(resolve => {
    const icons = { danger: 'bi-exclamation-triangle-fill', warning: 'bi-exclamation-circle-fill', info: 'bi-info-circle-fill' };
    const overlay = document.createElement('div');
    overlay.className = 'confirm-overlay';
    overlay.innerHTML = `
      <div class="confirm-box">
        <div class="confirm-icon-wrap">
          <div class="confirm-icon ${type}"><i class="bi ${icons[type]||icons.danger}"></i></div>
        </div>
        <div class="confirm-content">
          <div class="confirm-title">${title}</div>
          ${message ? `<div class="confirm-message">${message}</div>` : ''}
        </div>
        <div class="confirm-actions">
          <button class="btn btn-secondary" id="conf-cancel">${cancelText}</button>
          <button class="btn btn-${type==='info'?'primary':type}" id="conf-ok">${confirmText}</button>
        </div>
      </div>`;
    document.body.appendChild(overlay);
    const cleanup = val => { overlay.remove(); resolve(val); };
    overlay.querySelector('#conf-ok').addEventListener('click',     () => cleanup(true));
    overlay.querySelector('#conf-cancel').addEventListener('click', () => cleanup(false));
    overlay.addEventListener('click', e => { if (e.target === overlay) cleanup(false); });
    document.addEventListener('keydown', function esc(e) {
      if (e.key === 'Escape') { cleanup(false); document.removeEventListener('keydown', esc); }
    });
  });
}

/* ── SVG Progress Ring ── */
function progressRing(pct, color = 'var(--accent)', label = '') {
  const r = 48, circ = 2 * Math.PI * r;
  const dash = (pct / 100) * circ;
  return `
    <div class="progress-ring-wrap">
      <svg width="120" height="120" viewBox="0 0 120 120">
        <circle cx="60" cy="60" r="${r}" fill="none" stroke="var(--bg-raised)" stroke-width="8"/>
        <circle cx="60" cy="60" r="${r}" fill="none" stroke="${color}" stroke-width="8"
                stroke-dasharray="${dash} ${circ}" stroke-dashoffset="${circ/4}"
                stroke-linecap="round" style="transition:stroke-dasharray 1s ease"/>
      </svg>
      <div class="ring-label">
        <div class="ring-value" style="color:${color}">${Math.round(pct)}%</div>
        ${label ? `<div class="ring-sub">${label}</div>` : ''}
      </div>
    </div>`;
}

/* ── Event status stepper ── */
function statusStepper(currentStatus) {
  const steps = [
    { key: 'DRAFT',     label: 'Borrador',   icon: '1' },
    { key: 'PUBLISHED', label: 'Publicado',  icon: '2' },
    { key: 'COMPLETED', label: 'Completado', icon: '3' }
  ];
  const order = { DRAFT: 0, PUBLISHED: 1, COMPLETED: 2, CANCELLED: -1 };
  const cur = order[currentStatus] ?? 0;
  if (currentStatus === 'CANCELLED') {
    return `<div style="text-align:center;padding:10px 0">
      <span class="badge badge-red" style="font-size:13px;padding:6px 14px">
        <i class="bi bi-x-circle"></i> Evento Cancelado
      </span></div>`;
  }
  return `<div class="status-steps">
    ${steps.map((s,i) => `
      <div class="status-step ${i < cur ? 'done' : i === cur ? 'active' : ''}">
        <div class="step-dot">${i < cur ? '<i class="bi bi-check"></i>' : s.icon}</div>
        <div class="step-label">${s.label}</div>
      </div>`).join('')}
  </div>`;
}

/* ── Debounce ── */
function debounce(fn, ms) {
  let t; return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), ms); };
}
