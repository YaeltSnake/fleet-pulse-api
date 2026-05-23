package com.fleetpulse.api.application.port.out;

import java.time.Duration;
import java.time.Instant;

public interface TokenService {

    String generateAccessToken(Long userId, String role);

    String generateRefreshToken(Long userId);

    Long extractUserId(String token);

    boolean isTokenValid(String token, Long id);

    Duration remainingTtl(String token);

    Instant refreshTokenExpiresAt();

}
