package edu.pucmm.icc352.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;

public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private static final String DEFAULT_SECRET = "MySecureJWTSecret1234567890123456";
    private static final String SECRET_STRING =
            System.getenv("JWT_SECRET") != null && !System.getenv("JWT_SECRET").isBlank()
                    ? System.getenv("JWT_SECRET")
                    : DEFAULT_SECRET;
    private static final SecretKey SECRET_KEY = Keys.hmacShaKeyFor(SECRET_STRING.getBytes());

    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;


    public static String generateToken(String userId, String username, String role) {
        return Jwts.builder()
                .claims()
                .add("userId",   userId)
                .add("username", username)
                .add("role",     role)
                .subject(username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .and()
                .signWith(SECRET_KEY)
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public static Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(SECRET_KEY)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (Exception e) {
            logger.error("Error extracting claims from token: {}", e.getMessage());
            return null;
        }
    }

    public static String extractUsername(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.getSubject() : null;
    }

    public static String extractUserId(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("userId", String.class) : null;
    }

    public static String extractRole(String token) {
        Claims claims = extractClaims(token);
        return claims != null ? claims.get("role", String.class) : null;
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }
}