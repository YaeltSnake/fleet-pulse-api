package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fleetpulse.api.domain.model.CoordinateMode;
import com.fleetpulse.api.domain.model.Unit;

import java.time.LocalTime;

public record UnitResponse(
        String numUnidad,
        boolean horarioFijo,
        LocalTime horaInicio,
        LocalTime horaFin,
        boolean active,
        boolean roundActive,
        CoordinateMode currentCoordinateMode
) {
    public static UnitResponse from(Unit unit, boolean roundActive, CoordinateMode currentMode) {
        return new UnitResponse(
                unit.getNumUnidad(),
                unit.isHorarioFijo(),
                unit.getHoraInicio(),
                unit.getHoraFin(),
                unit.isActive(),
                roundActive,
                currentMode
        );
    }
}
