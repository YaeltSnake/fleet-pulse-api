package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.GpsReading;

public interface TestProviderUseCase {
    GpsReading testProvider(String numUnidad);
}
