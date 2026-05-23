package com.fleetpulse.api.domain.exception;

public class UserNotActiveException extends RuntimeException {
    public UserNotActiveException(String username) {
        super("User not active :" + username);
    }
}
