package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// FIXME-CLOCK: JwtService uses Instant.now() internally — no injectable Clock (CLAUDE.md ADR).
// When Clock injection is added (planned before Phase 5), two tests depend on wall-clock time
// and must be updated to pass Clock.fixed(..., FLEET_TIMEZONE) into the JwtService constructor:
//   - isTokenValid_withValidToken_returnsTrue
//   - remainingTtl_withFreshToken_returnsPositiveDuration
// Every other test constructs tokens with explicit timestamps and is already clock-independent.
class JwtServiceTest {

    private static final String TEST_SECRET            = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2";
    private static final long   ACCESS_EXPIRY_SECONDS  = 900L;
    private static final long   REFRESH_EXPIRY_SECONDS = 604800L;

    private JwtService jwtService;
    private SecretKey  signingKey;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(TEST_SECRET, ACCESS_EXPIRY_SECONDS, REFRESH_EXPIRY_SECONDS);
        signingKey  = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
    }

    // ==================== generateAccessToken ====================

    @Test
    void generateAccessToken_withValidInputs_setsSubjectAndRoleClaims() {
        // Arrange
        Long   userId = 44L;
        String role   = "USER";

        // Act
        String token = jwtService.generateAccessToken(userId, role);

        // Assert
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("44");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
    }

    @Test
    void generateAccessToken_withValidInputs_ttlEqualsAccessExpirySeconds() {
        // Arrange
        Long   userId = 1L;
        String role   = "ADMIN";

        // Act
        String token = jwtService.generateAccessToken(userId, role);

        // Assert
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long ttlSeconds = Duration.between(
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        ).getSeconds();
        assertThat(ttlSeconds).isEqualTo(ACCESS_EXPIRY_SECONDS);
    }

    // ==================== generateRefreshToken ====================

    @Test
    void generateRefreshToken_withValidUserId_setsSubjectAndOmitsRoleClaim() {
        // Arrange / Act
        TokenService.GeneratedRefreshToken result = jwtService.generateRefreshToken(1L);

        // Assert
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(result.token())
                .getPayload();

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.get("role")).isNull();
    }

    @Test
    void generateRefreshToken_withValidUserId_ttlEqualsRefreshExpirySeconds() {
        // Arrange / Act
        TokenService.GeneratedRefreshToken result = jwtService.generateRefreshToken(1L);

        // Assert
        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(result.token())
                .getPayload();

        long ttlSeconds = Duration.between(
                claims.getIssuedAt().toInstant(),
                claims.getExpiration().toInstant()
        ).getSeconds();
        assertThat(ttlSeconds).isEqualTo(REFRESH_EXPIRY_SECONDS);
    }

    // ==================== isTokenValid ====================

    @Test
    void isTokenValid_withValidToken_returnsTrue() {
        // Arrange
        String token = jwtService.generateAccessToken(10L, "ADMIN");

        // Act
        boolean isValid = jwtService.isTokenValid(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void isTokenValid_withExpiredToken_returnsFalse() {
        // Arrange
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(signingKey)
                .compact();

        // Act
        boolean isValid = jwtService.isTokenValid(expiredToken);

        // Assert
        assertThat(isValid).isFalse();
    }

    @Test
    void isTokenValid_withWrongSignature_returnsFalse() {
        // Arrange
        String anotherSecret = "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLWZha2UtdG9rZW5z";
        JwtService anotherService = new JwtService(anotherSecret, ACCESS_EXPIRY_SECONDS, REFRESH_EXPIRY_SECONDS);
        String token = anotherService.generateAccessToken(99L, "USER");

        // Act
        boolean isValid = jwtService.isTokenValid(token);

        // Assert
        assertThat(isValid).isFalse();
    }

    // ==================== extractUserId ====================

    @Test
    void extractUserId_withValidToken_returnsCorrectLong() {
        // Arrange
        Long   expectedUserId = 77L;
        String token          = jwtService.generateAccessToken(expectedUserId, "ADMIN");

        // Act
        Long actualUserId = jwtService.extractUserId(token);

        // Assert
        assertThat(actualUserId).isEqualTo(expectedUserId);
    }

    @Test
    void extractUserId_withNonNumericSubject_throwsIllegalArgumentException() {
        // Arrange — valid signature, non-numeric subject triggers NumberFormatException in production
        String malformedToken = Jwts.builder()
                .subject("not-a-number")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(60)))
                .signWith(signingKey)
                .compact();

        // Act + Assert
        assertThatThrownBy(() -> jwtService.extractUserId(malformedToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid userId format in token");
    }

    // ==================== extractRole ====================

    @Test
    void extractRole_withAccessToken_returnsCorrectRole() {
        // Arrange
        String token = jwtService.generateAccessToken(1L, "ADMIN");

        // Act
        String role = jwtService.extractRole(token);

        // Assert
        assertThat(role).isEqualTo("ADMIN");
    }

    @Test
    void extractRole_withRefreshToken_returnsNull() {
        // Arrange — refresh tokens carry no role claim by design
        String refreshToken = jwtService.generateRefreshToken(1L).token();

        // Act
        String role = jwtService.extractRole(refreshToken);

        // Assert
        assertThat(role).isNull();
    }

    // ==================== remainingTtl ====================

    @Test
    void remainingTtl_withFreshToken_returnsPositiveDuration() {
        // Arrange
        String token = jwtService.generateAccessToken(1L, "USER");

        // Act
        Duration ttl = jwtService.remainingTtl(token);

        // Assert
        assertThat(ttl).isPositive();
        assertThat(ttl.getSeconds()).isLessThanOrEqualTo(ACCESS_EXPIRY_SECONDS);
    }

    @Test
    void remainingTtl_withExpiredToken_returnsDurationZero() {
        // Arrange
        String expiredToken = Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(signingKey)
                .compact();

        // Act
        Duration ttl = jwtService.remainingTtl(expiredToken);

        // Assert
        assertThat(ttl).isEqualTo(Duration.ZERO);
    }
}
