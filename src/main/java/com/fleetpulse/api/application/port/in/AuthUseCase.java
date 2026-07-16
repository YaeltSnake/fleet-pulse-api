package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.application.service.command.LoginCommand;

import java.time.Instant;

public interface AuthUseCase {

    AuthResult login(LoginCommand command);
    AuthResult refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
    record AuthResult(String accessToken, String refreshToken, Instant refreshTokenExpiresAt){}
}
