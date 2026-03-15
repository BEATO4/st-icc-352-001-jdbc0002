package edu.pucmm.icc352.controllers;

import edu.pucmm.icc352.models.Event;
import edu.pucmm.icc352.services.EventService;
import io.javalin.apibuilder.ApiBuilder;
import java.time.LocalDateTime;
import java.util.Map;

public class EventController {

    private static final EventService svc = new EventService();

    @SuppressWarnings("unchecked")
    public static void register() {

        // Lista publica de eventos publicados
        ApiBuilder.get("/api/events", ctx -> {
            ctx.json(svc.findPublished());
        });

        // Eventos del organizador autenticado
        // IMPORTANTE: esta ruta debe ir ANTES de /api/events/{id}
        ApiBuilder.get("/api/events/mine", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            ctx.json(svc.findByOrganizer(userId));
        });

        // Detalle de un evento por ID
        ApiBuilder.get("/api/events/{id}", ctx -> {
            long id = Long.parseLong(ctx.pathParam("id"));
            ctx.json(svc.findById(id));
        });

        // Crear evento - solo ORGANIZER o ADMIN
        ApiBuilder.post("/api/events", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            String role = ctx.sessionAttribute("role");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            if ("PARTICIPANT".equals(role)) {
                ctx.status(403).json(Map.of("error", "Sin permiso."));
                return;
            }
            Map<String, Object> b = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            Event event = svc.create(
                    (String) b.get("title"),
                    (String) b.get("description"),
                    LocalDateTime.parse((String) b.get("dateTime")),
                    (String) b.get("location"),
                    ((Number) b.get("maxCapacity")).intValue(),
                    userId
            );
            ctx.status(201).json(event);
        });

        // Editar evento
        ApiBuilder.put("/api/events/{id}", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            String role = ctx.sessionAttribute("role");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            if ("PARTICIPANT".equals(role)) {
                ctx.status(403).json(Map.of("error", "Sin permiso."));
                return;
            }
            long eventId = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> b = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            ctx.json(svc.update(
                    eventId, userId,
                    (String) b.get("title"),
                    (String) b.get("description"),
                    LocalDateTime.parse((String) b.get("dateTime")),
                    (String) b.get("location"),
                    ((Number) b.get("maxCapacity")).intValue()
            ));
        });

        // Publicar o des-publicar
        ApiBuilder.patch("/api/events/{id}/publish", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            String role = ctx.sessionAttribute("role");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            if ("PARTICIPANT".equals(role)) {
                ctx.status(403).json(Map.of("error", "Sin permiso."));
                return;
            }
            long eventId = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> b = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            boolean publish = (Boolean) b.get("publish");
            ctx.json(svc.setPublished(eventId, userId, publish));
        });

        // Cancelar evento
        ApiBuilder.patch("/api/events/{id}/cancel", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            String role = ctx.sessionAttribute("role");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            if ("PARTICIPANT".equals(role)) {
                ctx.status(403).json(Map.of("error", "Sin permiso."));
                return;
            }
            long eventId = Long.parseLong(ctx.pathParam("id"));
            ctx.json(svc.cancel(eventId, userId));
        });
    }
}