package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.application.port.out.RefreshTokenRepository;
import com.fleetpulse.api.domain.model.RefreshToken;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Component
public class RefreshTokenJpaAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenJpaAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Optional<RefreshToken> findByToken(String token) {
        return jpaRepository.findByToken(token).map(this::toDomain);
    }

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenEntity saved = jpaRepository.save(toEntity(token));
        return toDomain(saved);
    }

    @Override
    public void revokeByToken(String token) {
        jpaRepository.revokeByToken(token);
    }

    @Override
    public void revokeAllByUserId(Long userId) {
        jpaRepository.revokeAllByUserId(userId);
    }

    @Override
    public void deleteAllExpired() {
        jpaRepository.deleteAllExpired(Instant.now());
    }

    private RefreshToken toDomain(RefreshTokenEntity entity){
        return new RefreshToken(
                entity.getToken(),
                entity.getUserId(),
                entity.getExpiresAt(),
                entity.isRevoked()
        );
    }

    private RefreshTokenEntity toEntity(RefreshToken domain){
        return RefreshTokenEntity.builder()
                .id(null)
                .token(domain.getToken())
                .userId(domain.getUserId())
                .expiresAt(domain.getExpiresAt())
                .revoked(domain.isRevoked())
                .build();
    }
}
