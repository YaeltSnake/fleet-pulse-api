package com.fleetpulse.api.domain.model;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Objects;

public record PulseLog(
        String numUnidad,
        PulseLogStatus status,
        BigDecimal lat,
        BigDecimal lon,
        String provider,
        String trackingNumber,
        ZonedDateTime sentAt,
        String errorMessage
) {
    public PulseLog {
        Objects.requireNonNull(numUnidad, "numUnidad must not be null");
        Objects.requireNonNull(status,    "status must not be null");
        Objects.requireNonNull(sentAt,    "sentAt must not be null");
    }

    public static PulseLog sent(String numUnidad, GpsReading reading,
                                String trackingNumber, ZonedDateTime sentAt) {
        return new PulseLog(numUnidad, PulseLogStatus.SENT,
                reading.getLatitud(), reading.getLongitud(),
                reading.getProviderType().name(), trackingNumber, sentAt, null);
    }

    public static PulseLog skipped(String numUnidad, PulseLogStatus status, ZonedDateTime sentAt) {
        return new PulseLog(numUnidad, status, null, null, null, null, sentAt, null);
    }

    public static PulseLog rejected(String numUnidad, GpsReading reading,
                                    String trackingNumber, ZonedDateTime sentAt, String errorMessage) {
        return new PulseLog(numUnidad, PulseLogStatus.REJECTED,
                reading.getLatitud(), reading.getLongitud(),
                reading.getProviderType().name(), trackingNumber, sentAt, errorMessage);
    }

    public static PulseLog error(String numUnidad, GpsReading reading,
                                 String trackingNumber, ZonedDateTime sentAt, String errorMessage) {
        return new PulseLog(numUnidad, PulseLogStatus.ERROR,
                reading.getLatitud(), reading.getLongitud(),
                reading.getProviderType().name(), trackingNumber, sentAt, errorMessage);
    }
}
