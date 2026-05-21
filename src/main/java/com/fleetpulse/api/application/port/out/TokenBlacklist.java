package com.fleetpulse.api.application.port.out;

import java.time.Duration;

public interface TokenBlacklist {

    void blacklist(String token, Duration remainingTtl);

    boolean isBlacklisted(String token);

}
