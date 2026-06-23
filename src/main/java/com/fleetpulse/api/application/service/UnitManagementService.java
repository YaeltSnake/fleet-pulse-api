package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.ConfigureScheduleUseCase;
import com.fleetpulse.api.application.port.in.ManageUnitUseCase;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.ScheduleConflictException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.Unit;

import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class UnitManagementService implements ManageUnitUseCase, ConfigureScheduleUseCase {

    private final UnitRepository unitRepository;

    public UnitManagementService(UnitRepository unitRepository) {
        this.unitRepository = Objects.requireNonNull(unitRepository, "unitRepository must not be null");
    }

    @Override
    public List<Unit> listAllUnits() {
        return unitRepository.findAll();
    }

    @Override
    public Optional<Unit> findByNumUnidad(String numUnidad) {
        return unitRepository.findByNumUnidad(numUnidad);
    }

    @Override
    public Unit activateUnit(String numUnidad) {
        unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));
        unitRepository.setActive(numUnidad, true);
        return unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));
    }

    @Override
    public Unit deactivateUnit(String numUnidad) {
        unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));
        unitRepository.setActive(numUnidad, false);
        return unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));
    }

    @Override
    public Unit updateSchedule(String numUnidad, boolean horarioFijo,
                               LocalTime horaInicio, LocalTime horaFin) {
        unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        if (!horarioFijo && horaInicio != null && horaFin != null) {
            if (!horaInicio.isBefore(horaFin)) {
                throw new ScheduleConflictException(numUnidad);
            }
        }

        // ADR-013: null hours → sentinel (MIN, MAX) — unit runs all day
        LocalTime resolvedInicio = (horaInicio != null) ? horaInicio : LocalTime.MIN;
        LocalTime resolvedFin    = (horaFin != null)    ? horaFin    : LocalTime.MAX;

        unitRepository.updateSchedule(numUnidad, resolvedInicio, resolvedFin);
        unitRepository.setHorarioFijo(numUnidad, horarioFijo);

        return unitRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));
    }
}
