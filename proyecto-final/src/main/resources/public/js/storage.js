/**
 * storage.js — Offline queue and local persistence
 * Wraps localStorage so the rest of the app never touches it directly.
 */

const Storage = (() => {
  const SURVEYS_KEY = 'fieldform_surveys';
  const USER_KEY    = 'fieldform_user';

  /* ── USER SESSION ──────────────────────── */
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

  /* ── SURVEYS ───────────────────────────── */
  function loadSurveys() {
    const raw = localStorage.getItem(SURVEYS_KEY);
    return raw ? JSON.parse(raw) : [];
  }
  function saveSurveys(surveys) {
    localStorage.setItem(SURVEYS_KEY, JSON.stringify(surveys));
  }

  /** Add a new survey record to the front of the list. */
  function addSurvey(record) {
    const surveys = loadSurveys();
    surveys.unshift(record);
    saveSurveys(surveys);
    return record;
  }

  /** Update an existing record by its local id. */
  function updateSurvey(id, patch) {
    const surveys = loadSurveys().map(s =>
      s.id === id ? { ...s, ...patch } : s
    );
    saveSurveys(surveys);
  }

  /** Delete a record by its local id. */
  function deleteSurvey(id) {
    const surveys = loadSurveys().filter(s => s.id !== id);
    saveSurveys(surveys);
  }

  /** Mark all 'pending' records as 'synced'. Called after a successful API sync. */
  function markAllSynced() {
    const surveys = loadSurveys().map(s =>
      s.status === 'pending' ? { ...s, status: 'synced' } : s
    );
    saveSurveys(surveys);
  }

  /** Returns only the records that still need to be sent to the server. */
  function getPendingQueue() {
    return loadSurveys().filter(s => s.status === 'pending');
  }

  return { saveUser, loadUser, clearUser, loadSurveys, addSurvey, updateSurvey, deleteSurvey, markAllSynced, getPendingQueue };
})();
