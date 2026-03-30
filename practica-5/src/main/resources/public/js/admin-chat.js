(function () {
    const panel = document.getElementById("admin-chat-panel");
    if (!panel) {
        return;
    }

    const sesionesLista = document.getElementById("sesiones-lista");
    const titulo = document.getElementById("chat-titulo");
    const mensajesBox = document.getElementById("admin-mensajes");
    const inputTexto = document.getElementById("admin-texto");
    const btnEnviar = document.getElementById("admin-enviar");
    const btnCerrar = document.getElementById("cerrar-chat");

    let sesiones = [];
    let sesionActiva = null;
    let ws = null;

    const escapeHtml = (value) => String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");

    const renderSesiones = () => {
        if (!sesiones.length) {
            sesionesLista.innerHTML = '<div class="p-3 text-muted">No hay conversaciones.</div>';
            return;
        }

        sesionesLista.innerHTML = sesiones.map((sesion) => {
            const active = sesionActiva === sesion.id ? "active" : "";
            const estado = sesion.abierta ? "Abierta" : "Cerrada";
            const atendido = sesion.atendidoPor ? ` | ${escapeHtml(sesion.atendidoPor)}` : "";
            return `<button class="list-group-item list-group-item-action ${active}" data-sesion-id="${sesion.id}">
                <strong>${escapeHtml(sesion.visitanteNombre)}</strong><br>
                <small>${escapeHtml(sesion.paginaOrigen)} | ${estado}${atendido}</small>
            </button>`;
        }).join("");
    };

    const renderMensaje = (mensaje) => {
        const cls = mensaje.emisorTipo === "STAFF" ? "text-primary" : "text-success";
        mensajesBox.insertAdjacentHTML("beforeend",
            `<div class="mb-2"><small class="${cls}"><strong>${escapeHtml(mensaje.emisorNombre || "")}</strong></small><br>${escapeHtml(mensaje.contenido || "")}</div>`);
        mensajesBox.scrollTop = mensajesBox.scrollHeight;
    };

    const setComposerEnabled = (enabled) => {
        inputTexto.disabled = !enabled;
        btnEnviar.disabled = !enabled;
        btnCerrar.disabled = !enabled;
    };

    const cargarSesiones = async () => {
        const response = await fetch("/api/admin/chats/sesiones");
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        sesiones = data.sesiones || [];
        renderSesiones();
    };

    const cargarMensajesSesion = async (sesionId) => {
        const response = await fetch(`/api/admin/chats/sesiones/${sesionId}/mensajes`);
        if (!response.ok) {
            return;
        }
        const data = await response.json();
        mensajesBox.innerHTML = "";
        (data.mensajes || []).forEach(renderMensaje);
    };

    const seleccionarSesion = async (sesionId) => {
        sesionActiva = sesionId;
        const sesion = sesiones.find((s) => s.id === sesionId);
        titulo.textContent = sesion ? `Chat con ${sesion.visitanteNombre}` : "Conversacion";
        setComposerEnabled(Boolean(sesion && sesion.abierta));
        renderSesiones();
        await cargarMensajesSesion(sesionId);

        if (ws && ws.readyState === WebSocket.OPEN) {
            ws.send(JSON.stringify({ accion: "unirseSesion", sesionId }));
        }
    };

    const enviarMensaje = () => {
        const contenido = (inputTexto.value || "").trim();
        if (!contenido || !sesionActiva || !ws || ws.readyState !== WebSocket.OPEN) {
            return;
        }
        ws.send(JSON.stringify({ accion: "mensaje", sesionId: sesionActiva, contenido }));
        inputTexto.value = "";
    };

    const cerrarSesion = async () => {
        if (!sesionActiva) {
            return;
        }
        const response = await fetch(`/api/admin/chats/sesiones/${sesionActiva}/cerrar`, { method: "POST" });
        if (!response.ok) {
            return;
        }
        await cargarSesiones();
        const sesion = sesiones.find((s) => s.id === sesionActiva);
        if (sesion) {
            setComposerEnabled(false);
        }
    };

    const conectarWs = () => {
        const protocol = window.location.protocol === "https:" ? "wss" : "ws";
        ws = new WebSocket(`${protocol}://${window.location.host}/ws/chat`);

        ws.addEventListener("open", () => {
            ws.send(JSON.stringify({ accion: "suscribirStaff" }));
            if (sesionActiva) {
                ws.send(JSON.stringify({ accion: "unirseSesion", sesionId: sesionActiva }));
            }
        });

        ws.addEventListener("message", (event) => {
            const payload = JSON.parse(event.data);
            if (payload.tipo === "sesion-actualizada" && payload.sesion) {
                const idx = sesiones.findIndex((s) => s.id === payload.sesion.id);
                if (idx >= 0) {
                    sesiones[idx] = payload.sesion;
                } else {
                    sesiones.unshift(payload.sesion);
                }
                renderSesiones();
            }

            if (payload.tipo === "mensaje" && payload.sesionId === sesionActiva) {
                renderMensaje(payload.mensaje);
            }

            if (payload.tipo === "sesion-cerrada" && payload.sesionId === sesionActiva) {
                setComposerEnabled(false);
            }
        });
    };

    sesionesLista.addEventListener("click", (event) => {
        const item = event.target.closest("[data-sesion-id]");
        if (!item) {
            return;
        }
        const sesionId = Number(item.dataset.sesionId || "0");
        if (sesionId > 0) {
            seleccionarSesion(sesionId);
        }
    });

    btnEnviar.addEventListener("click", enviarMensaje);
    inputTexto.addEventListener("keydown", (event) => {
        if (event.key === "Enter") {
            event.preventDefault();
            enviarMensaje();
        }
    });
    btnCerrar.addEventListener("click", cerrarSesion);

    setComposerEnabled(false);
    cargarSesiones();
    conectarWs();
})();

