package com.morawski.dev.backend.security;

import com.morawski.dev.backend.dto.common.PlanType;
import com.morawski.dev.backend.entity.User;
import com.morawski.dev.backend.exception.InvalidTokenException;
import com.morawski.dev.backend.exception.TokenExpiredException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtTokenProvider Tests")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private User testUser;
    private final String jwtSecret = "test-secret-key-that-is-long-enough-for-hs256-algorithm-minimum-32-bytes";
    private final long expirationMinutes = 60;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(jwtSecret, expirationMinutes);

        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .passwordHash("hashedPassword")
            .planType(PlanType.FREE)
            .maxSourcesAllowed(1)
            .emailVerified(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();
    }

    @Nested
    @DisplayName("generateToken() Tests")
    class GenerateTokenTests {

        @Test
        @DisplayName("Should generate valid JWT token")
        void shouldGenerateValidToken() {
            // When
            String token = jwtTokenProvider.generateToken(testUser);

            // Then
            assertThat(token).isNotNull();
            assertThat(token).isNotEmpty();
            assertThat(token.split("\\.")).hasSize(3); // JWT has 3 parts
        }

        @Test
        @DisplayName("Should include user ID in subject")
        void shouldIncludeUserIdInSubject() {
            // When
            String token = jwtTokenProvider.generateToken(testUser);
            Long userId = jwtTokenProvider.extractUserId(token);

            // Then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should include email in claims")
        void shouldIncludeEmailInClaims() {
            // When
            String token = jwtTokenProvider.generateToken(testUser);
            String email = jwtTokenProvider.extractEmail(token);

            // Then
            assertThat(email).isEqualTo("test@example.com");
        }

        @Test
        @DisplayName("Should include plan type in claims")
        void shouldIncludePlanTypeInClaims() {
            // When
            String token = jwtTokenProvider.generateToken(testUser);
            PlanType planType = jwtTokenProvider.extractPlanType(token);

            // Then
            assertThat(planType).isEqualTo(PlanType.FREE);
        }

        @Test
        @DisplayName("Should include max sources allowed in claims")
        void shouldIncludeMaxSourcesAllowedInClaims() {
            // When
            String token = jwtTokenProvider.generateToken(testUser);
            Integer maxSourcesAllowed = jwtTokenProvider.extractMaxSourcesAllowed(token);

            // Then
            assertThat(maxSourcesAllowed).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("extractUserId() Tests")
    class ExtractUserIdTests {

        @Test
        @DisplayName("Should extract user ID from valid token")
        void shouldExtractUserIdFromValidToken() {
            // Given
            String token = jwtTokenProvider.generateToken(testUser);

            // When
            Long userId = jwtTokenProvider.extractUserId(token);

            // Then
            assertThat(userId).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is malformed")
        void shouldThrowException_WhenTokenMalformed() {
            // Given
            String malformedToken = "invalid.token.here";

            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractUserId(malformedToken))
                .isInstanceOf(InvalidTokenException.class);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when subject is not a number")
        void shouldThrowException_WhenSubjectNotNumber() {
            // Given
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String tokenWithInvalidSubject = Jwts.builder()
                .setSubject("not-a-number")
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractUserId(tokenWithInvalidSubject))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Invalid user ID in token");
        }
    }

    @Nested
    @DisplayName("validateToken() Tests")
    class ValidateTokenTests {

        @Test
        @DisplayName("Should return true for valid token")
        void shouldReturnTrue_ForValidToken() {
            // Given
            String token = jwtTokenProvider.generateToken(testUser);

            // When
            boolean isValid = jwtTokenProvider.validateToken(token);

            // Then
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("Should return false for expired token")
        void shouldReturnFalse_ForExpiredToken() {
            // Given
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                .setSubject("1")
                .setIssuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .setExpiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

            // When
            boolean isValid = jwtTokenProvider.validateToken(expiredToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for malformed token")
        void shouldReturnFalse_ForMalformedToken() {
            // Given
            String malformedToken = "invalid.token.here";

            // When
            boolean isValid = jwtTokenProvider.validateToken(malformedToken);

            // Then
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("Should return false for token with invalid signature")
        void shouldReturnFalse_ForInvalidSignature() {
            // Given
            SecretKey differentKey = Keys.hmacShaKeyFor("different-secret-key-for-testing-minimum-32-bytes-required".getBytes(StandardCharsets.UTF_8));
            String tokenWithDifferentKey = Jwts.builder()
                .setSubject("1")
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(60, ChronoUnit.MINUTES)))
                .signWith(differentKey, SignatureAlgorithm.HS256)
                .compact();

            // When
            boolean isValid = jwtTokenProvider.validateToken(tokenWithDifferentKey);

            // Then
            assertThat(isValid).isFalse();
        }
    }

    @Nested
    @DisplayName("isTokenExpired() Tests")
    class IsTokenExpiredTests {

        @Test
        @DisplayName("Should return false for valid non-expired token")
        void shouldReturnFalse_ForNonExpiredToken() {
            // Given
            String token = jwtTokenProvider.generateToken(testUser);

            // When
            boolean isExpired = jwtTokenProvider.isTokenExpired(token);

            // Then
            assertThat(isExpired).isFalse();
        }

        @Test
        @DisplayName("Should return true for expired token")
        void shouldReturnTrue_ForExpiredToken() {
            // Given
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                .setSubject("1")
                .setIssuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .setExpiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

            // When
            boolean isExpired = jwtTokenProvider.isTokenExpired(expiredToken);

            // Then
            assertThat(isExpired).isTrue();
        }
    }

    @Nested
    @DisplayName("extractTokenFromHeader() Tests")
    class ExtractTokenFromHeaderTests {

        @Test
        @DisplayName("Should extract token from valid Bearer header")
        void shouldExtractToken_FromValidBearerHeader() {
            // Given
            String token = jwtTokenProvider.generateToken(testUser);
            String authHeader = "Bearer " + token;

            // When
            String extractedToken = jwtTokenProvider.extractTokenFromHeader(authHeader);

            // Then
            assertThat(extractedToken).isEqualTo(token);
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when header is null")
        void shouldThrowException_WhenHeaderIsNull() {
            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractTokenFromHeader(null))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Authorization header is missing");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when header is blank")
        void shouldThrowException_WhenHeaderIsBlank() {
            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractTokenFromHeader(""))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Authorization header is missing");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when header doesn't start with Bearer")
        void shouldThrowException_WhenHeaderDoesNotStartWithBearer() {
            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractTokenFromHeader("Basic token123"))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("Authorization header must start with 'Bearer '");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException when token is empty after Bearer")
        void shouldThrowException_WhenTokenEmptyAfterBearer() {
            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractTokenFromHeader("Bearer "))
                .isInstanceOf(InvalidTokenException.class)
                .hasMessageContaining("JWT token is empty");
        }
    }

    @Nested
    @DisplayName("getTokenExpiration() Tests")
    class GetTokenExpirationTests {

        @Test
        @DisplayName("Should return future expiration time")
        void shouldReturnFutureExpirationTime() {
            // When
            Instant expiration = jwtTokenProvider.getTokenExpiration();

            // Then
            assertThat(expiration).isAfter(Instant.now());
            assertThat(expiration).isBefore(Instant.now().plus(61, ChronoUnit.MINUTES));
        }
    }

    @Nested
    @DisplayName("getExpirationMinutes() Tests")
    class GetExpirationMinutesTests {

        @Test
        @DisplayName("Should return configured expiration minutes")
        void shouldReturnConfiguredExpirationMinutes() {
            // When
            long minutes = jwtTokenProvider.getExpirationMinutes();

            // Then
            assertThat(minutes).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("extractClaims() Tests - via extractEmail()")
    class ExtractClaimsTests {

        @Test
        @DisplayName("Should throw TokenExpiredException for expired token")
        void shouldThrowTokenExpiredException_ForExpiredToken() {
            // Given
            SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
            String expiredToken = Jwts.builder()
                .setSubject("1")
                .claim("email", "test@example.com")
                .setIssuedAt(Date.from(Instant.now().minus(2, ChronoUnit.HOURS)))
                .setExpiration(Date.from(Instant.now().minus(1, ChronoUnit.HOURS)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractEmail(expiredToken))
                .isInstanceOf(TokenExpiredException.class)
                .hasMessageContaining("JWT token has expired");
        }

        @Test
        @DisplayName("Should throw InvalidTokenException for empty token")
        void shouldThrowInvalidTokenException_ForEmptyToken() {
            // When & Then
            assertThatThrownBy(() -> jwtTokenProvider.extractEmail(""))
                .isInstanceOf(InvalidTokenException.class);
        }
    }
}
