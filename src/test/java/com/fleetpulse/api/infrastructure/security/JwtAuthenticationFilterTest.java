package com.fleetpulse.api.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.application.port.in.UserManagementUseCase;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.ExternalServiceUnavailableException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class JwtAuthenticationFilterTest {

    @Autowired MockMvc        mockMvc;
    @Autowired TokenService   tokenService;
    @Autowired ObjectMapper   objectMapper;

    @MockitoBean TokenBlacklist          tokenBlacklist;
    @MockitoBean AuthUseCase             authUseCase;
    @MockitoBean UserManagementUseCase   userManagementUseCase;

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2";

    // Builds an expired token signed with the correct key — valid signature, claims past expiration
    private String buildExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET));
        return Jwts.builder()
                .subject("1")
                .issuedAt(Date.from(Instant.now().minusSeconds(120)))
                .expiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(key)
                .compact();
    }

    // Builds a token signed with a DIFFERENT secret — invalid signature, not expired
    private String buildWrongSignatureToken() {
        String anotherSecret = "YW5vdGhlci1zZWNyZXQta2V5LWZvci10ZXN0aW5nLWZha2UtdG9rZW5z";
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(anotherSecret));
        return Jwts.builder()
                .subject("1")
                .claim("role", "USER")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusSeconds(900)))
                .signWith(key)
                .compact();
    }

    @Test
    void requestWithoutAuthorizationHeader_passesChainAsAnonymous() throws Exception {
        // Arrange — no header; POST /api/auth/login is permitAll
        when(authUseCase.login(any())).thenReturn(
                new AuthUseCase.AuthResult("access-token", "refresh-token",
                        Instant.now().plusSeconds(900)));

        // Act + Assert — filter lets the request through (no 401 from filter on public endpoint)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "user", "password", "pass"))))
                .andExpect(status().isOk());
    }

    @Test
    void requestWithValidToken_setsAuthenticationAndGrantsAccess() throws Exception {
        // Arrange
        String token = tokenService.generateAccessToken(1L, "USER");
        when(tokenBlacklist.isBlacklisted(token)).thenReturn(false);

        // Act + Assert
        // 204 proves two things: (1) the filter set an Authentication object in the SecurityContext,
        // (2) the authority string is exactly "USER" — SecurityConfig uses hasAnyAuthority("ADMIN","USER")
        // which requires the exact string; "ROLE_USER" would produce 403.
        // Note: SecurityMockMvcResultMatchers.authenticated() is incompatible with STATELESS session
        // policy. Spring Security 6 uses NullSecurityContextRepository for stateless apps, so the
        // SecurityContext is never persisted to the session after the filter chain unwinds and the
        // matcher always finds null. The HTTP 204 is the correct assertion here.
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isNoContent());
    }

    @Test
    void requestWithExpiredToken_returns401AndClearsContext() throws Exception {
        // Arrange
        String expiredToken = buildExpiredToken();

        // Act + Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + expiredToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void requestWithInvalidSignature_returns401AndClearsContext() throws Exception {
        // Arrange
        String badToken = buildWrongSignatureToken();

        // Act + Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + badToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void requestWithBlacklistedToken_returns401AndClearsContext() throws Exception {
        // Arrange
        String token = tokenService.generateAccessToken(1L, "USER");
        when(tokenBlacklist.isBlacklisted(token)).thenReturn(true);

        // Act + Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void requestWithMalformedToken_returns401AndClearsContext() throws Exception {
        // Act + Assert — "not-a-jwt" passes the "Bearer " prefix check but fails JWT parsing
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer not-a-jwt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isUnauthorized())
                .andExpect(unauthenticated());
    }

    @Test
    void requestWhenRedisDown_returns503AndClearsContext() throws Exception {
        // Arrange
        String token = tokenService.generateAccessToken(1L, "USER");
        when(tokenBlacklist.isBlacklisted(any()))
                .thenThrow(new ExternalServiceUnavailableException("Redis unavailable"));

        // Act + Assert — fail-closed: Redis down → 503, never passes through
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(unauthenticated());
    }
}
