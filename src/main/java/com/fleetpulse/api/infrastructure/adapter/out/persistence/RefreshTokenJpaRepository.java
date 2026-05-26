package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Modifying
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.token = :token")
    void revokeByToken(String token);

    @Modifying
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    void deleteAllExpired(Instant now);

}
