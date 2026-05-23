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
        String refreshToken = tokenService.generateRefreshToken(user.getId());

        RefreshToken refreshTokenEntity = new RefreshToken(refreshToken, user.getId(), tokenService.refreshTokenExpiresAt(), false);
        refreshTokenRepository.save(refreshTokenEntity);
        return new AuthResult(accessToken, refreshToken);
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh Token Not Found"));


        if(refreshTokenEntity.getExpiresAt().isBefore(Instant.now())) throw new RefreshTokenExpiredException("Refresh token expired");
        if(refreshTokenEntity.isRevoked()) throw new RefreshTokenRevokedException("Refresh Token Revoked");

        refreshTokenRepository.revokeByToken(refreshToken);

        Long userId = refreshTokenEntity.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(refreshTokenEntity.getUserId()));


        if(!user.isActive()) throw new UserNotActiveException(user.getId().toString());

        String accessToken = tokenService.generateAccessToken(userId, user.getRole().name());
        String newRefreshToken = tokenService.generateRefreshToken(userId);

        RefreshToken newRefreshTokenEntity = new RefreshToken(newRefreshToken, userId, tokenService.refreshTokenExpiresAt(), false);
        refreshTokenRepository.save(newRefreshTokenEntity);

        return new AuthResult(accessToken, newRefreshToken);
    }

    @Override
    public void logout(String accessToken, String refreshToken) {

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh Token Not Found"));

        if (refreshTokenEntity.getExpiresAt().isBefore(Instant.now())) throw new RefreshTokenExpiredException("Refresh Token Expired");

        Long accessTokenUserId;
        try {
            accessTokenUserId = tokenService.extractUserId(accessToken);
        } catch (Exception e) {
            throw new InvalidCredentialsException("Invalid access token");
        }

        if (!accessTokenUserId.equals(refreshTokenEntity.getUserId())) {
            throw new InvalidCredentialsException("Token mismatch");
        }

        Duration duration = tokenService.remainingTtl(accessToken);
        if (!duration.isNegative() && !duration.isZero()) {
            tokenBlacklist.blacklist(accessToken, duration);
        }

        refreshTokenRepository.revokeByToken(refreshToken);

    }
}
