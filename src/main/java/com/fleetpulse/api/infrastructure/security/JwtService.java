package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.TokenService;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Objects;

public class JwtService implements TokenService {

    private final SecretKey signingKey;
    private final long accessExpirySeconds;
    private final long refreshExpirySeconds;

    public JwtService( @Value("${jwt.secret}") String secret,
                       @Value("${jwt.access-expiry-seconds}") long accessExpirySeconds,
                       @Value("${jwt.refresh-expiry-seconds}") long refreshExpirySeconds){
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessExpirySeconds = accessExpirySeconds;
        this.refreshExpirySeconds = refreshExpirySeconds;

    }

    // FIXME-CLOCK: Instant.now() without injectable Clock violates 03-java-production.
    // Inject Clock via constructor for testability. Resolve before Phase 5.
    @Override
    public String generateAccessToken(Long userId, String role) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(accessExpirySeconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("role", Objects.requireNonNull(role, "role cannot be null"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(this.signingKey)
                .compact();
    }

    @Override
    public GeneratedRefreshToken generateRefreshToken(Long userId) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(refreshExpirySeconds);

        String token =  Jwts.builder()
                       .subject(userId.toString())
                       .issuedAt(Date.from(now))
                       .expiration(Date.from(expiration))
                       .signWith(this.signingKey)
                       .compact();

        return new GeneratedRefreshToken(token, expiration);
    }

    @Override
    public Long extractUserId(String token) {
        String subject = extractAllClaims(token).getSubject();
        try {
            return Long.valueOf(subject);
        }catch (NumberFormatException e){
            throw new IllegalArgumentException("Invalid userId format in token: " + subject);
        }
    }

    @Override
    public String extractRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    @Override
    public boolean isTokenValid(String token) {
        try {

            Claims claims = extractAllClaims(token);
            return !claims.getExpiration().toInstant().isBefore(Instant.now());
        }catch (ExpiredJwtException e){
            return false;
        }catch (SignatureException e){
            return false;
        }catch (MalformedJwtException |
                UnsupportedJwtException |
                IllegalArgumentException e){
            return false;
        }
    }

    @Override
    public Duration remainingTtl(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Instant expiration = claims.getExpiration().toInstant();
            Instant now = Instant.now();

            return expiration.isBefore(now)
                    ? Duration.ZERO
                    : Duration.between(now, expiration);
        }catch (ExpiredJwtException e){
            return Duration.ZERO;
        }

    }

    private Claims extractAllClaims(String token){
        return Jwts.parser()
                .verifyWith(this.signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
