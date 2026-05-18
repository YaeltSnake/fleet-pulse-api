package com.fleetpulse.api.domain.model;


public class User {

    private Long id;
    private String username;
    private String passwordHash;
    private Role role;
    private boolean active;

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
