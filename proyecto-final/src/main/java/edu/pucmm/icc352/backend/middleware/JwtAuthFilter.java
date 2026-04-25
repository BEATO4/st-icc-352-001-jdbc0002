package edu.pucmm.icc352.backend.middleware;

import edu.pucmm.icc352.backend.utils.JwtUtil;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class JwtAuthFilter implements Handler {

    @Override
    public void handle(@NotNull Context ctx) throws Exception {
        String authHeader = ctx.header("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            ctx.status(401).json(Map.of(
                    "success", false,
                    "message", "Missing or invalid Authorization header"
            ));
            return;
        }

        String token = authHeader.substring(7); // Remove "Bearer " prefix

        if (!JwtUtil.validateToken(token) || JwtUtil.isTokenExpired(token)) {
            ctx.status(401).json(Map.of(
                    "success", false,
                    "message", "Invalid or expired token"
            ));
            return;
        }

        ctx.attribute("userId", JwtUtil.extractUserId(token));
        ctx.attribute("username", JwtUtil.extractUsername(token));
        ctx.attribute("role", JwtUtil.extractRole(token));
    }

    public static boolean isAdmin(Context ctx) {
        String role = ctx.attribute("role");
        return role != null && role.equalsIgnoreCase("ADMIN");
    }

    public static void requireAdmin(Context ctx) {
        if (!isAdmin(ctx)) {
            ctx.status(403).json(Map.of(
                    "success", false,
                    "message", "Admin access required"
            ));
        }
    }
}
