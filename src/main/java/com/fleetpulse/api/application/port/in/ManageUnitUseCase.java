package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.Unit;

import java.util.List;
import java.util.Optional;

public interface ManageUnitUseCase {
    List<Unit> listAllUnits();
    Optional<Unit> findByNumUnidad(String numUnidad);
    Unit activateUnit(String numUnidad);
    Unit deactivateUnit(String numUnidad);
}
