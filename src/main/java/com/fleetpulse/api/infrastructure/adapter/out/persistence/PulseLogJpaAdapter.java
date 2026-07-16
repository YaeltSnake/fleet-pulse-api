package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.domain.model.PulseLog;
import com.fleetpulse.api.domain.model.PulseLogStatus;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.PulseLogEntity;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class PulseLogJpaAdapter implements PulseLogRepository {

    private static final ZoneId FLEET_TIMEZONE = ZoneId.of("America/Mexico_City");

    private final PulseLogJpaRepository jpaRepository;

    public PulseLogJpaAdapter(PulseLogJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(PulseLog pulseLog) {
        jpaRepository.save(toEntity(pulseLog));
    }

    @Override
    public List<PulseLog> findByFilters(String numUnidad, PulseLogStatus status,
                                        ZonedDateTime from, ZonedDateTime to,
                                        int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by("sentAt").descending());
        return jpaRepository
                .findByFilters(numUnidad, status, toLocal(from), toLocal(to), pageable)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public long countByFilters(String numUnidad, PulseLogStatus status,
                               ZonedDateTime from, ZonedDateTime to) {
        return jpaRepository.countByFilters(numUnidad, status, toLocal(from), toLocal(to));
    }

    @Override
    public Optional<ZonedDateTime> findLatestSentAt(String numUnidad) {
        return Optional.ofNullable(jpaRepository.findLatestSentAt(numUnidad))
                .map(sentAt -> sentAt.atZone(FLEET_TIMEZONE));
    }

    @Override
    public Map<String, ZonedDateTime> findLatestSentAtForAllUnits() {
        return jpaRepository.findLatestSentAtGroupedByUnit().stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> ((LocalDateTime) row[1]).atZone(FLEET_TIMEZONE)
                ));
    }

    @Override
    public Map<PulseLogStatus, Long> countGroupedByStatus(ZonedDateTime from, ZonedDateTime to) {
        return jpaRepository.countGroupedByStatus(toLocal(from), toLocal(to)).stream()
                .collect(Collectors.toMap(
                        row -> (PulseLogStatus) row[0],
                        row -> (Long) row[1]
                ));
    }

    private PulseLog toDomain(PulseLogEntity entity) {
        ZonedDateTime sentAt = entity.getSentAt().atZone(FLEET_TIMEZONE);
        return new PulseLog(
                entity.getNumUnidad(),
                entity.getStatus(),
                entity.getLat(),
                entity.getLon(),
                entity.getProvider(),
                entity.getTrackingNumber(),
                sentAt,
                entity.getErrorMessage()
        );
    }

    private PulseLogEntity toEntity(PulseLog domain) {
        LocalDateTime sentAt = domain.sentAt().withZoneSameInstant(FLEET_TIMEZONE).toLocalDateTime();
        return PulseLogEntity.builder()
                .id(null)
                .numUnidad(domain.numUnidad())
                .status(domain.status())
                .lat(domain.lat())
                .lon(domain.lon())
                .provider(domain.provider())
                .trackingNumber(domain.trackingNumber())
                .sentAt(sentAt)
                .errorMessage(domain.errorMessage())
                .build();
    }

    private LocalDateTime toLocal(ZonedDateTime zdt) {
        return zdt != null ? zdt.withZoneSameInstant(FLEET_TIMEZONE).toLocalDateTime() : null;
    }
}
