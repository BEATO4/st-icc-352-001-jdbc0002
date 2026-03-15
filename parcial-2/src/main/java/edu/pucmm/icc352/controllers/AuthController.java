package edu.pucmm.icc352.controllers;

import edu.pucmm.icc352.models.User;
import edu.pucmm.icc352.services.UserService;
import io.javalin.apibuilder.ApiBuilder;
import java.util.Map;

public class AuthController {

    private static final UserService svc = new UserService();

    @SuppressWarnings("unchecked")
    public static void register() {

        ApiBuilder.post("/api/auth/register", ctx -> {
            Map<String, Object> body = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            User user = svc.register(
                    (String) body.get("email"),
                    (String) body.get("password"),
                    (String) body.get("name")
            );
            ctx.status(201).json(Map.of(
                    "id",   user.getId(),
                    "name", user.getName(),
                    "role", user.getRole()
            ));
        });

        ApiBuilder.post("/api/auth/login", ctx -> {
            Map<String, Object> body = (Map<String, Object>) ctx.bodyAsClass(Map.class);
            User user = svc.login(
                    (String) body.get("email"),
                    (String) body.get("password")
            );
            ctx.sessionAttribute("userId", user.getId());
            ctx.sessionAttribute("role",   user.getRole().name());
            ctx.json(Map.of(
                    "id",    user.getId(),
                    "name",  user.getName(),
                    "role",  user.getRole(),
                    "email", user.getEmail()
            ));
        });

        ApiBuilder.post("/api/auth/logout", ctx -> {
            ctx.req().getSession().invalidate();
            ctx.status(200).json(Map.of("message", "Sesion cerrada."));
        });

        ApiBuilder.get("/api/auth/me", ctx -> {
            Long userId = ctx.sessionAttribute("userId");
            if (userId == null) {
                ctx.status(401).json(Map.of("error", "No autenticado."));
                return;
            }
            User u = svc.findById(userId).orElse(null);
            if (u == null) {
                ctx.status(404).json(Map.of("error", "Usuario no encontrado."));
                return;
            }
            ctx.json(Map.of(
                    "id",    u.getId(),
                    "name",  u.getName(),
                    "role",  u.getRole(),
                    "email", u.getEmail()
            ));
        });
    }
}