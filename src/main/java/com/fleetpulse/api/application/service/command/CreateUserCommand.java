package com.fleetpulse.api.application.service.command;

import com.fleetpulse.api.domain.model.Role;

public record CreateUserCommand(String username, String rawPassword, Role role) {}
