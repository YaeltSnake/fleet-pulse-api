package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.PulseLog;
import com.fleetpulse.api.domain.model.PulseLogStatus;

import java.time.ZonedDateTime;
import java.util.List;

public interface PulseLogRepository {

    void save(PulseLog pulseLog);

    List<PulseLog> findByFilters(
            String numUnidad,
            PulseLogStatus status,
            ZonedDateTime from,
            ZonedDateTime to,
            int page,
            int size
    );

    long countByFilters(
            String numUnidad,
            PulseLogStatus status,
            ZonedDateTime from,
            ZonedDateTime to
    );
}
