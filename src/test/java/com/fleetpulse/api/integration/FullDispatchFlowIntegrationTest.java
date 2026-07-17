package com.fleetpulse.api.integration;

import com.fleetpulse.api.application.port.out.PulseSender;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.model.CoordinateMode;
import com.fleetpulse.api.domain.model.PulseLogStatus;
import com.fleetpulse.api.domain.model.Unit;
import com.fleetpulse.api.infrastructure.adapter.in.web.PulseLogController.PulseLogPageResponse;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.ForcePulseRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.LoginRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.LoginResponse;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.RefreshResponse;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.UnitResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;

/**
 * Phase 8 Layer 4 -- exercises the full stack in one continuous flow (login -> force
 * dispatch -> pulse-log confirms it -> refresh -> logout -> old token rejected), the way
 * a real frontend session actually will. Distinct from the rest of the suite, which tests
 * each controller/service in isolation with mocked collaborators.
 *
 * Only PulseSender is mocked -- it is the one real external boundary (the QSolutions SOAP
 * service) that must never be hit by an automated test. Everything else (JWT, the httpOnly
 * cookie, Redis blacklist via a real Testcontainers Redis, H2 database, JPA repositories)
 * runs for real, so this test proves the endpoints compose correctly, not just that each
 * one works alone.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class FullDispatchFlowIntegrationTest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    private static final String NUM_UNIDAD = "FlowTestUnit";
    private static final String ADMIN_USERNAME = "testadmin";
    private static final String ADMIN_PASSWORD = "testpassword123";

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private UnitRepository unitRepository;

    @MockitoBean
    private PulseSender pulseSender;

    @BeforeEach
    void setUp() {
        doNothing().when(pulseSender).send(any());
        if (!unitRepository.existsByNumUnidad(NUM_UNIDAD)) {
            unitRepository.save(new Unit(NUM_UNIDAD, false, LocalTime.MIN, LocalTime.MAX, null, true));
        }
    }

    @Test
    void loginThroughLogout_fullFlow_composesCorrectly() {
        // 1. Login -- access token in body, refresh token exclusively in an httpOnly cookie
        HttpHeaders loginHeaders = new HttpHeaders();
        loginHeaders.setContentType(MediaType.APPLICATION_JSON);
        ResponseEntity<LoginResponse> loginResponse = restTemplate.postForEntity(
                "/api/auth/login",
                new HttpEntity<>(new LoginRequest(ADMIN_USERNAME, ADMIN_PASSWORD), loginHeaders),
                LoginResponse.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String accessToken = loginResponse.getBody().accessToken();
        String refreshCookie = extractCookie(loginResponse);
        assertThat(accessToken).isNotBlank();
        assertThat(refreshCookie).isNotBlank();

        // 2. GET /api/units with the access token -- confirms the seeded unit is reachable
        ResponseEntity<UnitResponse[]> unitsResponse = restTemplate.exchange(
                "/api/units", HttpMethod.GET, authEntity(accessToken), UnitResponse[].class);

        assertThat(unitsResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(unitsResponse.getBody()).extracting(UnitResponse::numUnidad).contains(NUM_UNIDAD);

        // 3. Force dispatch (MANUAL coordinates -- never touches GpsCoordinateProvider)
        HttpHeaders forceHeaders = new HttpHeaders();
        forceHeaders.setContentType(MediaType.APPLICATION_JSON);
        forceHeaders.setBearerAuth(accessToken);
        ForcePulseRequest forceBody = new ForcePulseRequest(
                CoordinateMode.MANUAL, new BigDecimal("19.4326"), new BigDecimal("-99.1332"));

        ResponseEntity<Void> forceResponse = restTemplate.postForEntity(
                "/api/units/" + NUM_UNIDAD + "/pulse/force",
                new HttpEntity<>(forceBody, forceHeaders),
                Void.class);

        assertThat(forceResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 4. GET /api/pulse-log confirms a new SENT entry -- proves dispatch() really wrote it
        ResponseEntity<PulseLogPageResponse> pulseLogResponse = restTemplate.exchange(
                "/api/pulse-log?numUnidad=" + NUM_UNIDAD,
                HttpMethod.GET, authEntity(accessToken), PulseLogPageResponse.class);

        assertThat(pulseLogResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(pulseLogResponse.getBody().content())
                .extracting(entry -> entry.status())
                .contains(PulseLogStatus.SENT);

        // 5. Refresh using the cookie captured at login -- proves the cookie is genuinely usable
        HttpHeaders refreshHeaders = new HttpHeaders();
        refreshHeaders.add(HttpHeaders.COOKIE, "refresh_token=" + refreshCookie);
        ResponseEntity<RefreshResponse> refreshResponse = restTemplate.postForEntity(
                "/api/auth/refresh", new HttpEntity<>(null, refreshHeaders), RefreshResponse.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String newAccessToken = refreshResponse.getBody().accessToken();
        String newRefreshCookie = extractCookie(refreshResponse);
        assertThat(newAccessToken).isNotEqualTo(accessToken);
        assertThat(newRefreshCookie).isNotEqualTo(refreshCookie);

        // 6. Logout with the rotated access token + rotated cookie
        HttpHeaders logoutHeaders = new HttpHeaders();
        logoutHeaders.setBearerAuth(newAccessToken);
        logoutHeaders.add(HttpHeaders.COOKIE, "refresh_token=" + newRefreshCookie);
        ResponseEntity<Void> logoutResponse = restTemplate.exchange(
                "/api/auth/logout", HttpMethod.POST, new HttpEntity<>(null, logoutHeaders), Void.class);

        assertThat(logoutResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // 7. The just-blacklisted access token is now rejected -- real Redis, not a mock's word for it
        ResponseEntity<String> afterLogout = restTemplate.exchange(
                "/api/units", HttpMethod.GET, authEntity(newAccessToken), String.class);

        assertThat(afterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    private HttpEntity<Void> authEntity(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        return new HttpEntity<>(headers);
    }

    private String extractCookie(ResponseEntity<?> response) {
        return response.getHeaders().get(HttpHeaders.SET_COOKIE).stream()
                .filter(cookie -> cookie.startsWith("refresh_token="))
                .findFirst()
                .map(cookie -> cookie.substring("refresh_token=".length(), cookie.indexOf(';')))
                .orElseThrow(() -> new AssertionError("No refresh_token cookie in response"));
    }
}
