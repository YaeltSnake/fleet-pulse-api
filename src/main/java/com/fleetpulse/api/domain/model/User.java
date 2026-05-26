package com.fleetpulse.api.domain.model;


public class User {

    // FIXME: userId should be a business identifier (UUID or stable natural key),
   // not the database surrogate key. See ADR-003. Migrate before Phase 6.
    private final Long id;
    private final String username;
    private final String passwordHash;
    private final Role role;
    private final boolean active;

    public User(Long id, String username, String passwordHash, Role role, boolean active) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public boolean isActive() {
        return active;
    }

}
