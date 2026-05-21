package com.fleetpulse.api.domain.model;

import java.time.Instant;

public class RefreshToken {

    private String token;
    private String username;
    private Instant expiresAt;
    private boolean revoked;

    public RefreshToken(String token,
                        String username,
                        Instant expiresAt,
                        boolean revoked) {
        this.token = token;
        this.username = username;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }

}
