package com.fleetpulse.api.infrastructure.adapter.out.redis;

import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.domain.exception.ExternalServiceUnavailableException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RedisTokenBlacklistAdapter implements TokenBlacklist {

    private final StringRedisTemplate redisTemplate;

    public RedisTokenBlacklistAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // FIXME-[Rule14] ExternalServiceUnavailableException lives in domain.exception but is thrown from
    // infrastructure. Acceptable here because it crosses to JwtAuthenticationFilter (also infra),
    // not the application layer. Revisit if a dedicated infrastructure.exception package is introduced.
    @Override
    public void blacklist(String token, Duration remainingTtl) {
        try {
            redisTemplate.opsForValue().set(token, "1", remainingTtl);
            // FIXME-IMPORT-1: DataAccessException used as fully qualified name in catch blocks.
            // Move to import header for consistency. Cosmetic — no functional impact.
        } catch (org.springframework.dao.DataAccessException e) {
            throw new ExternalServiceUnavailableException("Redis unavailable during token validation: ", e);
        }
    }

    // FIXME-[Rule14] same as above — fail-closed signal for JwtAuthenticationFilter.
    @Override
    public boolean isBlacklisted(String token) {
        try {
            Boolean hasKey = redisTemplate.hasKey(token);

            return Boolean.TRUE.equals(hasKey);
        } catch (org.springframework.dao.DataAccessException e) {
            throw new ExternalServiceUnavailableException("Redis unavailable during token validation: ", e);
        }
    }
}
