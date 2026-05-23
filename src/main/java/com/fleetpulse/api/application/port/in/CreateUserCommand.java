package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.Role;

public record CreateUserCommand(String username, String rawPassword, Role role) {}
