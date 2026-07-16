package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.model.PulseLog;
import com.fleetpulse.api.domain.model.PulseLogStatus;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    Optional<ZonedDateTime> findLatestSentAt(String numUnidad);

    Map<String, ZonedDateTime> findLatestSentAtForAllUnits();

    Map<PulseLogStatus, Long> countGroupedByStatus(ZonedDateTime from, ZonedDateTime to);
}
