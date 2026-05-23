package com.fleetpulse.api.application.port.in;

public interface AuthUseCase {

    AuthResult login(String username, String password);
    AuthResult refresh(String refreshToken);
    void logout(String accessToken, String refreshToken);
    record AuthResult(String accessToken, String refreshToken){}
}
