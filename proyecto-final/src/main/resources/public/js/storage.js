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

  function addSurvey(survey) {
    const surveys = loadSurveys();
    surveys.unshift(survey);
    saveSurveys(surveys);
    return survey;
  }

  /** Insert or replace a record by id/serverId to keep cache in sync. */
  function upsertSurvey(survey) {
    const surveys = loadSurveys();
    const idx = surveys.findIndex(s =>
      s.id === survey.id ||
      (s.serverId && survey.id && s.serverId === survey.id) ||
      (survey.serverId && s.id === survey.serverId)
    );

    if (idx >= 0) {
      surveys[idx] = { ...surveys[idx], ...survey };
    } else {
      surveys.unshift(survey);
    }
    saveSurveys(surveys);
  }

  /** Update an existing record by its local id. */
  function updateSurvey(id, patch) {
    const surveys = loadSurveys().map(s =>
        s.id === id ? { ...s, ...patch } : s
    );
    saveSurveys(surveys);
  }

  /** Find one record by local id or server id. */
  function findSurvey(id) {
    return loadSurveys().find(s => s.id === id || s.serverId === id) || null;
  }

  /** Remove a record by local id or server id. */
  function removeSurvey(id) {
    const surveys = loadSurveys().filter(s => s.id !== id && s.serverId !== id);
    saveSurveys(surveys);
  }

  /** Returns only the records that still need to be sent to the server. */
  function getPendingQueue() {
    return loadSurveys().filter(s => s.status === 'pending');
  }

  return {
    saveUser,
    loadUser,
    clearUser,
    loadSurveys,
    saveSurveys,
    addSurvey,
    upsertSurvey,
    updateSurvey,
    findSurvey,
    removeSurvey,
    getPendingQueue
  };
})();