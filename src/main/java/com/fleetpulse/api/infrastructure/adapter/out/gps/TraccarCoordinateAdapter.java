package com.fleetpulse.api.infrastructure.adapter.out.gps;

import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.infrastructure.adapter.out.cache.GpsPositionCache;

import java.util.Objects;

public class TraccarCoordinateAdapter implements GpsCoordinateProvider {

    private final GpsPositionCache cache;

    public TraccarCoordinateAdapter(GpsPositionCache cache) {
        this.cache = Objects.requireNonNull(cache, "cache must not be null");
    }

    @Override
    public boolean isAvailable(String numUnidad) {
        return cache.findLatest(numUnidad)
                .map(cached -> !cache.isStale(cached))
                .orElse(false);
    }

    @Override
    public GpsReading getCoordinates(String numUnidad) throws GpsProviderUnavailableException {
        GpsPositionCache.CachedReading cached = cache.findLatest(numUnidad)
                .orElseThrow(() -> new GpsProviderUnavailableException(
                        "No GPS data received yet for unit: " + numUnidad));

        if (cache.isStale(cached)) {
            throw new GpsProviderUnavailableException(
                    "GPS data is stale (> max age) for unit: " + numUnidad);
        }

        return cached.reading();
    }
}
