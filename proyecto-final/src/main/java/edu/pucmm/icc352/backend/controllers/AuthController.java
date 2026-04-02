package edu.pucmm.icc352.backend.controllers;

import edu.pucmm.icc352.backend.dto.LoginRequest;
import edu.pucmm.icc352.backend.dto.RegisterRequest;
import edu.pucmm.icc352.backend.services.AuthService;
import io.javalin.http.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Controller for authentication endpoints
 */
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    public AuthController() {
        this(new AuthService());
    }

    /**
     * POST /api/auth/register
     */
    public void register(Context ctx) {
        try {
            RegisterRequest request = ctx.bodyAsClass(RegisterRequest.class);
            Map<String, Object> response = authService.register(
                    request.getUsername(),
                    request.getPassword(),
                    request.getRole()
            );

            if ((Boolean) response.get("success")) {
                ctx.status(201).json(response);
            } else {
                ctx.status(400).json(response);
            }
        } catch (Exception e) {
            logger.error("Error in register endpoint", e);
            ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * POST /api/auth/login
     */
    public void login(Context ctx) {
        try {
            LoginRequest request = ctx.bodyAsClass(LoginRequest.class);
            Map<String, Object> response = authService.login(
                    request.getUsername(),
                    request.getPassword()
            );

            if ((Boolean) response.get("success")) {
                ctx.status(200).json(response);
            } else {
                ctx.status(401).json(response);
            }
        } catch (Exception e) {
            logger.error("Error in login endpoint", e);
            ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/auth/validate
     */
    public void validateToken(Context ctx) {
        try {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                ctx.status(400).json(Map.of(
                        "valid", false,
                        "message", "Missing or invalid Authorization header"
                ));
                return;
            }

            String token = authHeader.substring(7);
            Map<String, Object> response = authService.validateToken(token);

            if ((Boolean) response.get("valid")) {
                ctx.status(200).json(response);
            } else {
                ctx.status(401).json(response);
            }
        } catch (Exception e) {
            logger.error("Error in validate endpoint", e);
            ctx.status(500).json(Map.of(
                    "valid", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/auth/me - Get current user info
     */
    public void getCurrentUser(Context ctx) {
        try {
            String userId = ctx.attribute("userId");
            String username = ctx.attribute("username");
            String role = ctx.attribute("role");

            ctx.json(Map.of(
                    "success", true,
                    "user", Map.of(
                            "id", userId,
                            "username", username,
                            "role", role
                    )
            ));
        } catch (Exception e) {
            logger.error("Error in getCurrentUser endpoint", e);
            ctx.status(500).json(Map.of(
                    "success", false,
                    "message", "Internal server error: " + e.getMessage()
            ));
        }
    }
}
