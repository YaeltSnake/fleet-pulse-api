package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.RefreshTokenEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenEntity, Long> {

    Optional<RefreshTokenEntity> findByToken(String token);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.token = :token")
    void revokeByToken(String token);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
    void revokeAllByUserId(Long userId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM RefreshTokenEntity r WHERE r.expiresAt < :now")
    void deleteAllExpired(Instant now);

}
