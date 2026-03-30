package org.pucmm.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.http.UploadedFile;
import io.javalin.http.staticfiles.Location;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.websocket.WsMessageContext;
import io.javalin.websocket.WsContext;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;
import org.h2.tools.Server;
import org.jasypt.util.text.AES256TextEncryptor;
import org.pucmm.blog.encapsulaciones.*;
import org.pucmm.blog.servicios.*;

import java.sql.SQLException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    public static EntityManagerFactory emf;

    private static final int ARTICULOS_POR_PAGINA = 5;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final DateTimeFormatter FECHA_FORMATO = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final Set<WsContext> STAFF_SOCKETS = ConcurrentHashMap.newKeySet();
    private static final Map<Long, Set<WsContext>> VISITOR_SOCKETS = new ConcurrentHashMap<>();
    private static final Map<WsContext, Long> VISITOR_SESION_POR_SOCKET = new ConcurrentHashMap<>();
    private static final Map<WsContext, Long> STAFF_SESION_ACTIVA = new ConcurrentHashMap<>();

    public static void main(String[] args) throws SQLException {
        AES256TextEncryptor encriptador = new AES256TextEncryptor();
        encriptador.setPassword("ClaveSecreta_Blog_ICC352");

        Map<String, String> jpaOverrides = construirOverridesJpa();
        if ("org.h2.Driver".equals(jpaOverrides.get("jakarta.persistence.jdbc.driver"))) {
            Server.createTcpServer("-tcp", "-tcpAllowOthers", "-ifNotExists", "-tcpPort", "9092").start();
            System.out.println("Servidor H2 iniciado en modo TCP.");
        }

        emf = Persistence.createEntityManagerFactory("BlogPU", new HashMap<>(jpaOverrides));
        System.out.println("Conexion a BD establecida.");

        AuditoriaServices.crearTablaAuditoria();
        crearDatosIniciales();

        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public", Location.CLASSPATH);
            config.fileRenderer(new JavalinThymeleaf());
        }).start(7000);

        app.before(ctx -> {
            Usuario usuarioSesion = ctx.sessionAttribute("usuario");
            if (usuarioSesion != null) {
                return;
            }
            String cookieRecordar = ctx.cookie("usuario_recordado");
            if (cookieRecordar == null) {
                return;
            }
            try {
                String usernameDescifrado = encriptador.decrypt(cookieRecordar);
                Usuario usuarioBD = UsuarioServices.getInstancia().buscar(usernameDescifrado);
                if (usuarioBD != null) {
                    ctx.sessionAttribute("usuario", usuarioBD);
                }
            } catch (Exception e) {
                ctx.removeCookie("usuario_recordado");
            }
        });

        app.before("/admin/*", ctx -> validarStaff(ctx.sessionAttribute("usuario"), true, ctx));
        app.before("/api/admin/*", ctx -> validarStaff(ctx.sessionAttribute("usuario"), false, ctx));

        app.before("/crear-articulo", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (!esStaff(usuario)) {
                ctx.redirect("/login");
            }
        });

        app.get("/", ctx -> {
            Map<String, Object> modelo = new HashMap<>();
            String etiqueta = ctx.queryParam("etiqueta");
            int paginaActual = parseEnteroPositivo(ctx.queryParam("pagina"), 1);

            List<Articulo> articulos;
            long totalArticulos = 0;
            int totalPaginas = 1;

            if (etiqueta != null && !etiqueta.isBlank()) {
                articulos = ArticuloServices.getInstancia().buscarPorEtiqueta(etiqueta);
                modelo.put("etiquetaFiltro", etiqueta);
            } else {
                articulos = ArticuloServices.getInstancia().buscarArticulosPaginados(paginaActual, ARTICULOS_POR_PAGINA);
                totalArticulos = ArticuloServices.getInstancia().contarArticulos();
                totalPaginas = (int) Math.ceil((double) totalArticulos / ARTICULOS_POR_PAGINA);
            }

            modelo.put("articulos", articulos);
            modelo.put("usuario", ctx.sessionAttribute("usuario"));
            modelo.put("paginaActual", paginaActual);
            modelo.put("totalPaginas", Math.max(1, totalPaginas));

            ctx.render("/templates/index.html", modelo);
        });

        app.get("/api/articulos", ctx -> {
            int paginaActual = parseEnteroPositivo(ctx.queryParam("pagina"), 1);
            List<Articulo> articulos = ArticuloServices.getInstancia().buscarArticulosPaginados(paginaActual, ARTICULOS_POR_PAGINA);
            long totalArticulos = ArticuloServices.getInstancia().contarArticulos();
            int totalPaginas = (int) Math.ceil((double) totalArticulos / ARTICULOS_POR_PAGINA);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("paginaActual", paginaActual);
            respuesta.put("totalPaginas", Math.max(1, totalPaginas));
            respuesta.put("articulos", articulos.stream().map(Main::serializarArticuloResumen).toList());
            ctx.json(respuesta);
        });

        app.get("/login", ctx -> {
            Map<String, Object> modelo = new HashMap<>();
            if (ctx.queryParam("error") != null) {
                modelo.put("error", true);
            }
            ctx.render("/templates/login.html", modelo);
        });

        app.post("/login", ctx -> {
            String username = ctx.formParam("username");
            String pass = ctx.formParam("password");
            String recordar = ctx.formParam("recordar");
            Usuario authUser = UsuarioServices.getInstancia().autenticarUsuario(username, pass);

            if (authUser == null) {
                ctx.redirect("/login?error");
                return;
            }

            ctx.sessionAttribute("usuario", authUser);
            AuditoriaServices.registrarLogin(authUser.getUsername());
            if ("on".equals(recordar)) {
                ctx.cookie("usuario_recordado", encriptador.encrypt(authUser.getUsername()), 604800);
            }
            ctx.redirect("/");
        });

        app.get("/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.removeCookie("usuario_recordado");
            ctx.redirect("/");
        });

        app.get("/admin/crear-usuario", ctx -> ctx.render("/templates/crear_usuario.html", new HashMap<>()));

        app.post("/crear-usuario", ctx -> {
            String username = ctx.formParam("username");
            String nombre = ctx.formParam("nombre");
            String pass = ctx.formParam("password");
            boolean esAutor = ctx.formParam("autor") != null;
            boolean esAdmin = ctx.formParam("admin") != null;

            Usuario nuevoUsuario = new Usuario(username, nombre, pass, esAdmin, esAutor);
            UploadedFile foto = ctx.uploadedFile("fotoPerfil");
            if (foto != null && foto.size() > 0) {
                byte[] bytes = foto.content().readAllBytes();
                String base64Image = Base64.getEncoder().encodeToString(bytes);
                String mimeType = foto.contentType();
                nuevoUsuario.setFotoPerfilBase64("data:" + mimeType + ";base64," + base64Image);
            }

            UsuarioServices.getInstancia().crear(nuevoUsuario);
            ctx.redirect("/");
        });

        app.get("/crear-articulo", ctx -> ctx.render("/templates/crear_articulo.html"));

        app.post("/crear-articulo", ctx -> {
            String titulo = ctx.formParam("titulo");
            String cuerpo = ctx.formParam("cuerpo");
            String etiquetasParam = ctx.formParam("etiquetas");
            Usuario autor = ctx.sessionAttribute("usuario");

            Articulo nuevoArticulo = new Articulo(titulo, cuerpo, autor);
            if (etiquetasParam != null && !etiquetasParam.trim().isEmpty()) {
                String[] tags = etiquetasParam.split(",");
                for (String tag : tags) {
                    nuevoArticulo.getListaEtiquetas().add(new Etiqueta(tag.trim()));
                }
            }

            ArticuloServices.getInstancia().crear(nuevoArticulo);
            ctx.redirect("/");
        });

        app.get("/articulo/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            Articulo articulo = ArticuloServices.getInstancia().buscar(id);
            if (articulo == null) {
                ctx.status(404).result("Articulo no encontrado");
                return;
            }
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("articulo", articulo);
            modelo.put("usuario", ctx.sessionAttribute("usuario"));
            ctx.render("/templates/articulo.html", modelo);
        });

        app.post("/articulo/{id}/comentario", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            String textoComentario = ctx.formParam("comentario");
            Usuario autor = ctx.sessionAttribute("usuario");
            Articulo articulo = ArticuloServices.getInstancia().buscar(id);

            if (articulo != null && autor != null && textoComentario != null && !textoComentario.trim().isEmpty()) {
                articulo.getListaComentarios().add(new Comentario(textoComentario, autor));
                ArticuloServices.getInstancia().editar(articulo);
            }
            ctx.redirect("/articulo/" + id);
        });

        app.post("/api/chat/sesiones", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            JsonNode payload = MAPPER.readTree(ctx.body());
            String nombreChat = texto(payload, "nombreChat", "").trim();
            String nombreInvitado = texto(payload, "nombreInvitado", "Invitado").trim();
            String pagina = texto(payload, "pagina", "inicio").trim();

            // Si nombreChat está vacío, quiere decir que es un invitado uniéndose a un chat general
            if (nombreChat.isBlank()) {
                if (nombreInvitado.isBlank()) {
                    ctx.status(400).json(Map.of("error", "Debes indicar tu nombre"));
                    return;
                }
                // Crear o unirse al chat "General" de esa página
                ChatSesion general = ChatSesionServices.getInstancia().buscarPorNombreYPagina("General", pagina);
                if (general == null) {
                    general = new ChatSesion(UUID.randomUUID().toString(), "General", nombreInvitado, pagina);
                    ChatSesionServices.getInstancia().crear(general);
                } else {
                    general.setNombreInvitado(nombreInvitado);
                    ChatSesionServices.getInstancia().editar(general);
                }
                ctx.json(Map.of(
                    "id", general.getId(),
                    "token", general.getToken(),
                    "nombreChat", general.getNombreChat(),
                    "nombreInvitado", general.getNombreInvitado()
                ));
                return;
            }

            // Crear nuevo chat (solo admin/autor)
            if (!esStaff(usuario)) {
                ctx.status(403).json(Map.of("error", "Solo admin/autor pueden crear chats"));
                return;
            }

            if (nombreChat.isBlank()) {
                ctx.status(400).json(Map.of("error", "El nombre del chat es obligatorio"));
                return;
            }

            ChatSesion nuevaSesion = new ChatSesion(UUID.randomUUID().toString(), nombreChat, nombreInvitado, pagina);
            nuevaSesion.setCreadoPor(usuario);
            ChatSesionServices.getInstancia().crear(nuevaSesion);

            Map<String, Object> respuesta = new HashMap<>();
            respuesta.put("id", nuevaSesion.getId());
            respuesta.put("token", nuevaSesion.getToken());
            respuesta.put("nombreChat", nuevaSesion.getNombreChat());
            respuesta.put("nombreInvitado", nuevaSesion.getNombreInvitado());
            ctx.json(respuesta);
            notificarResumenStaff(nuevaSesion);
        });

        app.get("/api/chat/sesiones/{id}/mensajes", ctx -> {
            long sesionId = Long.parseLong(ctx.pathParam("id"));
            String token = Optional.ofNullable(ctx.queryParam("token")).orElse("");

            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null || !sesion.getToken().equals(token)) {
                ctx.status(403).json(Map.of("error", "Sesion invalida"));
                return;
            }

            ctx.json(Map.of("mensajes", ChatMensajeServices.getInstancia().listarPorSesion(sesionId).stream().map(Main::serializarMensaje).toList()));
        });

        app.post("/api/chat/sesiones/{id}/validar", ctx -> {
            long sesionId = Long.parseLong(ctx.pathParam("id"));
            String token = Optional.ofNullable(ctx.queryParam("token")).orElse("");

            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null) {
                ctx.status(404).json(Map.of("error", "Sesion no encontrada"));
                return;
            }
            
            if (!sesion.getToken().equals(token)) {
                ctx.status(403).json(Map.of("error", "Token invalido"));
                return;
            }
            
            ctx.json(Map.of("abierta", sesion.isAbierta(), "atendidoPor", sesion.getAtendidoPor() != null));
        });

        app.get("/api/chat/sesiones-pagina", ctx -> {
            String pagina = Optional.ofNullable(ctx.queryParam("pagina")).orElse("inicio");
            List<Map<String, Object>> sesiones = ChatSesionServices.getInstancia().listarPorPagina(pagina).stream()
                    .map(Main::serializarSesionCompleta)
                    .toList();
            ctx.json(Map.of("sesiones", sesiones));
        });

        app.post("/api/chat/sesiones/{id}/cerrar", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (!esStaff(usuario)) {
                ctx.status(403).json(Map.of("error", "Solo admin/autor pueden cerrar chats"));
                return;
            }

            long sesionId = Long.parseLong(ctx.pathParam("id"));
            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null) {
                ctx.status(404).json(Map.of("error", "Chat no encontrado"));
                return;
            }

            sesion.setAbierta(false);
            ChatSesionServices.getInstancia().editar(sesion);
            
            // Notificar a los visitantes que el chat cerró
            enviarATodosEnSesion(sesionId, Map.of("tipo", "sesion-cerrada", "sesionId", sesionId, "mensaje", "La conversación ha finalizado. Gracias."));
            
            ctx.json(Map.of("cerrada", true, "id", sesionId));
        });

        app.post("/api/chat/sesiones/{id}/reabrir", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (!esStaff(usuario)) {
                ctx.status(403).json(Map.of("error", "Solo admin/autor pueden reabrir chats"));
                return;
            }

            long sesionId = Long.parseLong(ctx.pathParam("id"));
            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null) {
                ctx.status(404).json(Map.of("error", "Chat no encontrado"));
                return;
            }

            sesion.setAbierta(true);
            sesion.setEsperandoAgente(true);
            ChatSesionServices.getInstancia().editar(sesion);
            ctx.json(Map.of("reabierta", true, "id", sesionId));
        });

        app.delete("/api/chat/sesiones/{id}", ctx -> {
            Usuario usuario = ctx.sessionAttribute("usuario");
            if (!esAdmin(usuario)) {
                ctx.status(403).json(Map.of("error", "Solo admin puede eliminar chats"));
                return;
            }

            long sesionId = Long.parseLong(ctx.pathParam("id"));
            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null) {
                ctx.status(404).json(Map.of("error", "Chat no encontrado"));
                return;
            }

            if (sesion.isAbierta()) {
                ctx.status(400).json(Map.of("error", "No se puede eliminar un chat abierto. Ciérralo primero."));
                return;
            }

            ChatMensajeServices.getInstancia().listarPorSesion(sesionId).forEach(ChatMensajeServices.getInstancia()::eliminar);
            ChatSesionServices.getInstancia().eliminar(sesion);
            ctx.json(Map.of("eliminada", true, "id", sesionId));
        });

        app.get("/admin/chats", ctx -> {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuario", ctx.sessionAttribute("usuario"));
            ctx.render("/templates/admin_chats.html", modelo);
        });

        app.get("/api/admin/chats/sesiones", ctx -> {
            List<Map<String, Object>> sesiones = ChatSesionServices.getInstancia().listarRecientes().stream()
                    .map(Main::serializarSesionResumen)
                    .toList();
            ctx.json(Map.of("sesiones", sesiones));
        });

        app.get("/api/admin/chats/sesiones/{id}/mensajes", ctx -> {
            long sesionId = Long.parseLong(ctx.pathParam("id"));
            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null) {
                ctx.status(404).json(Map.of("error", "Sesion no encontrada"));
                return;
            }
            ctx.json(Map.of("mensajes", ChatMensajeServices.getInstancia().listarPorSesion(sesionId).stream().map(Main::serializarMensaje).toList()));
        });

        app.post("/api/admin/chats/sesiones/{id}/cerrar", ctx -> {
            long sesionId = Long.parseLong(ctx.pathParam("id"));
            ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
            if (sesion == null) {
                ctx.status(404).json(Map.of("error", "Sesion no encontrada"));
                return;
            }
            sesion.setAbierta(false);
            ChatSesionServices.getInstancia().editar(sesion);
            Map<String, Object> evento = Map.of("tipo", "sesion-cerrada", "sesionId", sesionId);
            enviarATodosStaff(evento);
            enviarAVisitantes(sesionId, evento);
            ctx.json(Map.of("ok", true));
        });

        app.ws("/ws/chat", ws -> {
            ws.onClose(ctx -> limpiarSocket(ctx));
            ws.onError(ctx -> limpiarSocket(ctx));
            ws.onMessage(ctx -> procesarWsMensaje(ctx));
        });
    }

    private static void procesarWsMensaje(WsMessageContext ctx) {
        try {
            JsonNode payload = MAPPER.readTree(ctx.message());
            String accion = texto(payload, "accion", "");
            switch (accion) {
                case "suscribirStaff" -> suscribirStaff(ctx);
                case "suscribirVisitante" -> suscribirVisitante(ctx, payload);
                case "unirseSesion" -> unirseSesionStaff(ctx, payload);
                case "mensaje" -> manejarMensaje(ctx, payload);
                default -> enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Accion no soportada"));
            }
        } catch (Exception e) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Formato de mensaje invalido"));
        }
    }

    private static void suscribirStaff(WsContext ctx) {
        Usuario usuario = ctx.sessionAttribute("usuario");
        if (!esStaff(usuario)) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "No autorizado"));
            return;
        }
        STAFF_SOCKETS.add(ctx);
        enviarWs(ctx, Map.of("tipo", "staff-suscrito"));
    }

    private static void suscribirVisitante(WsContext ctx, JsonNode payload) {
        long sesionId = payload.path("sesionId").asLong(-1);
        String token = texto(payload, "token", "");
        
        if (sesionId < 1 || token.isBlank()) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Sesion invalida"));
            return;
        }
        
        ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
        if (sesion == null) {
            System.err.println("Sesion no encontrada: ID=" + sesionId);
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Sesion cerrada o inexistente"));
            return;
        }
        
        if (!sesion.getToken().equals(token)) {
            System.err.println("Token invalido para sesion: ID=" + sesionId + ", esperado=" + sesion.getToken() + ", recibido=" + token);
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Sesion invalida"));
            return;
        }

        VISITOR_SOCKETS.computeIfAbsent(sesionId, key -> ConcurrentHashMap.newKeySet()).add(ctx);
        VISITOR_SESION_POR_SOCKET.put(ctx, sesionId);
        System.out.println("Visitante suscrito a sesion: " + sesionId);
        enviarWs(ctx, Map.of("tipo", "visitante-suscrito", "sesionId", sesionId));
    }

    private static void unirseSesionStaff(WsContext ctx, JsonNode payload) {
        Usuario usuario = ctx.sessionAttribute("usuario");
        if (!esStaff(usuario)) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "No autorizado"));
            return;
        }

        long sesionId = payload.path("sesionId").asLong(-1);
        ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
        if (sesion == null) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Sesion no encontrada"));
            return;
        }

        STAFF_SOCKETS.add(ctx);
        STAFF_SESION_ACTIVA.put(ctx, sesionId);
        if (sesion.getAtendidoPor() == null) {
            sesion.setAtendidoPor(usuario);
            ChatSesionServices.getInstancia().editar(sesion);
            notificarResumenStaff(sesion);
        }
        enviarWs(ctx, Map.of("tipo", "sesion-unida", "sesionId", sesionId));
    }

    private static void manejarMensaje(WsContext ctx, JsonNode payload) {
        long sesionId = payload.path("sesionId").asLong(-1);
        String contenido = texto(payload, "contenido", "").trim();
        if (sesionId < 1 || contenido.isBlank()) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Mensaje invalido"));
            return;
        }

        ChatSesion sesion = ChatSesionServices.getInstancia().buscar(sesionId);
        if (sesion == null || !sesion.isAbierta()) {
            enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Sesion cerrada o inexistente"));
            return;
        }

        Usuario usuario = ctx.sessionAttribute("usuario");
        String emisorTipo;
        String emisorNombre;

        if (esStaff(usuario)) {
            emisorTipo = "STAFF";
            emisorNombre = usuario.getNombre();
            if (sesion.getAtendidoPor() == null) {
                sesion.setAtendidoPor(usuario);
            }
        } else {
            String token = texto(payload, "token", "");
            if (!sesion.getToken().equals(token)) {
                enviarWs(ctx, Map.of("tipo", "error", "mensaje", "Token invalido"));
                return;
            }
            emisorTipo = "VISITANTE";
            emisorNombre = sesion.getNombreInvitado();
        }

        ChatMensaje mensaje = new ChatMensaje(sesion, emisorNombre, emisorTipo, contenido);
        ChatMensajeServices.getInstancia().crear(mensaje);
        sesion.setFechaUltimoMensaje(new Date());
        ChatSesionServices.getInstancia().editar(sesion);

        Map<String, Object> evento = new HashMap<>();
        evento.put("tipo", "mensaje");
        evento.put("sesionId", sesionId);
        evento.put("mensaje", serializarMensaje(mensaje));

        enviarAVisitantes(sesionId, evento);
        enviarATodosStaff(evento);
        notificarResumenStaff(sesion);
    }

    private static void limpiarSocket(WsContext ctx) {
        STAFF_SOCKETS.remove(ctx);
        STAFF_SESION_ACTIVA.remove(ctx);
        Long sesionId = VISITOR_SESION_POR_SOCKET.remove(ctx);
        if (sesionId != null) {
            Set<WsContext> visitantes = VISITOR_SOCKETS.get(sesionId);
            if (visitantes != null) {
                visitantes.remove(ctx);
                if (visitantes.isEmpty()) {
                    VISITOR_SOCKETS.remove(sesionId);
                }
            }
        }
    }

    private static void notificarResumenStaff(ChatSesion sesion) {
        Map<String, Object> evento = Map.of("tipo", "sesion-actualizada", "sesion", serializarSesionResumen(sesion));
        enviarATodosStaff(evento);
    }

    private static void enviarATodosStaff(Object payload) {
        STAFF_SOCKETS.removeIf(ctx -> !enviarWs(ctx, payload));
    }

    private static void enviarAVisitantes(long sesionId, Object payload) {
        Set<WsContext> sockets = VISITOR_SOCKETS.get(sesionId);
        if (sockets == null) {
            return;
        }
        sockets.removeIf(ctx -> !enviarWs(ctx, payload));
    }

    private static void enviarATodosEnSesion(long sesionId, Object payload) {
        enviarATodosStaff(payload);
        enviarAVisitantes(sesionId, payload);
    }

    private static boolean enviarWs(WsContext ctx, Object payload) {
        try {
            ctx.send(MAPPER.writeValueAsString(payload));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static Map<String, String> construirOverridesJpa() {
        Map<String, String> overrides = new HashMap<>();
        String dbVendor = Optional.ofNullable(System.getenv("DB_VENDOR")).orElse("h2").toLowerCase(Locale.ROOT);

        if ("postgres".equals(dbVendor)) {
            overrides.put("jakarta.persistence.jdbc.driver", "org.postgresql.Driver");
            overrides.put("jakarta.persistence.jdbc.url", Optional.ofNullable(System.getenv("DB_URL")).orElse("jdbc:postgresql://db:5432/blogdb"));
            overrides.put("jakarta.persistence.jdbc.user", Optional.ofNullable(System.getenv("DB_USER")).orElse("blog"));
            overrides.put("jakarta.persistence.jdbc.password", Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse("blog"));
            overrides.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        } else {
            overrides.put("jakarta.persistence.jdbc.driver", "org.h2.Driver");
            overrides.put("jakarta.persistence.jdbc.url", Optional.ofNullable(System.getenv("DB_URL")).orElse("jdbc:h2:tcp://localhost/~/blogDB"));
            overrides.put("jakarta.persistence.jdbc.user", Optional.ofNullable(System.getenv("DB_USER")).orElse("sa"));
            overrides.put("jakarta.persistence.jdbc.password", Optional.ofNullable(System.getenv("DB_PASSWORD")).orElse(""));
            overrides.put("hibernate.dialect", "org.hibernate.dialect.H2Dialect");
        }

        overrides.put("hibernate.hbm2ddl.auto", Optional.ofNullable(System.getenv("HIBERNATE_DDL_AUTO")).orElse("update"));
        overrides.put("hibernate.show_sql", Optional.ofNullable(System.getenv("HIBERNATE_SHOW_SQL")).orElse("false"));
        overrides.put("hibernate.format_sql", "true");
        return overrides;
    }

    private static void crearDatosIniciales() {
        if (UsuarioServices.getInstancia().buscar("admin") != null) {
            return;
        }

        Usuario admin = new Usuario("admin", "Administrador", "admin", true, true);
        UsuarioServices.getInstancia().crear(admin);

        Usuario autor = new Usuario("autor", "Autor Demo", "1234", false, true);
        UsuarioServices.getInstancia().crear(autor);

        Articulo a1 = new Articulo("Primer Post", "Este es el cuerpo del articulo de ejemplo.", admin);
        a1.getListaEtiquetas().add(new Etiqueta("Tecnologia"));
        ArticuloServices.getInstancia().crear(a1);
    }

    private static Map<String, Object> serializarArticuloResumen(Articulo art) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", art.getId());
        data.put("titulo", art.getTitulo());
        data.put("resumen", art.getResumen());
        data.put("fecha", formatearFecha(art.getFecha()));
        data.put("autor", art.getAutor() != null ? art.getAutor().getUsername() : "anonimo");
        data.put("etiquetas", art.getListaEtiquetas().stream().map(Etiqueta::getEtiqueta).toList());
        return data;
    }

    private static Map<String, Object> serializarSesionResumen(ChatSesion sesion) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", sesion.getId());
        data.put("nombreChat", sesion.getNombreChat());
        data.put("nombreInvitado", sesion.getNombreInvitado());
        data.put("paginaOrigen", sesion.getPaginaOrigen());
        data.put("abierta", sesion.isAbierta());
        data.put("fechaUltimoMensaje", formatearFecha(sesion.getFechaUltimoMensaje()));
        data.put("atendidoPor", sesion.getAtendidoPor() != null ? sesion.getAtendidoPor().getNombre() : null);
        return data;
    }

    private static Map<String, Object> serializarSesionCompleta(ChatSesion sesion) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", sesion.getId());
        data.put("token", sesion.getToken());
        data.put("nombreChat", sesion.getNombreChat());
        data.put("nombreInvitado", sesion.getNombreInvitado());
        data.put("paginaOrigen", sesion.getPaginaOrigen());
        data.put("abierta", sesion.isAbierta());
        data.put("esperandoAgente", sesion.isEsperandoAgente());
        data.put("fechaCreacion", formatearFecha(sesion.getFechaCreacion()));
        data.put("fechaUltimoMensaje", formatearFecha(sesion.getFechaUltimoMensaje()));
        data.put("creadoPor", sesion.getCreadoPor() != null ? sesion.getCreadoPor().getNombre() : null);
        data.put("atendidoPor", sesion.getAtendidoPor() != null ? sesion.getAtendidoPor().getNombre() : null);
        return data;
    }

    private static Map<String, Object> serializarMensaje(ChatMensaje mensaje) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", mensaje.getId());
        data.put("emisorNombre", mensaje.getEmisorNombre());
        data.put("emisorTipo", mensaje.getEmisorTipo());
        data.put("contenido", mensaje.getContenido());
        data.put("fecha", formatearFecha(mensaje.getFecha()));
        return data;
    }

    private static void validarStaff(Usuario usuario, boolean redirect, io.javalin.http.Context ctx) {
        if (esStaff(usuario)) {
            return;
        }
        if (redirect) {
            ctx.redirect("/login");
        } else {
            ctx.status(403).json(Map.of("error", "No autorizado"));
        }
    }

    private static boolean esStaff(Usuario usuario) {
        return usuario != null && (usuario.isAdministrator() || usuario.isAutor());
    }

    private static boolean esAdmin(Usuario usuario) {
        return usuario != null && usuario.isAdministrator();
    }

    private static int parseEnteroPositivo(String valor, int porDefecto) {
        try {
            int numero = Integer.parseInt(valor);
            return Math.max(1, numero);
        } catch (Exception e) {
            return porDefecto;
        }
    }

    private static String texto(JsonNode nodo, String campo, String porDefecto) {
        JsonNode valor = nodo.path(campo);
        return valor.isMissingNode() || valor.isNull() ? porDefecto : valor.asText(porDefecto);
    }

    private static String formatearFecha(Date fecha) {
        if (fecha == null) {
            return "";
        }
        return FECHA_FORMATO.format(fecha.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
    }
}