package org.pucmm.eventos;

import org.pucmm.eventos.config.AppConfig;
import org.pucmm.eventos.config.DatabaseConfig;
import org.pucmm.eventos.config.DatabaseSeeder;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Punto de entrada de la aplicación.
 *
 * Responsabilidades:
 *  1. Inicializar la base de datos (SessionFactory + Seeder)
 *  2. Configurar Javalin (JSON, sesiones, CORS, rutas)
 *  3. Registrar un shutdown hook para cerrar la SessionFactory
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {

        // ── 1. Base de datos ──────────────────────────────────────────────────
        log.info("Inicializando base de datos...");
        DatabaseConfig.getSessionFactory();   // Crea tablas si no existen (hbm2ddl=update)
        DatabaseSeeder.seed();                // Crea el admin inicial si no existe

        // ── 2. Jackson con soporte para Java 8 Date/Time ──────────────────────
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule());

        // ── 3. Javalin ────────────────────────────────────────────────────────
        Javalin app = Javalin.create(config -> {

            // Serialización JSON
            config.jsonMapper(new JavalinJackson(objectMapper));

            // Archivos estáticos (HTML, CSS, JS)
            config.staticFiles.add("/public");

            // Sesiones
            config.jetty.modifyServletContextHandler(handler -> {
                var sessionHandler = new org.eclipse.jetty.server.session.SessionHandler();
                sessionHandler.setMaxInactiveInterval(60 * 60); // 1 hora
                handler.setSessionHandler(sessionHandler);
            });

            // Logging de requests en modo dev
            if (AppConfig.isDevMode()) {
                config.requestLogger.http((ctx, ms) ->
                        log.info("[{}] {} {} → {} ({}ms)",
                                ctx.method(), ctx.path(),
                                ctx.queryString() != null ? "?" + ctx.queryString() : "",
                                ctx.status(), ms));
            }

        });

        // ── 4. Rutas ──────────────────────────────────────────────────────────
        registerRoutes(app);

        // ── 5. Manejo global de errores ───────────────────────────────────────
        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST).json(errorResponse(e.getMessage()));
        });

        app.exception(IllegalStateException.class, (e, ctx) -> {
            ctx.status(HttpStatus.CONFLICT).json(errorResponse(e.getMessage()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Error no controlado: {}", e.getMessage(), e);
            String msg = AppConfig.isDevMode() ? e.getMessage() : "Error interno del servidor";
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(errorResponse(msg));
        });

        app.error(HttpStatus.NOT_FOUND, ctx -> {
            if (ctx.header("Accept") != null && ctx.header("Accept").contains("application/json")) {
                ctx.json(errorResponse("Recurso no encontrado"));
            } else {
                ctx.redirect("/");
            }
        });

        // ── 6. Arrancar el servidor ───────────────────────────────────────────
        int port = AppConfig.getPort();
        app.start(port);
        log.info("✅ Servidor iniciado en http://localhost:{}", port);

        // ── 7. Shutdown hook ──────────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Apagando servidor...");
            app.stop();
            DatabaseConfig.shutdown();
            log.info("Servidor detenido.");
        }));
    }

    /**
     * Registra todos los grupos de rutas de la aplicación.
     * Cada controller se añadirá aquí a medida que se implemente.
     */
    private static void registerRoutes(Javalin app) {

        // ── Health check ──────────────────────────────────────────────────────
        app.get("/api/health", ctx -> ctx.json(java.util.Map.of(
                "status", "UP",
                "version", "1.0.0",
                "timestamp", java.time.LocalDateTime.now().toString()
        )));

        // ── Auth ──────────────────────────────────────────────────────────────
        // app.post("/api/auth/login",    AuthController::login);
        // app.post("/api/auth/logout",   AuthController::logout);
        // app.post("/api/auth/register", AuthController::register);

        // ── Users (Admin) ─────────────────────────────────────────────────────
        // app.get   ("/api/users",               UserController::getAll);
        // app.patch ("/api/users/{id}/block",    UserController::block);
        // app.patch ("/api/users/{id}/role",     UserController::changeRole);

        // ── Events ────────────────────────────────────────────────────────────
        // app.get   ("/api/events",              EventController::getAll);
        // app.get   ("/api/events/{id}",         EventController::getById);
        // app.post  ("/api/events",              EventController::create);
        // app.put   ("/api/events/{id}",         EventController::update);
        // app.delete("/api/events/{id}",         EventController::delete);
        // app.patch ("/api/events/{id}/publish", EventController::togglePublish);
        // app.patch ("/api/events/{id}/cancel",  EventController::cancel);

        // ── Registrations ─────────────────────────────────────────────────────
        // app.post  ("/api/events/{id}/register",   RegistrationController::register);
        // app.delete("/api/registrations/{id}",     RegistrationController::cancel);
        // app.post  ("/api/registrations/validate", RegistrationController::validateQr);

        // ── Statistics ────────────────────────────────────────────────────────
        // app.get("/api/events/{id}/stats", StatsController::getEventStats);
    }

    /** Construye un body de error estándar */
    private static java.util.Map<String, String> errorResponse(String message) {
        return java.util.Map.of("error", message != null ? message : "Error desconocido");
    }
}