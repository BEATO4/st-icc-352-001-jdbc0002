package edu.pucmm.icc352.backend.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * Utility class for JWT token generation and validation.
 * Written for jjwt 0.12 / 0.13 — the parserBuilder() API was removed in 0.12.
 */
public class JwtUtil {
    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    // Hmac-SHA256 key — generated once per JVM lifetime.
    // In production load this from an environment variable so tokens survive restarts.
    // Keys.secretKeyFor(SignatureAlgorithm.HS256) was removed in 0.12;
    // use Jwts.SIG.HS256.key().build() instead.
    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();

    // Token validity: 24 hours
    private static final long EXPIRATION_TIME = 1000L * 60 * 60 * 24;

    // ── GENERATE ──────────────────────────────────────────────────────────────

    /**
     * Generate a signed JWT for a user.
     * Claims: userId, username, role — subject is set to username.
     */
    public static String generateToken(String userId, String username, String role) {
        return Jwts.builder()
                // jjwt 0.12: use claims() builder instead of setClaims(map)
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

    // ── VALIDATE ──────────────────────────────────────────────────────────────

    /**
     * Returns true if the token has a valid signature and is not expired.
     */
    public static boolean validateToken(String token) {
        try {
            // jjwt 0.12: Jwts.parser() + verifyWith() + parseSignedClaims()
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

    // ── EXTRACT ───────────────────────────────────────────────────────────────

    /**
     * Parse and return the Claims payload.
     * Returns null if the token is invalid or expired.
     */
    public static Claims extractClaims(String token) {
        try {
            // jjwt 0.12: getPayload() replaces getBody()
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

    /**
     * Returns true if the token's expiration timestamp is in the past.
     * Also returns true if the token cannot be parsed at all.
     */
    public static boolean isTokenExpired(String token) {
        Claims claims = extractClaims(token);
        if (claims == null) return true;
        return claims.getExpiration().before(new Date());
    }
}