package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.Role;

public record UpdateUserCommand(Long id, String username, String rawPassword, Role role, boolean active) {}
