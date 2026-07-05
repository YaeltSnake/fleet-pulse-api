package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.domain.model.PulseLogStatus;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.PulseLogEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface PulseLogJpaRepository extends JpaRepository<PulseLogEntity, Long> {

    @Query("""
            SELECT e FROM PulseLogEntity e
            WHERE (:numUnidad IS NULL OR e.numUnidad = :numUnidad)
              AND (:status    IS NULL OR e.status    = :status)
              AND (:from      IS NULL OR e.sentAt   >= :from)
              AND (:to        IS NULL OR e.sentAt   <= :to)
            ORDER BY e.sentAt DESC
            """)
    Page<PulseLogEntity> findByFilters(
            @Param("numUnidad") String numUnidad,
            @Param("status")    PulseLogStatus status,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(e) FROM PulseLogEntity e
            WHERE (:numUnidad IS NULL OR e.numUnidad = :numUnidad)
              AND (:status    IS NULL OR e.status    = :status)
              AND (:from      IS NULL OR e.sentAt   >= :from)
              AND (:to        IS NULL OR e.sentAt   <= :to)
            """)
    long countByFilters(
            @Param("numUnidad") String numUnidad,
            @Param("status")    PulseLogStatus status,
            @Param("from")      LocalDateTime from,
            @Param("to")        LocalDateTime to
    );
}
