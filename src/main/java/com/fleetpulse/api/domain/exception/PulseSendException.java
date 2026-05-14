package com.fleetpulse.api.domain.exception;

public class PulseSendException extends RuntimeException {
    public PulseSendException(String message) {
        super(message);
    }

    public PulseSendException(String message, Throwable cause) {
        super(message, cause);
    }
}
