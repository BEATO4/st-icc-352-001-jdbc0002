const DB_NAME    = 'FormularioDB';
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
        request.onerror  = (event) => reject(event.target.error);
    });
}

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

let ws;
const WS_URL = 'ws://' + location.host + '/sync';

const inFlight = new Map();

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
            if (response.status === 'success' && response.id) {
                const serverId = response.id;
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
        isSyncing = false;
        setTimeout(connectWebSocket, 5000);
    };

    ws.onerror = (error) => {
        console.error('[Worker] Error de WebSocket', error);
    };
}

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
            const serverId = record.serverId || record.id;
            inFlight.set(serverId, localId);

            await markAsSyncing(localId);

            ws.send(JSON.stringify(record));
        }
    } catch (error) {
        console.error('[Worker] Error al obtener datos pendientes de IndexedDB', error);
        isSyncing = false;
    }
}

self.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'SYNC_NOW') {
        syncData();
    }
});

connectWebSocket();
