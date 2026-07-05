package com.fleetpulse.api.domain.model;

public enum PulseLogStatus {
    SENT,
    SKIPPED_INACTIVE,
    SKIPPED_OUT_OF_WINDOW,
    SKIPPED_STALE,
    SKIPPED_NO_COORDS,
    REJECTED,
    ERROR
}
