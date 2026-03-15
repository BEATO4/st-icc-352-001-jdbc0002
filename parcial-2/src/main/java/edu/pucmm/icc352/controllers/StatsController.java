package edu.pucmm.icc352.controllers;

import edu.pucmm.icc352.services.StatsService;
import io.javalin.apibuilder.ApiBuilder;
import java.util.Map;

public class StatsController {

    private static final StatsService svc = new StatsService();

    public static void register() {

        // Resumen completo de un evento - solo ORGANIZER o ADMIN
        ApiBuilder.get("/api/stats/{eventId}", ctx -> {
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
            long eventId = Long.parseLong(ctx.pathParam("eventId"));
            ctx.json(svc.getEventStats(eventId));
        });
    }
}