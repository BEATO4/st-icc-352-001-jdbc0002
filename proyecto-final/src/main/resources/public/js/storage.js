/**
 * storage.js — Offline queue and local persistence
 * Wraps localStorage so the rest of the app never touches it directly.
 */

const SurveyStorage = (() => {
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

  /** Update an existing record by its local id. */
  function updateSurvey(id, patch) {
    const surveys = loadSurveys().map(s =>
        s.id === id ? { ...s, ...patch } : s
    );
    saveSurveys(surveys);
  }

  /** Returns only the records that still need to be sent to the server. */
  function getPendingQueue() {
    return loadSurveys().filter(s => s.status === 'pending');
  }

  return { saveUser, loadUser, clearUser, loadSurveys, saveSurveys, updateSurvey, getPendingQueue };
})();