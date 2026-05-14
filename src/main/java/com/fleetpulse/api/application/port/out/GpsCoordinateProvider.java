package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.model.GpsReading;

public interface GpsCoordinateProvider {
    GpsReading getCoordinates(String numUnidad) throws GpsProviderUnavailableException;
    boolean isAvailable(String numUnidad);
}
