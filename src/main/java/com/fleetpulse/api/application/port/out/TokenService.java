package com.fleetpulse.api.application.port.out;

import java.time.Duration;
import java.time.Instant;

public interface TokenService {

    record GeneratedRefreshToken(String token, Instant expiresAt){}

    String generateAccessToken(Long userId, String role);

    GeneratedRefreshToken generateRefreshToken(Long userId);

    Long extractUserId(String token);

    boolean isTokenValid(String token, Long userId);

    Duration remainingTtl(String token);

}
