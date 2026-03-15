package edu.pucmm.icc352.controllers;

import edu.pucmm.icc352.models.Role;
import edu.pucmm.icc352.services.EventService;
import edu.pucmm.icc352.services.UserService;
import io.javalin.apibuilder.ApiBuilder;
import io.javalin.http.Context;
import java.util.Map;

public class AdminController {

    private static final UserService  userSvc  = new UserService();
    private static final EventService eventSvc = new EventService();

    @SuppressWarnings("unchecked")
    public static void register() {

        // Ver todos los usuarios
        ApiBuilder.get("/api/admin/users", ctx -> {
            if (!isAdmin(ctx)) return;
            ctx.json(userSvc.findAll());
        });

        // Bloquear o desbloquear usuario
        ApiBuilder.patch("/api/admin/users/{id}/block", ctx -> {
            if (!isAdmin(ctx)) return;
            long targetId = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> b = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            boolean blocked = (Boolean) b.get("blocked");
            ctx.json(userSvc.setBlocked(targetId, blocked));
        });

        // Asignar o revocar rol
        ApiBuilder.patch("/api/admin/users/{id}/role", ctx -> {
            if (!isAdmin(ctx)) return;
            long targetId = Long.parseLong(ctx.pathParam("id"));
            Map<String, Object> b = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            Role newRole = Role.valueOf((String) b.get("role"));
            ctx.json(userSvc.setRole(targetId, newRole));
        });

        // Eliminar usuario
        ApiBuilder.delete("/api/admin/users/{id}", ctx -> {
            if (!isAdmin(ctx)) return;
            long targetId = Long.parseLong(ctx.pathParam("id"));
            userSvc.delete(targetId);
            ctx.json(Map.of("message", "Usuario eliminado."));
        });

        // Ver todos los eventos
        ApiBuilder.get("/api/admin/events", ctx -> {
            if (!isAdmin(ctx)) return;
            ctx.json(eventSvc.findAll());
        });

        // Eliminar evento inapropiado
        ApiBuilder.delete("/api/admin/events/{id}", ctx -> {
            if (!isAdmin(ctx)) return;
            long eventId = Long.parseLong(ctx.pathParam("id"));
            eventSvc.adminDelete(eventId);
            ctx.json(Map.of("message", "Evento eliminado."));
        });
    }

    private static boolean isAdmin(Context ctx) {
        String role = ctx.sessionAttribute("role");
        if (!"ADMIN".equals(role)) {
            ctx.status(403).json(Map.of("error", "Acceso restringido a administradores."));
            return false;
        }
        return true;
    }
}