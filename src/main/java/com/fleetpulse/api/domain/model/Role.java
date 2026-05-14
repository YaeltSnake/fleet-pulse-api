package com.fleetpulse.api.domain.model;

// Phase 3 note: Spring Security will use hasAuthority() with the role name as-is (e.g., hasAuthority("ADMIN")).
// Do NOT add ROLE_ prefix to these values. The ROLE_ prefix convention only applies when using hasRole(),
// which we are intentionally avoiding to keep the mapping explicit and framework-transparent.
public enum Role {
    ADMIN,
    USER
}
