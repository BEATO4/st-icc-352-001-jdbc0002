package edu.pucmm.icc352.backend.services;

import at.favre.lib.crypto.bcrypt.BCrypt;
import edu.pucmm.icc352.backend.utils.JwtUtil;
import edu.pucmm.icc352.models.User;
import edu.pucmm.icc352.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for authentication operations
 */
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public AuthService() {
        this(new UserRepository());
    }

    /**
     * Register a new user
     */
    public Map<String, Object> register(String username, String password, String role) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (username == null || username.trim().isEmpty()) {
                response.put("success", false);
                response.put("message", "Username is required");
                return response;
            }

            if (password == null || password.length() < 6) {
                response.put("success", false);
                response.put("message", "Password must be at least 6 characters");
                return response;
            }

            if (userRepository.existsByUsername(username)) {
                response.put("success", false);
                response.put("message", "Username already exists");
                return response;
            }

            String passwordHash = BCrypt.withDefaults().hashToString(12, password.toCharArray());

            String userRole = (role != null && !role.trim().isEmpty()) ? role : "USER";

            User user = new User(username, passwordHash, userRole);
            userRepository.create(user);

            String token = JwtUtil.generateToken(user.getIdAsString(), user.getUsername(), user.getRole());

            response.put("success", true);
            response.put("message", "User registered successfully");
            response.put("token", token);
            response.put("user", createUserMap(user));

            logger.info("User registered: {}", username);

        } catch (Exception e) {
            logger.error("Error during registration", e);
            response.put("success", false);
            response.put("message", "Registration failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Login user
     */
    public Map<String, Object> login(String username, String password) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (username == null || username.trim().isEmpty() || password == null || password.isEmpty()) {
                response.put("success", false);
                response.put("message", "Username and password are required");
                return response;
            }

            Optional<User> userOpt = userRepository.findByUsername(username);
            if (userOpt.isEmpty()) {
                response.put("success", false);
                response.put("message", "Invalid username or password");
                return response;
            }

            User user = userOpt.get();

            BCrypt.Result result = BCrypt.verifyer().verify(password.toCharArray(), user.getPasswordHash());
            if (!result.verified) {
                response.put("success", false);
                response.put("message", "Invalid username or password");
                return response;
            }

            String token = JwtUtil.generateToken(user.getIdAsString(), user.getUsername(), user.getRole());

            response.put("success", true);
            response.put("message", "Login successful");
            response.put("token", token);
            response.put("user", createUserMap(user));

            logger.info("User logged in: {}", username);

        } catch (Exception e) {
            logger.error("Error during login", e);
            response.put("success", false);
            response.put("message", "Login failed: " + e.getMessage());
        }

        return response;
    }

    /**
     * Validate JWT token
     */
    public Map<String, Object> validateToken(String token) {
        Map<String, Object> response = new HashMap<>();

        if (token == null || token.trim().isEmpty()) {
            response.put("valid", false);
            response.put("message", "Token is required");
            return response;
        }

        boolean isValid = JwtUtil.validateToken(token) && !JwtUtil.isTokenExpired(token);

        if (isValid) {
            String userId = JwtUtil.extractUserId(token);
            String username = JwtUtil.extractUsername(token);
            String role = JwtUtil.extractRole(token);

            response.put("valid", true);
            response.put("userId", userId);
            response.put("username", username);
            response.put("role", role);
        } else {
            response.put("valid", false);
            response.put("message", "Invalid or expired token");
        }

        return response;
    }

    /**
     * Helper method to create user map for response (without password hash)
     */
    private Map<String, Object> createUserMap(User user) {
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getIdAsString());
        userMap.put("username", user.getUsername());
        userMap.put("role", user.getRole());
        return userMap;
    }
}
