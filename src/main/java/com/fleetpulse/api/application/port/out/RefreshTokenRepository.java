package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByToken(String token);
    RefreshToken save(RefreshToken token);
    void revokeByToken(String token);
    void revokeAllByUserId(Long userId);
    void deleteAllExpired();

}
