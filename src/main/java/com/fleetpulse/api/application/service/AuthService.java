package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.AuthUseCase;
import com.fleetpulse.api.application.port.out.*;
import com.fleetpulse.api.application.service.command.LoginCommand;
import com.fleetpulse.api.domain.exception.*;
import com.fleetpulse.api.domain.model.RefreshToken;
import com.fleetpulse.api.domain.model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AuthService implements AuthUseCase {

    private static final Pattern BCRYPT_COST_FACTOR_PATTERN = Pattern.compile("^\\$2[aby]?\\$(\\d+)\\$");

    // Constant-time defense against user enumeration (ASVS V2.7.1).
    // Cost factor MUST match app.security.bcrypt-strength (BCRYPT_STRENGTH env var) — the
    // constructor guard below fails fast at startup if they ever drift apart, since a mismatch
    // would make passwordHasher.matches() against this dummy run at a different cost than a real
    // hash, reopening the timing side-channel this constant exists to close.
    private static final String DUMMY_HASH =
            "$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqhmM6JGKpS4G3R1G2JH8YpfB0Bqy";

    private final PasswordHasher passwordHasher;
    private final TokenBlacklist tokenBlacklist;
    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    public AuthService(PasswordHasher passwordHasher, TokenBlacklist tokenBlacklist,
                       TokenService tokenService, UserRepository userRepository,
                       RefreshTokenRepository refreshTokenRepository, int bcryptStrength) {
        this.passwordHasher = passwordHasher;
        this.tokenBlacklist = tokenBlacklist;
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        requireDummyHashMatchesConfiguredStrength(bcryptStrength);
    }

    private static void requireDummyHashMatchesConfiguredStrength(int bcryptStrength) {
        Matcher matcher = BCRYPT_COST_FACTOR_PATTERN.matcher(DUMMY_HASH);
        if (!matcher.find()) {
            throw new IllegalStateException("DUMMY_HASH is not a valid BCrypt hash");
        }

        int dummyHashCostFactor = Integer.parseInt(matcher.group(1));
        if (dummyHashCostFactor != bcryptStrength) {
            throw new IllegalStateException(
                    "app.security.bcrypt-strength (" + bcryptStrength + ") does not match the cost "
                    + "factor embedded in AuthService.DUMMY_HASH (" + dummyHashCostFactor + "). "
                    + "Regenerate DUMMY_HASH at the configured strength before deploying — a mismatch "
                    + "reopens the ASVS V2.7.1 timing side-channel.");
        }
    }

    @Override
    public AuthResult login(LoginCommand command) {
        Optional<User> userOpt = userRepository.findByUsername(command.username());

        if (userOpt.isEmpty()) {
            passwordHasher.matches(command.password(), DUMMY_HASH);
            throw new InvalidCredentialsException("Invalid Credentials");
        }

        User user = userOpt.get();

        if (!user.isActive()) {
            throw new UserNotActiveException(user.getUsername());
        }

        if (!passwordHasher.matches(command.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException("Invalid Credentials");
        }

        refreshTokenRepository.revokeAllByUserId(user.getId());

        String accessToken = tokenService.generateAccessToken(user.getId(), user.getRole().name());
        TokenService.GeneratedRefreshToken generated = tokenService.generateRefreshToken(user.getId());

        RefreshToken storedRefreshToken = new RefreshToken(
                generated.token(),
                user.getId(),
                generated.expiresAt(),
                false);
        refreshTokenRepository.save(storedRefreshToken);

        return new AuthResult(accessToken, generated.token(), generated.expiresAt());
    }

    @Override
    public AuthResult refresh(String refreshToken) {
        RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh Token Not Found"));

        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            throw new RefreshTokenExpiredException("Refresh token expired");
        }

        if (storedToken.isRevoked()) {
            throw new RefreshTokenRevokedException("Refresh Token Revoked");
        }

        // FIXME-SEC-FAMILY: Refresh Token Families not implemented (OAuth 2.0 BCP Section 4.14).
        // Token theft is undetectable — a stolen+used token only triggers RevokedException
        // on the legitimate user's next refresh, with no alert or forced re-login for all sessions.
        // Implement token family tracking in Phase 8+.
        refreshTokenRepository.revokeByToken(refreshToken);

        Long userId = storedToken.getUserId();

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isActive()) {
            throw new UserNotActiveException(user.getUsername());
        }

        String accessToken = tokenService.generateAccessToken(userId, user.getRole().name());
        TokenService.GeneratedRefreshToken generated = tokenService.generateRefreshToken(userId);

        RefreshToken newStoredToken = new RefreshToken(
                generated.token(),
                userId,
                generated.expiresAt(),
                false);
        refreshTokenRepository.save(newStoredToken);

        return new AuthResult(accessToken, generated.token(), generated.expiresAt());
    }

    @Override
    public void logout(String accessToken, String refreshToken) {
        // refreshToken may be null — multi-tab case: user already logged out in another tab,
        // the refresh_token cookie is gone, but this tab still holds a live access token.
        // Full logout must still happen: blacklist the access token, just skip the DB revoke.
        if (refreshToken != null) {
            RefreshToken storedToken = refreshTokenRepository.findByToken(refreshToken)
                    .orElseThrow(() -> new RefreshTokenNotFoundException("Refresh token not found"));

            Long accessTokenUserId = tokenService.extractUserId(accessToken);

            if (!accessTokenUserId.equals(storedToken.getUserId())) {
                throw new InvalidCredentialsException("Token mismatch");
            }
        }

        Duration remaining = tokenService.remainingTtl(accessToken);
        if (!remaining.isNegative() && !remaining.isZero()) {
            tokenBlacklist.blacklist(accessToken, remaining);
        }

        if (refreshToken != null) {
            refreshTokenRepository.revokeByToken(refreshToken);
        }
    }
}
