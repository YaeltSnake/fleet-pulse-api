package com.fleetpulse.api.application.port.out;

import java.time.Duration;
import java.time.Instant;
// FIXME-PERF (Layer 4.5): TokenService performs redundant cryptographic parsing per request.
//
// PROBLEM:
//   Each authenticated request triggers 3 independent JWT parse operations:
//     1. tokenService.isTokenValid(token)     -> Jwts.parser().verifyWith(key).build().parseSignedClaims()
//     2. tokenService.extractUserId(token)    -> Jwts.parser().verifyWith(key).build().parseSignedClaims()
//     3. tokenService.extractRole(token)       -> Jwts.parser().verifyWith(key).build().parseSignedClaims()
//
//   Base64 decoding + HMAC-SHA256 verification is CPU-intensive. At 1,000 req/s,
//   this triples cryptographic load with no security benefit.
//
// IMPACT:
//   - Increased latency per request (~0.3-0.8ms per redundant parse on modern hardware)
//   - Higher CPU utilization under load, reducing headroom for burst traffic
//   - No functional defect; purely a performance optimization
//
// ROOT CAUSE:
//   TokenService interface was designed with single-responsibility methods for testability
//   (Phase 3, Layer 2). Each method encapsulates its own parsing logic, which is correct
//   for isolated unit tests but suboptimal for the hot path in JwtAuthenticationFilter.
//
// RESOLUTION PLAN:
//   1. Introduce TokenClaims record: { Long userId; String role; Instant expiration; }
//   2. Add TokenService.parseToken(String token) -> Optional<TokenClaims>
//      - Performs single parse, returns all claims atomically
//      - Validates signature and expiration inline
//   3. Refactor JwtAuthenticationFilter to use parseToken() instead of sequential calls
//   4. Deprecate (but do not remove) extractUserId() and extractRole() for backward
//      compatibility with existing tests and any non-filter consumers
//   5. Update JwtServiceTest to verify parseToken() contract
//
// BLOCKED BY:
//   - Layer 7 (Tests): Must confirm end-to-end auth flow via AuthControllerTest before
//     touching the TokenService contract. Changing the interface now risks breaking
//     controller tests that depend on the current method signatures.
//
//   - Layer 4.6 (SecurityConfig + ApplicationConfig): Must be complete and stable so
//     that the filter wiring is frozen before we refactor its dependencies.
//
//   - Layer 5 (Controllers): AuthController must be fully implemented and green-tested
//     to serve as the integration baseline for the refactor.
//
// TIMELINE:
//   Target: Immediately after Layer 7 exit condition is met (all tests pass).
//   Owner: [Your name / team]
//   Ticket: [JIRA/GitHub issue link when created]
//
// REFERENCES:
//   - io.jsonwebtoken.Jwts.parser() documentation: each call allocates new parser instance
//   - Spring Security filter chain: JwtAuthenticationFilter is on the hot path for
//     every authenticated request; micro-optimizations here compound at scale
//   - Session analysis: 2026-05-28, Layer 4 Block 5 security review

public interface TokenService {

    record GeneratedRefreshToken(String token, Instant expiresAt){}

    String generateAccessToken(Long userId, String role);

    GeneratedRefreshToken generateRefreshToken(Long userId);

    Long extractUserId(String token);

    String extractRole(String token);

    boolean isTokenValid(String token);

    Duration remainingTtl(String token);

}
