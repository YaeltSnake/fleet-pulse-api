package com.fleetpulse.api.infrastructure.security;

import com.fleetpulse.api.application.port.out.PasswordHasher;

public class BcryptPasswordHasherAdapter implements PasswordHasher {

    // FIXME-IMPORT-2: PasswordEncoder used as fully qualified name in field and constructor.
    // Move to import header for consistency. Cosmetic — no functional impact.
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    public BcryptPasswordHasherAdapter(
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public String encode(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

}
