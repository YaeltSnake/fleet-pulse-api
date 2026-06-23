package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UnitEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

interface UnitJpaRepository extends JpaRepository<UnitEntity, Long> {

    Optional<UnitEntity> findByNumUnidad(String numUnidad);

    boolean existsByNumUnidad(String numUnidad);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UnitEntity u SET u.active = false WHERE u.numUnidad = :numUnidad")
    void deactivateByNumUnidad(String numUnidad);

    @Query("SELECT u.numUnidad FROM UnitEntity u WHERE u.active = true")
    List<String> findAllActiveNumUnidades();

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UnitEntity u SET u.active = :active WHERE u.numUnidad = :numUnidad")
    void setActive(String numUnidad, boolean active);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UnitEntity u SET u.horarioFijo = :horarioFijo WHERE u.numUnidad = :numUnidad")
    void setHorarioFijo(String numUnidad, boolean horarioFijo);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UnitEntity u SET u.horaInicio = :horaInicio, u.horaFin = :horaFin WHERE u.numUnidad = :numUnidad")
    void updateSchedule(String numUnidad, LocalTime horaInicio, LocalTime horaFin);
}
