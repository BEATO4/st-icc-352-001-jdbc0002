/**
 * storage.js — Offline queue and local persistence using IndexedDB
 */

const SurveyStorage = (() => {
  const USER_KEY    = 'fieldform_user';

  /* ── USER SESSION (localStorage) ─────────── */
  function saveUser(user) {
    localStorage.setItem(USER_KEY, JSON.stringify(user));
  }
  function loadUser() {
    const raw = localStorage.getItem(USER_KEY);
    return raw ? JSON.parse(raw) : null;
  }
  function clearUser() {
    localStorage.removeItem(USER_KEY);
  }

  /* ── INDEXED DB PARA FORMULARIOS ─────────── */
  const DB_NAME = 'FormularioDB';
  const DB_VERSION = 1;
  const STORE_NAME = 'formularios';

  function openDB() {
    return new Promise((resolve, reject) => {
      const request = indexedDB.open(DB_NAME, DB_VERSION);
      request.onupgradeneeded = (event) => {
        const db = event.target.result;
        if (!db.objectStoreNames.contains(STORE_NAME)) {
          db.createObjectStore(STORE_NAME, { keyPath: 'id' });
        }
      };
      request.onsuccess = (event) => resolve(event.target.result);
      request.onerror = (event) => reject(event.target.error);
    });
  }

  /* ── SURVEYS ───────────────────────────── */
  async function loadSurveys() {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readonly');
      const store = tx.objectStore(STORE_NAME);
      const request = store.getAll();
      request.onsuccess = () => {
        const sorted = request.result.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
        resolve(sorted);
      };
      request.onerror = () => reject(request.error);
    });
  }

  async function addSurvey(survey) {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const request = store.add(survey);
      request.onsuccess = () => resolve(survey);
      request.onerror = () => reject(request.error);
    });
  }

  async function upsertSurvey(survey) {
    const db = await openDB();
    return new Promise((resolve, reject) => {
      const tx = db.transaction(STORE_NAME, 'readwrite');
      const store = tx.objectStore(STORE_NAME);
      const request = store.put(survey);
      request.onsuccess = () => resolve(survey);
      request.onerror = () => reject(request.error);
    });
  }

  async function updateSurvey(id, patch) {
    const current = await findSurvey(id);
    if (!current) return;
    const updated = { ...current, ...patch };
    return upsertSurvey(updated);
  }

  async function findSurvey(id) {
    const surveys = await loadSurveys();
    return surveys.find(s => s.id === id || s.serverId === id) || null;
  }

  async function removeSurvey(id) {
    const db      = await openDB();
    const all     = await loadSurveys();
    // Collect every entry that logically represents this record
    // (could be a local-id entry AND a server-id entry for the same form)
    const toDelete = all.filter(s => s.id === id || s.serverId === id);
    if (!toDelete.length) return;

    return Promise.all(toDelete.map(survey =>
      new Promise((resolve, reject) => {
        const tx      = db.transaction(STORE_NAME, 'readwrite');
        const store   = tx.objectStore(STORE_NAME);
        const request = store.delete(survey.id);
        request.onsuccess = () => resolve();
        request.onerror   = () => reject(request.error);
      })
    ));
  }


  async function getPendingQueue() {
    const surveys = await loadSurveys();
    return surveys.filter(s => s.status === 'pending');
  }

  return {
    saveUser,
    loadUser,
    clearUser,
    loadSurveys,
    addSurvey,
    upsertSurvey,
    updateSurvey,
    findSurvey,
    removeSurvey,
    getPendingQueue
  };
})();