package edu.pucmm.icc352;

import edu.pucmm.icc352.config.HibernateConfig;
import edu.pucmm.icc352.controllers.*;
import edu.pucmm.icc352.utils.SeedData;
import io.javalin.Javalin;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.json.JavalinJackson;


public class Main {

    public static void main(String[] args) {

        // 1. Inicializar Hibernate
        HibernateConfig.init();

        // 2. Crear admin por defecto
        SeedData.createDefaultAdmin();



        // 3. Crear app Javalin 7
        // En Javalin 7: config.routes es un campo, no un metodo
        // Los exception handlers van encadenados despues del create(), antes del start()
        Javalin app = Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));            config.bundledPlugins.enableCors(cors ->
                    cors.addRule(it -> it.anyHost())
            );
            config.routes.apiBuilder(() -> {
                AuthController.register();
                EventController.register();
                RegistrationController.register();
                AdminController.register();
                StatsController.register();
            });

            config.routes.exception(IllegalArgumentException.class, (e, ctx) -> {
                ctx.status(400);
                ctx.json(java.util.Map.of("error", e.getMessage()));
            });
            config.routes.exception(IllegalStateException.class, (e, ctx) -> {
                ctx.status(409);
                ctx.json(java.util.Map.of("error", e.getMessage()));
            });
            config.routes.exception(SecurityException.class, (e, ctx) -> {
                ctx.status(403);
                ctx.json(java.util.Map.of("error", e.getMessage()));
            });
            config.routes.exception(Exception.class, (e, ctx) -> {
                ctx.status(500);
                ctx.json(java.util.Map.of("error", "Error interno: " + e.getMessage()));
            });
        });

        app.start(7070);

        System.out.println("Servidor corriendo en http://localhost:8080");
    }
}