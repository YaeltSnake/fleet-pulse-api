package com.fleetpulse.api.application.service;


import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.application.port.out.*;
import com.fleetpulse.api.application.service.command.LoginCommand;
import com.fleetpulse.api.domain.exception.*;
import com.fleetpulse.api.domain.model.RefreshToken;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenBlacklist tokenBlacklist;
    @Mock private TokenService tokenService;
    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_withValidCredentials_returnsAuthResultWithTokens() {
        // Arrange
        User user = new User(1L, "user-test", "hash", Role.ADMIN, true);
        Instant expectedExpiry = Instant.now().plusSeconds(604800);

        when(userRepository.findByUsername("user-test")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password", "hash")).thenReturn(true);
        when(tokenService.generateAccessToken(1L, "ADMIN")).thenReturn("Access Token");
        when(tokenService.generateRefreshToken(1L))
                .thenReturn(new TokenService.GeneratedRefreshToken("Refresh Token", expectedExpiry));

        // Act
        AuthUseCase.AuthResult result = authService.login(new LoginCommand("user-test", "password"));

        // Assert
        assertThat(result.accessToken()).isEqualTo("Access Token");
        assertThat(result.refreshToken()).isEqualTo("Refresh Token");
        assertThat(result.expiresAt()).isEqualTo(expectedExpiry);
        verify(passwordHasher).matches("password", "hash");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_withNonExistentUser_throwsInvalidCredentialsException() {
        // Arrange
        LoginCommand command = new LoginCommand("ghost", "password");
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());
        when(passwordHasher.matches(eq("password"), any())).thenReturn(false);

        // Act
        // Assert
        assertThatThrownBy(() -> authService.login(command))
                .isInstanceOf(InvalidCredentialsException.class);

        // Then: timing-attack defense — dummy hash WAS computed even though user doesn't exist
        verify(passwordHasher).matches(eq("password"), any());
        verify(tokenService, never()).generateAccessToken(any(), any());
        verify(tokenService, never()).generateRefreshToken(anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_withWrongPassword_throwsInvalidCredentialsException() {
        // Arrange
        User user = new User(1L, "admin", "hash", Role.ADMIN, true);
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password", "hash")).thenReturn(false);

        // Act
        // Assert
        assertThatThrownBy(() -> authService.login(new LoginCommand("admin", "password")))
                .isInstanceOf(InvalidCredentialsException.class);

        // Then: password was actually checked before rejecting
        verify(passwordHasher).matches("password", "hash");
        verify(tokenService, never()).generateAccessToken(any(), any());
        verify(tokenService, never()).generateRefreshToken(anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_withInactiveUser_throwsUserNotActiveException() {
        // Arrange
        User inactiveUser = new User(1L, "user", "hash", Role.USER, false);
        when(userRepository.findByUsername("user")).thenReturn(Optional.of(inactiveUser));

        // Act
        // Assert
        assertThatThrownBy(() -> authService.login(new LoginCommand("user", "password")))
                .isInstanceOf(UserNotActiveException.class);

        // Then: inactive guard triggers BEFORE password check
        verify(passwordHasher, never()).matches(any(), any());
        verify(tokenService, never()).generateAccessToken(any(), any());
        verify(tokenService, never()).generateRefreshToken(anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void login_savesRefreshTokenWithCorrectFields() {
        // Arrange
        User user = new User(1L, "user", "hash", Role.USER, true);
        Instant expectedExpiry = Instant.now().plusSeconds(604800);
        TokenService.GeneratedRefreshToken generatedRefresh =
                new TokenService.GeneratedRefreshToken("refresh-token", expectedExpiry);

        when(userRepository.findByUsername("user")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("password", "hash")).thenReturn(true);
        when(tokenService.generateAccessToken(1L, "USER")).thenReturn("new-access-token");
        when(tokenService.generateRefreshToken(1L)).thenReturn(generatedRefresh);

        // Act
        authService.login(new LoginCommand("user", "password"));

        // Assert
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.getToken()).isEqualTo("refresh-token");
        assertThat(saved.isRevoked()).isFalse();
        assertThat(saved.getExpiresAt()).isEqualTo(expectedExpiry);
    }

    @Test
    void refresh_withValidToken_revokesOldAndIssuesNew() {
        // Arrange
        User activeUser = new User(1L, "user", "hash", Role.USER, true);
        Instant newExpiry = Instant.now().plusSeconds(604800);
        RefreshToken oldRefresh = new RefreshToken("old-refresh", 1L, Instant.now().plusSeconds(604800), false);

        when(refreshTokenRepository.findByToken("old-refresh")).thenReturn(Optional.of(oldRefresh));
        when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
        when(tokenService.generateAccessToken(1L, "USER")).thenReturn("new-access");
        when(tokenService.generateRefreshToken(1L))
                .thenReturn(new TokenService.GeneratedRefreshToken("new-refresh", newExpiry));

        // Act
        AuthUseCase.AuthResult result = authService.refresh("old-refresh");

        // Assert
        verify(refreshTokenRepository).revokeByToken("old-refresh");

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        RefreshToken saved = captor.getValue();

        assertThat(saved.getToken()).isEqualTo("new-refresh");
        assertThat(saved.getUserId()).isEqualTo(1L);
        assertThat(saved.isRevoked()).isFalse();

        assertThat(result.accessToken()).isEqualTo("new-access");
        assertThat(result.refreshToken()).isEqualTo("new-refresh");
        assertThat(result.expiresAt()).isEqualTo(newExpiry);
    }

    @Test
    void refresh_withExpiredToken_throwsRefreshTokenExpiredException() {
        // Arrange
        Instant past = Instant.now().minusSeconds(1);
        RefreshToken expiredToken = new RefreshToken("expired-refresh", 1L, past, false);
        when(refreshTokenRepository.findByToken("expired-refresh")).thenReturn(Optional.of(expiredToken));

        // Act
        // Assert
        assertThatThrownBy(() -> authService.refresh("expired-refresh"))
                .isInstanceOf(RefreshTokenExpiredException.class);

        verify(refreshTokenRepository, never()).revokeByToken(any());
        verify(userRepository, never()).findById(anyLong());
        verify(tokenService, never()).generateAccessToken(anyLong(), any());
        verify(tokenService, never()).generateRefreshToken(anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_withRevokedToken_throwsRefreshTokenRevokedException() {
        // Arrange
        Instant future = Instant.now().plusSeconds(604800);
        RefreshToken revokedToken = new RefreshToken("revoked-refresh", 1L, future, true);
        when(refreshTokenRepository.findByToken("revoked-refresh")).thenReturn(Optional.of(revokedToken));

        // Act
        // Assert
        assertThatThrownBy(() -> authService.refresh("revoked-refresh"))
                .isInstanceOf(RefreshTokenRevokedException.class);

        verify(refreshTokenRepository, never()).revokeByToken(any());
        verify(userRepository, never()).findById(anyLong());
        verify(tokenService, never()).generateAccessToken(anyLong(), any());
        verify(tokenService, never()).generateRefreshToken(anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void refresh_withNonExistentToken_throwsRefreshTokenNotFoundException() {
        // Arrange
        when(refreshTokenRepository.findByToken("unknown-refresh")).thenReturn(Optional.empty());

        // Act
        // Assert
        assertThatThrownBy(() -> authService.refresh("unknown-refresh"))
                .isInstanceOf(RefreshTokenNotFoundException.class);

        verify(refreshTokenRepository, never()).revokeByToken(any());
        verify(userRepository, never()).findById(anyLong());
        verify(tokenService, never()).generateAccessToken(anyLong(), any());
        verify(tokenService, never()).generateRefreshToken(anyLong());
        verify(refreshTokenRepository, never()).save(any());
    }

    @Test
    void logout_blacklistsAccessTokenWithRemainingTtl() {
        // Arrange
        Instant future = Instant.now().plusSeconds(604800);
        RefreshToken validRefresh = new RefreshToken("refresh-token", 1L, future, false);
        String accessToken = "access-token";
        Duration remaining = Duration.ofMinutes(10);

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(validRefresh));
        when(tokenService.extractUserId(accessToken)).thenReturn(1L);
        when(tokenService.remainingTtl(accessToken)).thenReturn(remaining);

        // Act
        authService.logout(accessToken, "refresh-token");

        // Assert
        verify(tokenBlacklist).blacklist(accessToken, remaining);
        verify(refreshTokenRepository).revokeByToken("refresh-token");
    }

    @Test
    void logout_revokesRefreshTokenInDb() {
        // Arrange
        Instant future = Instant.now().plusSeconds(604800);
        RefreshToken validRefresh = new RefreshToken("refresh-token", 1L, future, false);
        String accessToken = "access-token";
        Duration remaining = Duration.ofMinutes(10);

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(validRefresh));
        when(tokenService.extractUserId(accessToken)).thenReturn(1L);
        when(tokenService.remainingTtl(accessToken)).thenReturn(remaining);

        // Act
        authService.logout(accessToken, "refresh-token");

        // Assert
        verify(refreshTokenRepository).revokeByToken("refresh-token");
        verify(tokenBlacklist).blacklist(accessToken, remaining);
    }

    @Test
    void logout_withAlreadyExpiredAccessToken_doesNotBlacklist() {
        // Arrange
        Instant future = Instant.now().plusSeconds(604800);
        RefreshToken validRefresh = new RefreshToken("refresh-token", 1L, future, false);
        String accessToken = "expired-access-token";
        Duration negativeRemaining = Duration.ofMinutes(-5);

        when(refreshTokenRepository.findByToken("refresh-token")).thenReturn(Optional.of(validRefresh));
        when(tokenService.extractUserId(accessToken)).thenReturn(1L);
        when(tokenService.remainingTtl(accessToken)).thenReturn(negativeRemaining);

        // Act
        authService.logout(accessToken, "refresh-token");

        // Assert
        verify(tokenBlacklist, never()).blacklist(any(), any());
        verify(refreshTokenRepository).revokeByToken("refresh-token");
    }
}

