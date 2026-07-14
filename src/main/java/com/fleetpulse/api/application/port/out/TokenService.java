package com.fleetpulse.api.application.port.out;

import java.time.Duration;
import java.time.Instant;

public interface TokenService {

    record GeneratedRefreshToken(String token, Instant expiresAt){}

    record TokenClaims(Long userId, String role, Duration remainingTtl) {}

    String generateAccessToken(Long userId, String role);

    GeneratedRefreshToken generateRefreshToken(Long userId);

    Long extractUserId(String token);

    String extractRole(String token);

    boolean isTokenValid(String token);

    Duration remainingTtl(String token);

    /**
     * Parses the token exactly once, returning userId, role, and remaining TTL together.
     * Throws the same JJWT exceptions as an invalid/expired token would on any other parse
     * (ExpiredJwtException, SignatureException, MalformedJwtException, UnsupportedJwtException,
     * IllegalArgumentException) — callers on the hot path (JwtAuthenticationFilter) already
     * handle these as an invalid-token 401.
     */
    TokenClaims parseToken(String token);

}
