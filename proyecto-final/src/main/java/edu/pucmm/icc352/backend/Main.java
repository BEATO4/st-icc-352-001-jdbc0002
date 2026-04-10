package edu.pucmm.icc352.backend;

import edu.pucmm.icc352.backend.controllers.AuthController;
import edu.pucmm.icc352.backend.controllers.SurveyFormController;
import edu.pucmm.icc352.backend.middleware.JwtAuthFilter;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PORT = 8080;

    public static void main(String[] args) {
        // ── Controllers ───────────────────────────────────────────────────────
        AuthController       authController   = new AuthController();
        SurveyFormController surveyController = new SurveyFormController();
        JwtAuthFilter        jwtFilter        = new JwtAuthFilter();

        // ── Javalin 7: everything declared upfront inside create() ────────────
        // Drop the `app` variable — nothing calls app.stop() here, so the
        // assignment would produce an "unused variable" warning.
        Javalin.create(config -> {

            config.jetty.port = PORT;

            // JavalinJackson has no single-arg ObjectMapper constructor in v7.
            // Use updateMapper() to configure the built-in Jackson instance.
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));

            // CORS for local development / mobile clients
            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(rule -> {
                        rule.anyHost();
                        rule.allowCredentials = false;
                    })
            );

            config.bundledPlugins.enableRouteOverview("/api/routes");

            // Serve frontend — explicit form avoids any ambiguity about directory vs hostedPath
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory  = "/public";
                staticFiles.location   = Location.CLASSPATH;
            });

            // ── Routes ────────────────────────────────────────────────────────
            // In Javalin 7 routes are registered directly on config.routes —
            // no wrapping lambda needed when using the config.routes.verb() style.

            // Public auth endpoints (no JWT required)
            config.routes.post("/api/auth/register", authController::register);
            config.routes.post("/api/auth/login",    authController::login);
            config.routes.get ("/api/auth/validate", authController::validateToken);

            // Protected auth endpoint
            config.routes.get("/api/auth/me", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                authController.getCurrentUser(ctx);
            });

            // Survey forms – specific paths must come before parameterised ones
            config.routes.get("/api/surveys/location", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.getWithLocation(ctx);
            });

            config.routes.get("/api/surveys/user/{userId}", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.getByUserId(ctx);
            });

            config.routes.post("/api/surveys", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.create(ctx);
            });

            config.routes.get("/api/surveys", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.getAll(ctx);
            });

            config.routes.get("/api/surveys/{id}", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.getById(ctx);
            });

            config.routes.put("/api/surveys/{id}", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.update(ctx);
            });

            config.routes.delete("/api/surveys/{id}", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                surveyController.delete(ctx);
            });

            // ── Exception and error handlers (must also be in config in v7) ──
            config.routes.exception(Exception.class, (e, ctx) -> {
                logger.error("Unhandled exception", e);
                ctx.status(500).json(Map.of(
                        "success", false,
                        "message", "Internal server error"
                ));
            });

            config.routes.error(404, ctx ->
                    ctx.json(Map.of("success", false, "message", "Endpoint not found"))
            );

            config.routes.error(405, ctx ->
                    ctx.json(Map.of("success", false, "message", "Method not allowed"))
            );

        }).start();

        logger.info("Server started on port {}", PORT);
        logger.info("Route overview available at http://localhost:{}/api/routes", PORT);
    }
}