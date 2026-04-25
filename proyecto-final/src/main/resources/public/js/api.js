const API = (() => {
  const BASE = '/api';

  function token() {
    const user = SurveyStorage.loadUser();
    return user?.token || null;
  }

  function authHeaders() {
    return {
      'Content-Type': 'application/json',
      ...(token() ? { Authorization: `Bearer ${token()}` } : {}),
    };
  }

  async function request(method, path, body) {
    let res;
    try {
      res = await fetch(BASE + path, {
        method,
        headers: authHeaders(),
        ...(body ? { body: JSON.stringify(body) } : {}),
      });
    } catch (networkErr) {
      console.error('[API] Network error on', method, path, networkErr);
      return { ok: false, status: 0, message: 'Could not reach the server. Check it is running on port 8080.' };
    }

    let data;
    try {
      data = await res.json();
    } catch {
      const text = await res.text().catch(() => '');
      console.error('[API] Non-JSON response', res.status, text.slice(0, 300));
      return { ok: false, status: res.status, message: `Server error ${res.status} — check the server console.` };
    }

    if (!res.ok) {
      console.warn('[API]', method, path, '→', res.status, data);
      return { ok: false, status: res.status, message: data.message || `Request failed (${res.status})` };
    }

    return { ok: true, status: res.status, data };
  }

  const auth = {
    register: (username, password, role) =>
        request('POST', '/auth/register', { username, password, role }),

    login: (username, password) =>
        request('POST', '/auth/login', { username, password }),

    me: () => request('GET', '/auth/me'),
  };

  const surveys = {
    getAll:    ()             => request('GET',    '/surveys'),
    getById:   (id)           => request('GET',    `/surveys/${id}`),
    create:    (payload)      => request('POST',   '/surveys', payload),
    update:    (id, payload)  => request('PUT',    `/surveys/${id}`, payload),
    delete:    (id)           => request('DELETE', `/surveys/${id}`),
  };

   async function syncQueue() {
     const queue = SurveyStorage.getPendingQueue();
     if (!queue.length) return { synced: 0, failed: 0, errors: [] };

     let synced = 0, failed = 0;
     const errors = [];

     for (const record of queue) {
       const id = record.id;   // id local necesario para actualizar el estado después

       const payload = {
         name:             record.name,
         sector:           record.sector,
         educationalLevel: record.educationalLevel,
         latitude:         record.latitude,
         longitude:        record.longitude,
         photoBase64:      record.photoBase64 || null,
       };
       const res = await surveys.create(payload);
       if (res.ok) {
         SurveyStorage.updateSurvey(id, { status: 'synced', serverId: res.data?.form?.id });
         synced++;
       } else {
         if (res.status === 401) {
           return { synced, failed: queue.length - synced, errors, authExpired: true };
         }
         errors.push({ name: record.name, reason: res.message });
         failed++;
       }
     }
     return { synced, failed, errors };
   }

  return { auth, surveys, syncQueue };
})();