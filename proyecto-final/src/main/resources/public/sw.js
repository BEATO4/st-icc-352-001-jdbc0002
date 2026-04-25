/**
 * sw.js — FieldForm Service Worker
 * - Cache-first for static assets (HTML, CSS, JS, fonts, images)
 * - Network-first for API and WebSocket routes (pass-through)
 * - Offline fallback: serve cached page when network unavailable
 */

const CACHE_NAME   = 'fieldform-v2';
const STATIC_URLS  = [
  '/',
  '/login.html',
  '/dashboard.html',
  '/form.html',
  '/map.html',
  '/css/style.css',
  '/js/storage.js',
  '/js/api.js',
  '/js/app.js',
  '/js/sync-worker.js',
  // Leaflet — cached from CDN as well
  'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.css',
  'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/leaflet.min.js',
];

// ── Install: pre-cache all static assets ──────────────────────────────────
self.addEventListener('install', event => {
  event.waitUntil(
    caches.open(CACHE_NAME).then(cache => {
      return cache.addAll(STATIC_URLS);
    }).then(() => self.skipWaiting())
  );
});

// ── Activate: remove old caches ───────────────────────────────────────────
self.addEventListener('activate', event => {
  event.waitUntil(
    caches.keys().then(keys =>
      Promise.all(
        keys.filter(k => k !== CACHE_NAME).map(k => caches.delete(k))
      )
    ).then(() => self.clients.claim())
  );
});

// ── Fetch: strategy per request type ─────────────────────────────────────
self.addEventListener('fetch', event => {
  const url = new URL(event.request.url);

  // Let WebSocket and API requests go straight to the network (no caching)
  if (url.pathname.startsWith('/api/') || url.pathname === '/sync') {
    return; // browser handles normally
  }

  // For everything else: cache-first, network fallback
  event.respondWith(
    caches.match(event.request).then(cached => {
      if (cached) return cached;

      return fetch(event.request).then(response => {
        // Only cache valid GET responses
        if (
          event.request.method !== 'GET' ||
          !response ||
          response.status !== 200 ||
          response.type === 'error'
        ) {
          return response;
        }

        const clone = response.clone();
        caches.open(CACHE_NAME).then(cache => cache.put(event.request, clone));
        return response;
      }).catch(() => {
        // Offline and not in cache: for navigation requests return dashboard
        if (event.request.mode === 'navigate') {
          return caches.match('/dashboard.html');
        }
      });
    })
  );
});
