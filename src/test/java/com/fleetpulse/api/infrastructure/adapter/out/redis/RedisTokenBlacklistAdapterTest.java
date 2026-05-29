package com.fleetpulse.api.infrastructure.adapter.out.redis;

import com.fleetpulse.api.domain.exception.ExternalServiceUnavailableException;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.redis.DataRedisTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataRedisTest
@Testcontainers
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RedisTokenBlacklistAdapterTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        public RedisTokenBlacklistAdapter redisTokenBlacklistAdapter(StringRedisTemplate template) {
            return new RedisTokenBlacklistAdapter(template);
        }
    }

    @Autowired
    private RedisTokenBlacklistAdapter adapter;

    @Test
    @Order(1)
    void blacklist_thenIsBlacklisted_returnsTrue() {
        String token = "access-token-" + UUID.randomUUID();

        adapter.blacklist(token, Duration.ofMinutes(15));

        assertThat(adapter.isBlacklisted(token)).isTrue();
    }

    @Test
    @Order(2)
    void isBlacklisted_afterTtlExpiry_returnsFalse() throws InterruptedException {
        String token = "short-lived-" + UUID.randomUUID();

        adapter.blacklist(token, Duration.ofMillis(500));
        assertThat(adapter.isBlacklisted(token)).isTrue();

        Thread.sleep(800);

        assertThat(adapter.isBlacklisted(token)).isFalse();
    }

    @Test
    @Order(Integer.MAX_VALUE)
    void isBlacklisted_whenRedisDown_throwsExternalServiceUnavailableException() {
        redis.stop();

        assertThatThrownBy(() -> adapter.isBlacklisted("any-token"))
                .isInstanceOf(ExternalServiceUnavailableException.class);
    }
}