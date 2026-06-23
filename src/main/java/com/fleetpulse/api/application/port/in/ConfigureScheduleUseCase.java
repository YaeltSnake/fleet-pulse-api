package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.Unit;

import java.time.LocalTime;

public interface ConfigureScheduleUseCase {

    /**
     * Updates the dispatch schedule for the given unit.
     *
     * <p>When {@code horarioFijo} is {@code false} and both hour params are {@code null},
     * the schedule resets to the sentinel window {@code (LocalTime.MIN, LocalTime.MAX)}
     * meaning the unit runs all day (ADR-013).
     *
     * <p>When {@code horarioFijo} is {@code false} and hours are provided,
     * {@code horaInicio} must be strictly before {@code horaFin}, otherwise
     * {@link com.fleetpulse.api.domain.exception.ScheduleConflictException} is thrown.
     */
    Unit updateSchedule(String numUnidad, boolean horarioFijo,
                        LocalTime horaInicio, LocalTime horaFin);
}
