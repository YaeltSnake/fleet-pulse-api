package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.Unit;

import java.time.LocalTime;

public interface ConfigureScheduleUseCase {
    Unit updateSchedule(String numUnidad, LocalTime horaInicio, LocalTime horaFin, Role requesterRole);
}
