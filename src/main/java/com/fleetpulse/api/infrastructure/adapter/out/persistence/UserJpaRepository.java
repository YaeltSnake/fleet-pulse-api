package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

interface UserJpaRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
    boolean existsByUsername(String username);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserEntity u SET u.active = false WHERE u.id = :id")
    void deactivateById(Long id);


    long countByActiveTrue();

    boolean existsByRole(Role role);
}
