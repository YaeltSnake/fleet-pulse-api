package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.InvalidCredentialsException;
import com.fleetpulse.api.domain.exception.RefreshTokenExpiredException;
import com.fleetpulse.api.domain.exception.RefreshTokenRevokedException;
import com.fleetpulse.api.domain.exception.UserNotActiveException;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    private static final String REFRESH_COOKIE = "refresh_token";

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;

    @MockitoBean AuthUseCase authUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;

    // ------------------------------------------------------------------ login

    @Test
    void login_withValidCredentials_returns200WithAccessTokenAndRefreshCookie() throws Exception {
        // Arrange
        when(authUseCase.login(any())).thenReturn(
                new AuthUseCase.AuthResult("access-token", "refresh-token",
                        Instant.now().plusSeconds(900)));

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "user", "password", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andReturn();

        // Assert — the refresh token travels ONLY via cookie, never in the response body
        MockCookie cookie = (MockCookie) result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("refresh-token");
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getSameSite()).isEqualTo("Strict");
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
        assertThat(cookie.getMaxAge()).isEqualTo(604800);
    }

    // Regression guard for FIXME-REFRESH-BODY — the whole point of this migration
    @Test
    void login_withValidCredentials_responseBodyNeverContainsRefreshTokenKey() throws Exception {
        // Arrange
        when(authUseCase.login(any())).thenReturn(
                new AuthUseCase.AuthResult("access-token", "refresh-token",
                        Instant.now().plusSeconds(900)));

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "user", "password", "pass"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").doesNotExist());
    }

    @Test
    void login_withInvalidCredentials_returns401ProblemDetail() throws Exception {
        // Arrange
        when(authUseCase.login(any())).thenThrow(new InvalidCredentialsException("Invalid credentials"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "user", "password", "wrong"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/invalid-credentials")));
    }

    @Test
    void login_withInactiveUser_returns401WithSameTypeAsInvalidCredentials() throws Exception {
        // Arrange — security: reason for rejection must NOT be revealed
        when(authUseCase.login(any())).thenThrow(new UserNotActiveException("inactive"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "inactive", "password", "pass"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/invalid-credentials")));
    }

    @Test
    void login_withBlankUsername_returns400ValidationFailed() throws Exception {
        // Act + Assert — Jakarta Validation fires before use case is called
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "", "password", "pass"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/validation-failed")));

        verify(authUseCase, never()).login(any());
    }

    @Test
    void login_withMissingBody_returns400() throws Exception {
        // Act + Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ----------------------------------------------------------------- refresh

    @Test
    void refresh_withValidCookie_returns200WithNewAccessTokenAndRotatedCookie() throws Exception {
        // Arrange
        when(authUseCase.refresh("old-refresh")).thenReturn(
                new AuthUseCase.AuthResult("new-access", "new-refresh",
                        Instant.now().plusSeconds(900)));

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, "old-refresh")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").doesNotExist())
                .andReturn();

        // Assert — cookie rotated to the new value
        MockCookie cookie = (MockCookie) result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getValue()).isEqualTo("new-refresh");
    }

    @Test
    void refresh_withExpiredToken_returns401ProblemDetail() throws Exception {
        // Arrange
        when(authUseCase.refresh(any())).thenThrow(new RefreshTokenExpiredException("Token expired"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, "expired-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/token-expired")));
    }

    @Test
    void refresh_withRevokedToken_returns401ProblemDetail() throws Exception {
        // Arrange
        when(authUseCase.refresh(any())).thenThrow(new RefreshTokenRevokedException("Token revoked"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(new Cookie(REFRESH_COOKIE, "revoked-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                // GlobalExceptionHandler maps RefreshTokenRevokedException → token-invalid (same as NotFound)
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/token-invalid")));
    }

    // Guard added in Phase 7 Layer 2 — a missing cookie must map to a clean 401, never a 500/NPE
    @Test
    void refresh_withMissingCookie_returns401NotServerError() throws Exception {
        // Act + Assert — no cookie() call at all
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/token-invalid")));

        verify(authUseCase, never()).refresh(any());
    }

    // ----------------------------------------------------------------- logout

    @Test
    void logout_withoutAuthorizationHeader_returns401() throws Exception {
        // Act + Assert — no token → filter skips → AuthenticationEntryPoint returns 401
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withValidBearerTokenAndCookie_returns204AndClearsCookie() throws Exception {
        // Arrange — generate a real JWT so the filter accepts it
        String accessToken = tokenService.generateAccessToken(1L, "USER");
        when(tokenBlacklist.isBlacklisted(accessToken)).thenReturn(false);
        // authUseCase.logout is void — does nothing by default

        // Act
        MvcResult result = mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(new Cookie(REFRESH_COOKIE, "my-refresh-token")))
                .andExpect(status().isNoContent())
                .andReturn();

        // Assert — the controller stripped "Bearer " correctly and forwarded the cookie value
        ArgumentCaptor<String> accessCaptor  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> refreshCaptor = ArgumentCaptor.forClass(String.class);
        verify(authUseCase).logout(accessCaptor.capture(), refreshCaptor.capture());
        assertThat(accessCaptor.getValue()).isEqualTo(accessToken);
        assertThat(refreshCaptor.getValue()).isEqualTo("my-refresh-token");

        // Assert — cookie cleared: same Path/SameSite/Secure attrs, Max-Age=0
        MockCookie cookie = (MockCookie) result.getResponse().getCookie(REFRESH_COOKIE);
        assertThat(cookie).isNotNull();
        assertThat(cookie.getMaxAge()).isZero();
        assertThat(cookie.getPath()).isEqualTo("/api/auth");
    }

    // Multi-tab case: refresh_token cookie already cleared in another tab, this tab still has
    // a live access token. Logout must still succeed and blacklist — see AuthService.logout().
    @Test
    void logout_withoutRefreshCookie_stillReturns204() throws Exception {
        // Arrange
        String accessToken = tokenService.generateAccessToken(1L, "USER");
        when(tokenBlacklist.isBlacklisted(accessToken)).thenReturn(false);

        // Act + Assert — no cookie() call at all
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        verify(authUseCase).logout(eq(accessToken), eq(null));
    }

    @Test
    void logout_withMalformedBearerPrefix_returns401() throws Exception {
        // Arrange — "Token " prefix instead of "Bearer "; filter skips → AuthenticationEntryPoint → 401
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Token some-value"))
                .andExpect(status().isUnauthorized());
    }
}
