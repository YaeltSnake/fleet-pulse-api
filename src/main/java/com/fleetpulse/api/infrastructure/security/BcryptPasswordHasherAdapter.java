package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.PasswordHasher;
import org.springframework.stereotype.Component;

@Component
public class BcryptPasswordHasherAdapter implements PasswordHasher {

    private final org.springframework.security.crypto.password.PasswordEncoder delegate;

    public BcryptPasswordHasherAdapter(
            org.springframework.security.crypto.password.PasswordEncoder delegate) {
        this.delegate = delegate;
    }

    @Override
    public String encode(String rawPassword) {
        return delegate.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return delegate.matches(rawPassword, encodedPassword);
    }

}
