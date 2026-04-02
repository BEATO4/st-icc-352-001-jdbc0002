package edu.pucmm.icc352.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Utility class for JWT token generation and validation
 */
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Generate a secure key for HS256
    private static final SecretKey SECRET_KEY = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Token validity: 24 hours
    private static final long EXPIRATION_TIME = 1000 * 60 * 60 * 24;

    /**
     * Generate JWT token for a user
     */
    public static String generateToken(String userId, String username, String role) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);
        claims.put("role", role);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SECRET_KEY)
                .compact();
    }

    /**
     * Validate JWT token
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extract all claims from token
     */
    public static Claims extractClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(SECRET_KEY)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            logger.error("Error extracting claims from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Extract username from token
     */
    public static String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    /**
     * Extract user ID from token
     */
    public static String extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    /**
     * Extract role from token
     */
    public static String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    /**
     * Check if token is expired
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }
}
