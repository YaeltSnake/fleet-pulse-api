package com.fleetpulse.api.domain.exception;

public class GpsProviderUnavailableException extends RuntimeException {
    public GpsProviderUnavailableException(String message) {
        super(message);
    }

    public GpsProviderUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
