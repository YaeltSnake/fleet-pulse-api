package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.Unit;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

public interface UnitRepository {
    Unit save(Unit unit);
    Optional<Unit> findByNumUnidad(String numUnidad);
    List<Unit> findAll();
    void deactivateByNumUnidad(String numUnidad);
    boolean existsByNumUnidad(String numUnidad);
    List<String> findAllActiveNumUnidades();

    void setActive(String numUnidad, boolean active);
    void setHorarioFijo(String numUnidad, boolean horarioFijo);
    void updateSchedule(String numUnidad, LocalTime horaInicio, LocalTime horaFin);
}
