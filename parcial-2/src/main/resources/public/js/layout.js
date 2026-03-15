/* ============================================================
   EventPass — Shared Layout Builder
   Injected by each page that needs the sidebar
   ============================================================ */
function buildLayout(pageTitle, pageSubtitle) {
  const user = Auth.get();
  const role = user?.role || '';
  const initials = (user?.name || 'U').split(' ').map(w=>w[0]).slice(0,2).join('').toUpperCase();
  const path = window.location.pathname.split('/').pop() || 'dashboard.html';

  function navLink(href, icon, label, roles = null) {
    const hidden = roles && !roles.includes(role) ? 'style="display:none"' : '';
    const active = path === href ? 'active' : '';
    return `<a href="${href}" class="nav-item ${active}" ${hidden}>
              <i class="bi ${icon}"></i> ${label}
            </a>`;
  }

  document.body.insertAdjacentHTML('afterbegin', `
    <!-- BACKDROP -->
    <div class="sidebar-backdrop" id="sidebar-backdrop"></div>

    <!-- SIDEBAR -->
    <aside class="sidebar" id="sidebar">
      <a href="dashboard.html" class="sidebar-logo">
        <div class="logo-icon"><i class="bi bi-lightning-charge-fill"></i></div>
        <div class="logo-text">Event<span>Pass</span></div>
      </a>

      <nav class="sidebar-nav">
        <div class="sidebar-section">Principal</div>
        ${navLink('dashboard.html',      'bi-grid-1x2-fill',    'Dashboard')}
        ${navLink('events.html',         'bi-calendar-event',   'Eventos')}
        ${navLink('my-registrations.html','bi-ticket-perforated','Mis Inscripciones', ['PARTICIPANT','ORGANIZER','ADMIN'])}

        <div class="sidebar-section" style="margin-top:8px">Gestión</div>
        ${navLink('my-events.html',  'bi-calendar-plus',  'Mis Eventos',    ['ORGANIZER','ADMIN'])}
        ${navLink('scan.html',       'bi-qr-code-scan',   'Escanear QR',    ['ORGANIZER','ADMIN'])}

        <div class="sidebar-section" style="margin-top:8px" data-role="ADMIN" style="display:none">Admin</div>
        ${navLink('admin.html',      'bi-shield-check',   'Panel Admin',    ['ADMIN'])}
      </nav>

      <div class="sidebar-footer">
        <div class="user-card" id="sidebar-user">
          <div class="user-avatar">${initials}</div>
          <div class="user-info">
            <div class="user-name">${user?.name || '—'}</div>
            <div class="user-role">${role}</div>
          </div>
          <button class="btn-logout" title="Cerrar sesión" onclick="doLogout()">
            <i class="bi bi-box-arrow-right"></i>
          </button>
        </div>
      </div>
    </aside>

    <!-- MAIN -->
    <div class="main-content">
      <header class="topbar">
        <button class="topbar-toggle" id="sidebar-toggle">
          <i class="bi bi-list"></i>
        </button>
        <div style="flex:1">
          <div class="topbar-title">${pageTitle}</div>
          ${pageSubtitle ? `<div class="topbar-subtitle">${pageSubtitle}</div>` : ''}
        </div>
        <div style="display:flex;align-items:center;gap:12px">
          <span style="font-size:12px;color:var(--text-muted)">
            <span class="pulse-dot" style="margin-right:5px"></span>En línea
          </span>
          <div class="user-avatar" style="width:32px;height:32px;font-size:12px;cursor:pointer"
               onclick="window.location.href='dashboard.html'">${initials}</div>
        </div>
      </header>
      <div class="page-content" id="page-root">
        <!-- page content injected here -->
      </div>
    </div>

    <!-- TOASTS -->
    <div id="toast-container"></div>
  `);

  initSidebar();
}
