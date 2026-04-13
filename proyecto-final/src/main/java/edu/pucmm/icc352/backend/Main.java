package edu.pucmm.icc352.backend;

import edu.pucmm.icc352.backend.controllers.AuthController;
import edu.pucmm.icc352.backend.controllers.SurveyFormController;
import edu.pucmm.icc352.backend.middleware.JwtAuthFilter;
import edu.pucmm.icc352.backend.grpc.EncuestaServiceImpl;
import edu.pucmm.icc352.backend.services.SurveyFormService;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final int PORT = 8080;
    private static final int GRPC_PORT = 9090;
    private static Server grpcServer;

    public static void main(String[] args) {
        SurveyFormService surveyFormService = new SurveyFormService();

        AuthController       authController   = new AuthController();
        SurveyFormController surveyController = new SurveyFormController(surveyFormService);
        JwtAuthFilter        jwtFilter        = new JwtAuthFilter();

        Javalin.create(config -> {

            config.jetty.port = PORT;

            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));

            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(rule -> {
                        rule.anyHost();
                        rule.allowCredentials = false;
                    })
            );

            config.bundledPlugins.enableRouteOverview("/api/routes");

            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/";
                staticFiles.directory  = "/public";
                staticFiles.location   = Location.CLASSPATH;
            });

            config.routes.post("/api/auth/register", authController::register);
            config.routes.post("/api/auth/login",    authController::login);
            config.routes.get ("/api/auth/validate", authController::validateToken);

            config.routes.get("/api/auth/me", ctx -> {
                jwtFilter.handle(ctx);
                if (ctx.status().getCode() == 401) return;
                authController.getCurrentUser(ctx);
            });

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

        // ── Start gRPC Server ─────────────────────────────────────────────────
        startGrpcServer(surveyFormService);

        // ── Shutdown Hook ─────────────────────────────────────────────────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            logger.info("Shutting down gRPC server...");
            if (grpcServer != null) {
                grpcServer.shutdown();
                try {
                    grpcServer.awaitTermination();
                } catch (InterruptedException e) {
                    logger.error("Error during gRPC shutdown", e);
                    Thread.currentThread().interrupt();
                }
            }
        }));
    }

    private static void startGrpcServer(SurveyFormService surveyFormService) {
        new Thread(() -> {
            try {
                grpcServer = ServerBuilder
                        .forPort(GRPC_PORT)
                        .addService(new EncuestaServiceImpl(surveyFormService))
                        .build()
                        .start();

                logger.info("gRPC Server started on port {}", GRPC_PORT);
                grpcServer.awaitTermination();
            } catch (Exception e) {
                logger.error("Error starting gRPC server", e);
            }
        }).start();
    }
}
