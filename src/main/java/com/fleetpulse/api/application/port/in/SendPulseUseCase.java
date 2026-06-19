package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.GpsReading;

public interface SendPulseUseCase {

    /**
     * Global scheduler path: window check + provider lookup + send.
     * Silently skips inactive units or units outside their active window.
     */
    void sendPulse(String numUnidad);

    /**
     * Force / round-tick path: no window check, no provider call.
     * Caller supplies the GpsReading. Throws UnitNotActiveException on inactive unit — never silent skip.
     */
    void dispatch(String numUnidad, GpsReading gpsReading);
}
