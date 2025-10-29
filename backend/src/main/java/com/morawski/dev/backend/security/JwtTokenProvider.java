package com.morawski.dev.backend.security;

import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.InvalidTokenException;
import com.morawski.dev.backend.exception.TokenExpiredException;
import com.morawski.dev.backend.util.Constants;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWT Token Provider for authentication and authorization.
 * Handles token generation, validation, and claims extraction.
 *
 * Token Structure (API Plan Section 12.1):
 * - sub: User ID
 * - email: User email
 * - planType: User's plan type (FREE, PREMIUM)
 * - maxSourcesAllowed: Max sources for user's plan
 * - iat: Issued at timestamp
 * - exp: Expiration timestamp (60 minutes by default)
 *
 * Security:
 * - Uses HMAC SHA-256 signing algorithm
 * - Secret key from application.properties
 * - Stateless (no server-side session storage)
 */
@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long expirationMinutes;

    /**
     * Constructor with configuration from application.properties.
     *
     * @param jwtSecret Secret key for JWT signing
     * @param expirationMinutes Token expiration in minutes
     */
    public JwtTokenProvider(
        @Value("${jwt.secret}") String jwtSecret,
        @Value("${jwt.expiration-minutes:60}") long expirationMinutes
    ) {
        // Generate secure key from secret string
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.expirationMinutes = expirationMinutes;

        log.info("JWT Token Provider initialized with expiration: {} minutes", expirationMinutes);
    }

    /**
     * Generate JWT token from User entity.
     * API: POST /api/auth/register, POST /api/auth/login (Section 3.1, 3.2)
     *
     * Token contains:
     * - sub: User ID
     * - email: User email
     * - planType: User's subscription plan
     * - maxSourcesAllowed: Maximum sources for plan
     *
     * @param user User entity
     * @return JWT token string
     */
    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        claims.put("planType", user.getPlanType().name());
        claims.put("maxSourcesAllowed", user.getMaxSourcesAllowed());

        Instant now = Instant.now();
        Instant expiration = now.plus(expirationMinutes, ChronoUnit.MINUTES);

        String token = Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiration))
            .signWith(secretKey, SignatureAlgorithm.HS256)
            .compact();

        log.debug("Generated JWT token for user ID: {} (expires in {} minutes)",
            user.getId(), expirationMinutes);

        return token;
    }

    /**
     * Get token expiration instant.
     *
     * @return Expiration instant
     */
    public Instant getTokenExpiration() {
        return Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES);
    }

    /**
     * Extract user ID from JWT token.
     *
     * @param token JWT token string
     * @return User ID
     * @throws InvalidTokenException if token is invalid
     * @throws TokenExpiredException if token has expired
     */
    public Long extractUserId(String token) {
        String subject = extractClaims(token).getSubject();
        try {
            return Long.parseLong(subject);
        } catch (NumberFormatException e) {
            log.error("Invalid user ID in token subject: {}", subject);
            throw new InvalidTokenException("Invalid user ID in token");
        }
    }

    /**
     * Extract email from JWT token.
     *
     * @param token JWT token string
     * @return User email
     * @throws InvalidTokenException if token is invalid
     */
    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    /**
     * Extract plan type from JWT token.
     *
     * @param token JWT token string
     * @return PlanType
     * @throws InvalidTokenException if token is invalid
     */
    public PlanType extractPlanType(String token) {
        String planTypeStr = extractClaims(token).get("planType", String.class);
        try {
            return PlanType.valueOf(planTypeStr);
        } catch (IllegalArgumentException e) {
            log.error("Invalid plan type in token: {}", planTypeStr);
            throw new InvalidTokenException("Invalid plan type in token");
        }
    }

    /**
     * Extract max sources allowed from JWT token.
     *
     * @param token JWT token string
     * @return Maximum sources allowed
     * @throws InvalidTokenException if token is invalid
     */
    public Integer extractMaxSourcesAllowed(String token) {
        return extractClaims(token).get("maxSourcesAllowed", Integer.class);
    }

    /**
     * Validate JWT token.
     * Checks signature and expiration.
     *
     * @param token JWT token string
     * @return true if token is valid
     */
    public boolean validateToken(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (TokenExpiredException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return false;
        } catch (InvalidTokenException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());
            return false;
        } catch (SignatureException e) {
            log.error("Invalid JWT signature: {}", e.getMessage());
            return false;
        } catch (MalformedJwtException e) {
            log.error("Malformed JWT token: {}", e.getMessage());
            return false;
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported JWT token: {}", e.getMessage());
            return false;
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Check if JWT token is expired.
     *
     * @param token JWT token string
     * @return true if token is expired
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().before(new Date());
        } catch (ExpiredJwtException e) {
            return true;
        } catch (Exception e) {
            log.error("Error checking token expiration: {}", e.getMessage());
            return true;
        }
    }

    /**
     * Extract all claims from JWT token.
     * Internal method used by other extraction methods.
     *
     * @param token JWT token string
     * @return Claims object
     * @throws InvalidTokenException if token is invalid
     * @throws TokenExpiredException if token has expired
     */
    private Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (ExpiredJwtException e) {
            log.warn("Token expired for user: {}", e.getClaims().getSubject());
            throw new TokenExpiredException("JWT token has expired");
        } catch (SignatureException | MalformedJwtException | UnsupportedJwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            throw new InvalidTokenException("Invalid JWT token");
        } catch (IllegalArgumentException e) {
            log.error("JWT claims string is empty: {}", e.getMessage());
            throw new InvalidTokenException("JWT token is empty or null");
        }
    }

    /**
     * Extract bearer token from Authorization header.
     * Expected format: "Bearer {token}"
     *
     * @param authorizationHeader Authorization header value
     * @return JWT token string (without "Bearer " prefix)
     * @throws InvalidTokenException if header format is invalid
     */
    public String extractTokenFromHeader(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new InvalidTokenException("Authorization header is missing");
        }

        if (!authorizationHeader.startsWith(Constants.HEADER_BEARER_PREFIX)) {
            throw new InvalidTokenException("Authorization header must start with 'Bearer '");
        }

        String token = authorizationHeader.substring(Constants.HEADER_BEARER_PREFIX.length()).trim();

        if (token.isEmpty()) {
            throw new InvalidTokenException("JWT token is empty");
        }

        return token;
    }

    /**
     * Get token expiration minutes configuration.
     *
     * @return Expiration minutes
     */
    public long getExpirationMinutes() {
        return expirationMinutes;
    }
}
