package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.Unit;

import java.util.List;
import java.util.Optional;

public interface UnitRepository {
    Unit save(Unit unit);
    Optional<Unit> findByNumUnidad(String numUnidad);
    List<Unit> findAll();
    void deactivateByNumUnidad(String numUnidad);
    boolean existsByNumUnidad(String numUnidad);
}
