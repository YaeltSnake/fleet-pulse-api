package com.fleetpulse.api.application.port.out;

import java.time.Duration;

public interface TokenService {

    String generateAccessToken(String username, String role);

    String generateRefreshToken(String username);

    String extractUsername(String token);

    boolean isTokenValid(String token, String username);

    Duration remainingTtl(String token);

}
