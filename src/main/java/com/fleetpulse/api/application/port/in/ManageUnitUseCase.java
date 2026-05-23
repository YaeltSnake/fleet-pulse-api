package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.Unit;

import java.util.List;

public interface ManageUnitUseCase {
    Unit createUnit(Unit unit);
    Unit updateUnit(Unit unit);
    void deactivateUnit(String numUnidad);
    Unit findUnit(String numUnidad);
    List<Unit> findAllUnits();
}
