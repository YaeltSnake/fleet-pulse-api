package com.fleetpulse.api.domain.model;

import java.time.Instant;

public class RefreshToken {

    private final String token;
    private final Long userId;
    private final Instant expiresAt;
    private final boolean revoked;

    public RefreshToken(String token,
                        Long userId,
                        Instant expiresAt,
                        boolean revoked) {
        this.token = token;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

}
