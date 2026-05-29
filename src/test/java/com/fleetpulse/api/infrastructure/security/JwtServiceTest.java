package com.fleetpulse.api.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;

import java.time.Duration;
import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String TEST_SECRET = "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2";

    private static final long ACCESS_EXPIRY_SECONDS = 900L;

    private static final long REFRESH_EXPIRY_SECONDS = 604800L;

    private JwtService jwtService;

    private SecretKey signingKey;

    @BeforeEach
    void setUp(){
        jwtService = new JwtService(TEST_SECRET, ACCESS_EXPIRY_SECONDS, REFRESH_EXPIRY_SECONDS);

        signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
    }

    @Test
    void gneratedAccestokenShouldContainCorrectClaims(){
        // Arrange
        Long userId = 44L;
        String role = "USER";

        // Act
        String token = jwtService.generateAccessToken(userId, role);

        Claims claims = Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        // Assert
        assertThat(claims.getSubject()).isEqualTo("44");
        assertThat(claims.get("role", String.class)).isEqualTo("USER");
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isNotNull();

    }

    @Test
    void validTokenShouldPassValidation(){
        // Arrange
        Long userId = 10L;
        String role = "ADMIN";

        // Act
        String token = jwtService.generateAccessToken(userId, role);

        boolean isValid = jwtService.isTokenValid(token);

        // Assert
        assertThat(isValid).isTrue();
    }

    @Test
    void tokenWithInvalidSignatureShouldFailValidation(){

        String anotherSecret = "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLWZha2UtdG9rZW5z";

        JwtService anotherService = new JwtService(anotherSecret, ACCESS_EXPIRY_SECONDS, REFRESH_EXPIRY_SECONDS);

        Long userId = 99L;
        String role = "USER";

        String token = anotherService.generateAccessToken(userId, role);
        boolean isValid = jwtService.isTokenValid(token);

        assertThat(isValid).isFalse();

    }

    @Test
    void extractUserIdShouldReturnCorrectValue(){

        // Arrange
        Long expectUserId = 77L;
        String token = jwtService.generateAccessToken(expectUserId, "ADMIN");

        // Act
        Long actualUserId = jwtService.extractUserId(token);

        // Assert
        assertThat(actualUserId).isEqualTo(expectUserId);
    }

    @Test
    void remainingTtlForNewTokenShouldBeGreaterThanZero(){

        String token = jwtService.generateAccessToken(1L, "USER");

        Duration ttl = jwtService.remainingTtl(token);

        assertThat(ttl).isPositive();

        assertThat(ttl.getSeconds()).isLessThanOrEqualTo(ACCESS_EXPIRY_SECONDS);

    }

}
