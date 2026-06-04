package com.fleetpulse.api.application.service.command;

public record LoginCommand(
        String username,

        String password
) {
}
