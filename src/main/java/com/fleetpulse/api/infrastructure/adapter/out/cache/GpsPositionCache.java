package com.fleetpulse.api.infrastructure.adapter.out.cache;

import com.fleetpulse.api.domain.model.GpsReading;

import java.time.Clock;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class GpsPositionCache {

    public record CachedReading(GpsReading reading, Instant receivedAt) {}

    private final ConcurrentHashMap<String, CachedReading> cache = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long maxAgeSeconds;

    public GpsPositionCache(Clock clock, long maxAgeSeconds) {
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.maxAgeSeconds = maxAgeSeconds;
    }

    public void store(String numUnidad, GpsReading reading) {
        cache.put(numUnidad, new CachedReading(reading, Instant.now(clock)));
    }

    public Optional<CachedReading> findLatest(String numUnidad) {
        return Optional.ofNullable(cache.get(numUnidad));
    }

    public boolean isStale(CachedReading cached) {
        return cached.receivedAt().isBefore(Instant.now(clock).minusSeconds(maxAgeSeconds));
    }
}
