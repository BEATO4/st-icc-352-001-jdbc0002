const DB_NAME    = 'FormularioDB';
const DB_VERSION = 1;
const STORE_NAME = 'formularios';

// ── Utilidad para abrir IndexedDB ───────────────────────────────────────────
// Schema MUST match storage.js: keyPath 'id', NO autoIncrement
function openDB() {
    return new Promise((resolve, reject) => {
        const request = indexedDB.open(DB_NAME, DB_VERSION);
        request.onupgradeneeded = (event) => {
            const db = event.target.result;
            if (!db.objectStoreNames.contains(STORE_NAME)) {
                // keyPath is the string MongoDB _id, no autoIncrement
                db.createObjectStore(STORE_NAME, { keyPath: 'id' });
            }
        };
        request.onsuccess = (event) => resolve(event.target.result);
        request.onerror  = (event) => reject(event.target.error);
    });
}

// Obtener todos los formularios pendientes (status === 'pending')
function getPendingFormularios() {
    return openDB().then(db => {
        return new Promise((resolve, reject) => {
            const tx      = db.transaction(STORE_NAME, 'readonly');
            const store   = tx.objectStore(STORE_NAME);
            const request = store.getAll();
            request.onsuccess = () => {
                const all     = request.result || [];
                const pending = all.filter(r => r.status === 'pending');
                resolve(pending);
            };
            request.onerror = () => reject(request.error);
        });
    });
}

// Marcar un registro como 'syncing' para evitar reenvíos en una misma sesión
function markAsSyncing(localId) {
    return openDB().then(db => {
        return new Promise((resolve, reject) => {
            const tx      = db.transaction(STORE_NAME, 'readwrite');
            const store   = tx.objectStore(STORE_NAME);
            const getReq  = store.get(localId);
            getReq.onsuccess = () => {
                if (!getReq.result) { resolve(); return; }
                const updated = { ...getReq.result, status: 'syncing' };
                const putReq  = store.put(updated);
                putReq.onsuccess = () => resolve();
                putReq.onerror   = () => reject(putReq.error);
            };
            getReq.onerror = () => reject(getReq.error);
        });
    });
}

// Eliminar un formulario de IndexedDB por su clave local (string _id)
function deleteFormulario(localId) {
    return openDB().then(db => {
        return new Promise((resolve, reject) => {
            const tx      = db.transaction(STORE_NAME, 'readwrite');
            const store   = tx.objectStore(STORE_NAME);
            const request = store.delete(localId);
            request.onsuccess = () => resolve();
            request.onerror   = () => reject(request.error);
        });
    });
}

// ── WebSocket ───────────────────────────────────────────────────────────────
let ws;
const WS_URL = 'ws://' + location.host + '/sync';

// Map: serverId (returned by server) → localId (IndexedDB key)
// Populated before sending so the ACK handler can look up the right key.
const inFlight = new Map();

// Flag to prevent duplicate syncData calls while one is already running
let isSyncing = false;

function connectWebSocket() {
    ws = new WebSocket(WS_URL);

    ws.onopen = () => {
        console.log('[Worker] WebSocket conectado.');
        syncData();
    };

    ws.onmessage = (event) => {
        try {
            const response = JSON.parse(event.data);
            // Server responds with { status: 'success', id: <serverId> }
            if (response.status === 'success' && response.id) {
                const serverId = response.id;
                // Resolve the local IndexedDB key from the in-flight map
                const localId  = inFlight.get(serverId) || serverId;
                inFlight.delete(serverId);

                console.log(`[Worker] Registro ${serverId} sincronizado exitosamente.`);
                deleteFormulario(localId).then(() => {
                    console.log(`[Worker] Registro ${localId} borrado de IndexedDB local.`);
                    postMessage({ type: 'SYNC_SUCCESS', id: serverId });
                }).catch(err => {
                    console.error(`[Worker] No se pudo borrar ${localId} de IndexedDB`, err);
                });
            }
        } catch (e) {
            console.error('[Worker] Error procesando mensaje de servidor', e);
        }
    };

    ws.onclose = () => {
        console.log('[Worker] WebSocket desconectado. Reintentando en 5 segundos...');
        isSyncing = false; // reset lock so next connection can sync
        setTimeout(connectWebSocket, 5000);
    };

    ws.onerror = (error) => {
        console.error('[Worker] Error de WebSocket', error);
    };
}

// ── Sincronización ──────────────────────────────────────────────────────────
async function syncData() {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
        console.log('[Worker] WebSocket no está listo para sincronizar.');
        return;
    }
    if (isSyncing) {
        console.log('[Worker] Sincronización ya en curso, omitiendo.');
        return;
    }
    isSyncing = true;

    try {
        const pending = await getPendingFormularios();
        if (pending.length === 0) {
            console.log('[Worker] No hay datos pendientes para sincronizar.');
            isSyncing = false;
            return;
        }

        console.log(`[Worker] Sincronizando ${pending.length} registros pendientes...`);

        for (const record of pending) {
            const localId  = record.id;
            // The server will echo back whatever id the record carries;
            // map serverId → localId before sending.
            const serverId = record.serverId || record.id;
            inFlight.set(serverId, localId);

            // Mark as 'syncing' so a reconnect won't re-send this record
            await markAsSyncing(localId);

            ws.send(JSON.stringify(record));
        }
    } catch (error) {
        console.error('[Worker] Error al obtener datos pendientes de IndexedDB', error);
        isSyncing = false;
    }
}

// ── Mensajes del hilo principal ─────────────────────────────────────────────
self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SYNC_NOW') {
        syncData();
    }
});

// Inicializar la conexión al cargar el worker
connectWebSocket();
