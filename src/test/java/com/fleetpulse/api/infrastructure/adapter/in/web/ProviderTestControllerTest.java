package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.TestProviderUseCase;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderTestControllerTest {

    private static final ZoneId FLEET_TZ = ZoneId.of("America/Mexico_City");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T16:00:00Z"), FLEET_TZ);

    private static final BigDecimal LAT = new BigDecimal("19.4326");
    private static final BigDecimal LON = new BigDecimal("-99.1332");

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;

    @MockitoBean TestProviderUseCase testProviderUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        when(tokenBlacklist.isBlacklisted(any())).thenReturn(false);
    }

    // ── GET /api/units/{numUnidad}/pulse/test ─────────────────────────────────

    // 9.10.1
    @Test
    void testProvider_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/units/Peugeot/pulse/test"))
                .andExpect(status().isUnauthorized());
    }

    // 9.10.2
    @Test
    void testProvider_withUserRole_returns200WithCoordinates() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");
        when(testProviderUseCase.testProvider("Peugeot")).thenReturn(validReading("Peugeot"));

        mockMvc.perform(get("/api/units/Peugeot/pulse/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$.providerType").value("MANUAL"));
    }

    // 9.10.3
    @Test
    void testProvider_withAdminRole_returns200WithCoordinates() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(testProviderUseCase.testProvider("Peugeot")).thenReturn(validReading("Peugeot"));

        mockMvc.perform(get("/api/units/Peugeot/pulse/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"));
    }

    // 9.10.4
    @Test
    void testProvider_withUnknownUnit_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(testProviderUseCase.testProvider("Ghost")).thenThrow(new UnitNotFoundException("Ghost"));

        mockMvc.perform(get("/api/units/Ghost/pulse/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/errors/unit-not-found")));
    }

    // 9.10.5
    @Test
    void testProvider_whenProviderUnavailable_returns503() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(testProviderUseCase.testProvider("Peugeot"))
                .thenThrow(new GpsProviderUnavailableException("Peugeot not in manual config"));

        mockMvc.perform(get("/api/units/Peugeot/pulse/test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/errors/service-unavailable")));
    }

    // ── POST /api/units/{numUnidad}/pulse/test-manual ─────────────────────────

    // 9.10.6
    @Test
    void testWithManualCoordinates_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/units/Peugeot/pulse/test-manual")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isUnauthorized());
    }

    // 9.10.7
    @Test
    void testWithManualCoordinates_withAdminRole_returns200WithCoordinates() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(testProviderUseCase.testProviderWithOverride("Peugeot", LAT, LON))
                .thenReturn(validReading("Peugeot"));

        mockMvc.perform(post("/api/units/Peugeot/pulse/test-manual")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$.providerType").value("MANUAL"));
    }

    // 9.10.8 — Jakarta Validation: lat is @NotNull
    @Test
    void testWithManualCoordinates_withNullLat_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        String body  = objectMapper.writeValueAsString(Map.of("lon", -99.1332));

        mockMvc.perform(post("/api/units/Peugeot/pulse/test-manual")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/errors/validation-failed")));
    }

    // 9.10.9
    @Test
    void testWithManualCoordinates_withInvalidCoordinates_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(testProviderUseCase.testProviderWithOverride(any(), any(), any()))
                .thenThrow(new InvalidCoordinateException("Latitude out of range: 91.0"));

        String body = objectMapper.writeValueAsString(Map.of("lat", 91.0, "lon", -99.1332));

        mockMvc.perform(post("/api/units/Peugeot/pulse/test-manual")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/errors/invalid-coordinates")));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private GpsReading validReading(String numUnidad) {
        return new GpsReading(numUnidad, LAT, LON,
                ZonedDateTime.now(FIXED_CLOCK), ProviderType.MANUAL);
    }

    private String manualBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of("lat", 19.4326, "lon", -99.1332));
    }
}
