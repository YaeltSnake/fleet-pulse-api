package com.fleetpulse.api.infrastructure.adapter.in.web.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TraccarClientPushRequest(
        @JsonProperty("device_id") String deviceId,
        LocationData location
) {
    public record LocationData(CoordsData coords) {}

    public record CoordsData(
            double latitude,
            double longitude,
            double accuracy,
            double altitude,
            double speed,
            double heading
    ) {}
}
