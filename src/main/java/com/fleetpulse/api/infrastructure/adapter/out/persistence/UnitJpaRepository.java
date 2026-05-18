package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

interface UnitJpaRepository extends JpaRepository<UnitEntity, Long> {
    Optional<UnitEntity> findByNumUnidad(String numUnidad);
    boolean existsByNumUnidad(String numUnidad);
}
