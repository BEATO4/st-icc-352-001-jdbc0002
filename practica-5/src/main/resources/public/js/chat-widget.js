(function () {
    const root = document.getElementById("chat-widget");
    if (!root) return;

    // Detectar usuario PRIMERO, antes de usar variables dependientes
    const usuarioNombre = root.dataset.usuarioNombre || "";
    const esAdmin = root.dataset.esAdmin === "true";
    const esAutor = root.dataset.esAutor === "true";
    let esVisitante = !(usuarioNombre && (esAdmin || esAutor));
    
    console.log("Usuario:", usuarioNombre, "Es Admin:", esAdmin, "Es Autor:", esAutor, "Es Visitante:", esVisitante);

    const chatTabs = document.getElementById("chat-tabs");
    const mensajesBox = document.getElementById("chat-mensajes");
    const inputTexto = document.getElementById("chat-texto");
    const btnEnviar = document.getElementById("chat-enviar");
    const btnNuevoChat = document.getElementById("chat-nuevo-btn");
    const chatAcciones = document.getElementById("chat-acciones");

    let ws = null;
    let sesionActualId = null;
    let sesionActualToken = null;
    let sesiones = {};

    const pagina = root.dataset.page || "inicio";
    // Admin/Autor: usan clave global. Visitantes: clave por página
    const lsKey = `chat.sesiones.${esVisitante ? pagina : "GLOBAL"}`;
    const lsNombreInvitado = `chat.nombre-invitado.${pagina}`;

    const escapeHtml = (value) => String(value)
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;");

    const formatearHoraLocal = () => {
        const ahora = new Date();
        const horas = String(ahora.getHours()).padStart(2, "0");
        const minutos = String(ahora.getMinutes()).padStart(2, "0");
        return `${horas}:${minutos}`;
    };

    const appendMessage = (mensaje) => {
        const tipo = mensaje.emisorTipo === "STAFF" ? "text-primary" : "text-secondary";
        // Usar hora local del cliente, no la del servidor
        const hora = formatearHoraLocal();
        const html = `<div class="mb-2"><small class="${tipo}"><strong>${escapeHtml(mensaje.emisorNombre || "")}</strong> <span class="text-muted">${hora}</span></small><br>${escapeHtml(mensaje.contenido || "")}</div>`;
        mensajesBox.insertAdjacentHTML("beforeend", html);
        mensajesBox.scrollTop = mensajesBox.scrollHeight;
    };

    const appendSystem = (texto) => {
        mensajesBox.insertAdjacentHTML("beforeend", `<div class="mb-2"><small class="text-muted">${escapeHtml(texto)}</small></div>`);
        mensajesBox.scrollTop = mensajesBox.scrollHeight;
    };

    const conectarWebSocket = () => {
        if (ws && ws.readyState === WebSocket.OPEN) {
            console.log("WebSocket ya está abierto.");
            return;
        }
        
        console.log("Iniciando WebSocket...");
        const protocol = window.location.protocol === "https:" ? "wss" : "ws";
        ws = new WebSocket(`${protocol}://${window.location.host}/ws/chat`);

        ws.addEventListener("open", () => {
            console.log("WebSocket ABIERTO");
            if (sesionActualId && sesionActualToken) {
                console.log("Suscribiendo a sesión:", sesionActualId);
                ws.send(JSON.stringify({ accion: "suscribirVisitante", sesionId: sesionActualId, token: sesionActualToken }));
                appendSystem("Conectado al chat.");
            } else {
                console.warn("No hay sesión para suscribirse:", { sesionId: sesionActualId, token: sesionActualToken });
            }
        });

        ws.addEventListener("message", (event) => {
            try {
                console.log("Mensaje WS recibido:", event.data.substring(0, 100));
                const payload = JSON.parse(event.data);
                if (payload.tipo === "mensaje" && payload.sesionId === sesionActualId) {
                    appendMessage(payload.mensaje);
                }
                if (payload.tipo === "sesion-cerrada" && payload.sesionId === sesionActualId) {
                    appendSystem(payload.mensaje || "La conversación ha finalizado. Gracias.");
                    inputTexto.disabled = true;
                    btnEnviar.disabled = true;
                }
                if (payload.tipo === "error") {
                    appendSystem("❌ " + (payload.mensaje || "Error de chat."));
                }
            } catch (e) {
                console.error("Error procesando mensaje WS:", e);
            }
        });

        ws.addEventListener("close", () => {
            console.log("WebSocket CERRADO");
            appendSystem("Conexion cerrada.");
        });

        ws.addEventListener("error", (event) => {
            console.error("WebSocket ERROR:", event);
            appendSystem("❌ Error de conexion WebSocket.");
        });
    };

    const cargarHistorial = async (sesionId, token) => {
        try {
            const response = await fetch(`/api/chat/sesiones/${sesionId}/mensajes?token=${encodeURIComponent(token)}`);
            if (!response.ok) return;
            const data = await response.json();
            mensajesBox.innerHTML = "";
            (data.mensajes || []).forEach(appendMessage);
        } catch (e) {
            console.error("Error cargando historial:", e);
        }
    };

    const actualizarUI = () => {
        // Actualizar tabs de chats
        chatTabs.innerHTML = "";
        Object.values(sesiones).forEach(sesion => {
            const isActive = sesion.id === sesionActualId;
            const tab = document.createElement("button");
            tab.className = `btn btn-sm ${isActive ? "btn-primary" : "btn-outline-primary"} me-2 mb-2`;
            const estado = sesion.abierta ? "" : " (Cerrado)";
            tab.textContent = `${escapeHtml(sesion.nombreChat)}${estado}`;
            tab.onclick = () => cambiarChat(sesion.id, sesion.token);
            chatTabs.appendChild(tab);
        });

        // Habilitar/deshabilitar input según la sesión actual
        const sesionActual = sesiones[sesionActualId];
        if (sesionActual && sesionActual.abierta) {
            inputTexto.disabled = false;
            btnEnviar.disabled = false;
        } else {
            inputTexto.disabled = true;
            btnEnviar.disabled = true;
        }

        // Actualizar botones de acciones (SOLO para admin/autor)
        chatAcciones.innerHTML = "";
        if (!esVisitante && chatAcciones && sesionActual) {
            if (sesionActual.abierta) {
                const btnCerrar = document.createElement("button");
                btnCerrar.className = "btn btn-sm btn-warning";
                btnCerrar.textContent = "Cerrar chat";
                btnCerrar.onclick = () => cerrarChat(sesionActualId);
                chatAcciones.appendChild(btnCerrar);
            } else {
                const btnReabrir = document.createElement("button");
                btnReabrir.className = "btn btn-sm btn-info";
                btnReabrir.textContent = "Reabrir";
                btnReabrir.onclick = () => reabrirChat(sesionActualId);
                chatAcciones.appendChild(btnReabrir);

                const btnEliminar = document.createElement("button");
                btnEliminar.className = "btn btn-sm btn-danger ms-2";
                btnEliminar.textContent = "Eliminar";
                btnEliminar.onclick = () => eliminarChat(sesionActualId);
                chatAcciones.appendChild(btnEliminar);
            }
        }

        // Ocultar botón "Nuevo chat" si es visitante
        if (btnNuevoChat) {
            btnNuevoChat.style.display = esVisitante ? "none" : "inline-block";
        }

        guardarSesionesLocal();
    };

    const cambiarChat = async (sesionId, token) => {
        console.log("Cambiando a chat:", { sesionId, token: token?.substring(0, 10) });
        sesionActualId = sesionId;
        sesionActualToken = token;
        await cargarHistorial(sesionId, token);
        
        // Esperar a que el WebSocket esté listo
        if (!ws || ws.readyState !== WebSocket.OPEN) {
            console.log("WebSocket no está listo, esperando...");
            let intentos = 0;
            const esperar = setInterval(() => {
                if (ws && ws.readyState === WebSocket.OPEN) {
                    clearInterval(esperar);
                    console.log("WebSocket listo, suscribiendo...");
                    ws.send(JSON.stringify({ accion: "suscribirVisitante", sesionId, token }));
                } else if (++intentos > 10) {
                    clearInterval(esperar);
                    console.warn("Timeout esperando WebSocket");
                }
            }, 100);
        } else {
            console.log("WebSocket ya abierto, suscribiendo...");
            ws.send(JSON.stringify({ accion: "suscribirVisitante", sesionId, token }));
        }
        
        actualizarUI();
    };

    const crearNuevoChat = async () => {
        const nombreChat = prompt("Nombre del nuevo chat:", "Chat " + (Object.keys(sesiones).length + 1));
        if (!nombreChat) return;

        try {
            const response = await fetch("/api/chat/sesiones", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ nombreChat, pagina })
            });

            if (!response.ok) {
                const error = await response.json();
                appendSystem(error.error || "No se pudo crear el chat.");
                return;
            }

            const data = await response.json();
            sesiones[data.id] = {
                id: data.id,
                token: data.token,
                nombreChat: data.nombreChat,
                abierta: true
            };

            await cambiarChat(data.id, data.token);
            appendSystem(`Chat "${data.nombreChat}" creado.`);
        } catch (e) {
            console.error("Error creando chat:", e);
            appendSystem("Error al crear chat.");
        }
    };

    const cerrarChat = async (sesionId) => {
        try {
            const response = await fetch(`/api/chat/sesiones/${sesionId}/cerrar`, {
                method: "POST"
            });

            if (!response.ok) {
                const error = await response.json();
                appendSystem(error.error || "No se pudo cerrar.");
                return;
            }

            sesiones[sesionId].abierta = false;
            actualizarUI();
            appendSystem("Chat cerrado.");
        } catch (e) {
            console.error("Error cerrando:", e);
        }
    };

    const reabrirChat = async (sesionId) => {
        try {
            const response = await fetch(`/api/chat/sesiones/${sesionId}/reabrir`, {
                method: "POST"
            });

            if (!response.ok) {
                const error = await response.json();
                appendSystem(error.error || "No se pudo reabrir.");
                return;
            }

            sesiones[sesionId].abierta = true;
            actualizarUI();
            appendSystem("Chat reabierto.");
        } catch (e) {
            console.error("Error reabriendo:", e);
        }
    };

    const eliminarChat = async (sesionId) => {
        if (!confirm("¿Eliminar este chat permanentemente?")) return;

        try {
            const response = await fetch(`/api/chat/sesiones/${sesionId}`, {
                method: "DELETE"
            });

            if (!response.ok) {
                const error = await response.json();
                appendSystem(error.error || "No se pudo eliminar.");
                return;
            }

            delete sesiones[sesionId];
            const otrasSesiones = Object.values(sesiones);
            if (otrasSesiones.length > 0) {
                await cambiarChat(otrasSesiones[0].id, otrasSesiones[0].token);
            } else {
                sesionActualId = null;
                sesionActualToken = null;
                mensajesBox.innerHTML = "";
            }
            
            actualizarUI();
            appendSystem("Chat eliminado.");
        } catch (e) {
            console.error("Error eliminando:", e);
        }
    };

    const guardarSesionesLocal = () => {
        localStorage.setItem(lsKey, JSON.stringify(sesiones));
    };

    const cargarSesionesLocal = () => {
        try {
            const stored = localStorage.getItem(lsKey);
            if (stored) sesiones = JSON.parse(stored);
        } catch (e) {
            console.error("Error cargando sesiones:", e);
            sesiones = {};
        }
    };

    const cargarSesionesDelServidor = async () => {
        try {
            const response = await fetch(`/api/chat/sesiones-pagina?pagina=${encodeURIComponent(pagina)}`);
            if (!response.ok) return;

            const data = await response.json();
            sesiones = {};
            (data.sesiones || []).forEach(s => {
                sesiones[s.id] = {
                    id: s.id,
                    token: s.token,
                    nombreChat: s.nombreChat,
                    abierta: s.abierta
                };
            });

            guardarSesionesLocal();
        } catch (e) {
            console.error("Error cargando del servidor:", e);
        }
    };

    const unirseGeneralOPrimero = async () => {
        // Admin/Autor usan su nombre de usuario. Visitantes preguntan
        let nombreInvitado;
        
        if (esVisitante) {
            nombreInvitado = localStorage.getItem(lsNombreInvitado) || prompt("Tu nombre:", "Visitante");
            if (!nombreInvitado) {
                appendSystem("Necesitas indicar tu nombre para usar el chat.");
                return;
            }
            localStorage.setItem(lsNombreInvitado, nombreInvitado);
        } else {
            // Admin/Autor: usar su nombre de usuario
            const usuarioNombre = root.dataset.usuarioNombre || "Admin";
            nombreInvitado = usuarioNombre;
        }

        try {
            // Visitante se une sin nombreChat → se une a "General"
            const response = await fetch("/api/chat/sesiones", {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({ nombreInvitado, pagina })
            });

            if (!response.ok) {
                const error = await response.json();
                appendSystem(error.error || "No se pudo unirse al chat.");
                return;
            }

            const data = await response.json();
            sesiones[data.id] = {
                id: data.id,
                token: data.token,
                nombreChat: data.nombreChat,
                abierta: true
            };

            await cambiarChat(data.id, data.token);
        } catch (e) {
            console.error("Error uniéndose:", e);
            appendSystem("Error al conectar.");
        }
    };

    const enviarMensaje = () => {
        const contenido = (inputTexto.value || "").trim();
        
        console.log("Intentando enviar:", {
            contenido: contenido.substring(0, 20),
            sesionId: sesionActualId,
            token: sesionActualToken ? sesionActualToken.substring(0, 10) : "NO_TOKEN",
            wsState: ws ? ws.readyState : "NO_WS",
            wsOpen: ws && ws.readyState === WebSocket.OPEN
        });
        
        if (!contenido) {
            appendSystem("Escribe un mensaje primero.");
            return;
        }
        
        if (!sesionActualId) {
            appendSystem("No hay sesión seleccionada.");
            return;
        }
        
        if (!sesionActualToken) {
            appendSystem("Token de sesión no disponible.");
            return;
        }
        
        if (!ws) {
            appendSystem("WebSocket no conectado. Reconectando...");
            conectarWebSocket();
            return;
        }
        
        if (ws.readyState !== WebSocket.OPEN) {
            appendSystem(`WebSocket no listo. Estado: ${ws.readyState} (1=ABIERTO)`);
            return;
        }

        const mensaje = { 
            accion: "mensaje", 
            sesionId: sesionActualId, 
            token: sesionActualToken, 
            contenido 
        };
        
        console.log("Enviando por WebSocket:", mensaje);
        ws.send(JSON.stringify(mensaje));
        inputTexto.value = "";
    };

    const inicializar = async () => {

        cargarSesionesLocal();
        await cargarSesionesDelServidor();

        if (Object.keys(sesiones).length === 0) {
            // Visitante se une a General o primer chat
            await unirseGeneralOPrimero();
        } else {
            const primerChat = Object.values(sesiones)[0];
            await cambiarChat(primerChat.id, primerChat.token);
        }

        conectarWebSocket();
        actualizarUI();
        
        // Reconectar WebSocket cada 5 segundos si se desconecta
        setInterval(() => {
            if (!ws || ws.readyState !== WebSocket.OPEN) {
                conectarWebSocket();
            }
        }, 5000);
    };

    // Event listeners
    if (btnNuevoChat) btnNuevoChat.addEventListener("click", crearNuevoChat);
    if (btnEnviar) btnEnviar.addEventListener("click", enviarMensaje);
    if (inputTexto) {
        inputTexto.addEventListener("keydown", (event) => {
            if (event.key === "Enter") {
                event.preventDefault();
                enviarMensaje();
            }
        });
    }

    inicializar();
})();

