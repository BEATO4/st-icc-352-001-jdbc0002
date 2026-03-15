package org.pucmm.eventos.controller;

import io.javalin.http.Context;
import org.pucmm.eventos.model.User;
import org.pucmm.eventos.service.UserService;
import org.pucmm.eventos.util.SessionUtil;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) { this.userService = userService; }

    // POST /api/auth/login
    public void login(Context ctx) {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String username = str(body, "username");
        String password = str(body, "password");

        if (username == null || password == null) {
            ctx.status(400).json(err("username and password are required")); return;
        }

        Optional<User> opt = userService.authenticate(username, password);
        if (opt.isEmpty()) {
            ctx.status(401).json(err("Invalid credentials or account is blocked")); return;
        }

        SessionUtil.setUser(ctx, opt.get());
        ctx.json(toMap(opt.get()));
    }

    // POST /api/auth/logout
    public void logout(Context ctx) {
        SessionUtil.clear(ctx);
        ctx.json(Map.of("message", "Logged out"));
    }

    // POST /api/auth/register
    public void register(Context ctx) {
        Map<?, ?> body = ctx.bodyAsClass(Map.class);
        String username = str(body, "username");
        String email    = str(body, "email");
        String password = str(body, "password");

        if (username == null || email == null || password == null) {
            ctx.status(400).json(err("All fields are required")); return;
        }

        try {
            User user = userService.register(username, email, password);
            SessionUtil.setUser(ctx, user);
            ctx.status(201).json(toMap(user));
        } catch (IllegalArgumentException e) {
            ctx.status(409).json(err(e.getMessage()));
        }
    }

    // GET /api/auth/me
    public void me(Context ctx) {
        Long id = SessionUtil.getUserId(ctx);
        if (id == null) { ctx.status(401).json(err("Not authenticated")); return; }
        userService.findById(id)
                .ifPresentOrElse(u -> ctx.json(toMap(u)),
                        () -> ctx.status(404).json(err("User not found")));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    public static Map<String, Object> toMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",          u.getId());
        m.put("username",    u.getUsername());
        m.put("email",       u.getEmail());
        m.put("role",        u.getRole().name());
        m.put("blocked",     u.isBlocked());
        m.put("systemAdmin", u.isSystemAdmin());
        return m;
    }

    static Map<String, String> err(String msg) { return Map.of("error", msg); }

    private static String str(Map<?, ?> m, String key) {
        Object v = m.get(key);
        return (v instanceof String s && !s.isBlank()) ? s.trim() : null;
    }
}