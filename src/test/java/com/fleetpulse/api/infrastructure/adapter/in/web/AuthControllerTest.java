package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.InvalidCredentialsException;
import com.fleetpulse.api.domain.exception.RefreshTokenExpiredException;
import com.fleetpulse.api.domain.exception.RefreshTokenRevokedException;
import com.fleetpulse.api.domain.exception.UserNotActiveException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;

    @MockitoBean AuthUseCase authUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;

    // ------------------------------------------------------------------ login

    @Test
    void login_withValidCredentials_returns200WithTokens() throws Exception {
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
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"));
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
    void refresh_withValidToken_returns200WithNewPair() throws Exception {
        // Arrange
        when(authUseCase.refresh(any())).thenReturn(
                new AuthUseCase.AuthResult("new-access", "new-refresh",
                        Instant.now().plusSeconds(900)));

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "old-refresh"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh"));
    }

    @Test
    void refresh_withExpiredToken_returns401ProblemDetail() throws Exception {
        // Arrange
        when(authUseCase.refresh(any())).thenThrow(new RefreshTokenExpiredException("Token expired"));

        // Act + Assert
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "expired-token"))))
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "revoked-token"))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                // GlobalExceptionHandler maps RefreshTokenRevokedException → token-invalid (same as NotFound)
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/token-invalid")));
    }

    // ----------------------------------------------------------------- logout

    @Test
    void logout_withoutAuthorizationHeader_returns401() throws Exception {
        // Act + Assert — no token → filter skips → AuthenticationEntryPoint returns 401
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_withValidBearerToken_returns204() throws Exception {
        // Arrange — generate a real JWT so the filter accepts it
        String accessToken = tokenService.generateAccessToken(1L, "USER");
        when(tokenBlacklist.isBlacklisted(accessToken)).thenReturn(false);
        // authUseCase.logout is void — does nothing by default

        // Act + Assert
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "my-refresh-token"))))
                .andExpect(status().isNoContent());

        // Verify the controller stripped "Bearer " correctly
        ArgumentCaptor<String> accessCaptor  = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> refreshCaptor = ArgumentCaptor.forClass(String.class);
        verify(authUseCase).logout(accessCaptor.capture(), refreshCaptor.capture());
        assertThat(accessCaptor.getValue()).isEqualTo(accessToken);
        assertThat(refreshCaptor.getValue()).isEqualTo("my-refresh-token");
    }

    @Test
    void logout_withMalformedBearerPrefix_returns401() throws Exception {
        // Arrange — "Token " prefix instead of "Bearer "; filter skips → AuthenticationEntryPoint → 401
        mockMvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Token some-value")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("refreshToken", "any-refresh"))))
                .andExpect(status().isUnauthorized());
    }
}
