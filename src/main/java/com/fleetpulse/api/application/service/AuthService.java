package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.application.port.out.*;
import com.fleetpulse.api.domain.exception.*;
import com.fleetpulse.api.domain.model.RefreshToken;
import com.fleetpulse.api.domain.model.User;

import java.time.Duration;
import java.time.Instant;

public class AuthService implements AuthUseCase {

    private final PasswordHasher passwordHasher;
    private final TokenBlacklist tokenBlacklist;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(PasswordHasher passwordHasher, TokenBlacklist tokenBlacklist,
                       TokenService tokenService, UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository) {
        this.passwordHasher = passwordHasher;
        this.tokenBlacklist = tokenBlacklist;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @Override
    public AuthResult login(String username, String password) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new UserNotFoundException(username));

        if(!user.isActive()) throw new UserNotActiveException(username);
        if (!passwordHasher.matches(password, user.getPasswordHash())) throw new InvalidCredentialsException("Invalid Credentials");

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getRole().name());
        TokenService.GeneratedRefreshToken generated = tokenService.generateRefreshToken(user.getId());

        RefreshToken storedRefreshToken = new RefreshToken(
                generated.token(),
                user.getId(),
                generated.expiresAt(),
                false);
        refreshTokenRepository.save(storedRefreshToken);
        return new AuthResult(accessToken, generated.token());
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh Token Not Found"));


        if(storedToken.getExpiresAt().isBefore(Instant.now())) throw new RefreshTokenExpiredException("Refresh token expired");
        if(storedToken.isRevoked()) throw new RefreshTokenRevokedException("Refresh Token Revoked");

        refreshTokenRepository.revokeByToken(refreshToken);

        Long userId = storedToken.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));


        if(!user.isActive()) throw new UserNotActiveException(user.getUsername());

        String accessToken = tokenService.generateAccessToken(userId, user.getRole().name());
        TokenService.GeneratedRefreshToken generated = tokenService.generateRefreshToken(userId);

        RefreshToken newStoredToken = new RefreshToken(
                generated.token(),
                userId,
                generated.expiresAt(),
                false);
        refreshTokenRepository.save(newStoredToken);

        return new AuthResult(accessToken, generated.token());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException("Refresh token expired");
        }

        Long accessTokenUserId = tokenService.extractUserId(accessToken);

        if (!accessTokenUserId.equals(storedToken.getUserId())) {
            throw new InvalidCredentialsException("Token mismatch");
        }

        Duration remaining = tokenService.remainingTtl(accessToken);
        if (!remaining.isNegative() && !remaining.isZero()) {
            tokenBlacklist.blacklist(accessToken, remaining);
        }

        refreshTokenRepository.revokeByToken(refreshToken);
    }
}
