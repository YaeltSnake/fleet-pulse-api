package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.PulseSendException;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PulseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;

    @MockitoBean SendPulseUseCase sendPulseUseCase;
    @MockitoBean GpsCoordinateProvider gpsProvider;
    @MockitoBean TokenBlacklist tokenBlacklist;

    @BeforeEach
    void allowAllTokens() {
        when(tokenBlacklist.isBlacklisted(any())).thenReturn(false);
    }

    // ── 9.7.1 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isUnauthorized());
    }

    // ── 9.7.2 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withUserRole_withValidManualCoords_returns204() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isNoContent());
    }

    // ── 9.7.3 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withAdminRole_withValidManualCoords_returns204() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isNoContent());
    }

    // ── 9.7.4 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withAutomaticMode_andAvailableGps_returns204() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        String body = objectMapper.writeValueAsString(Map.of("coordinateMode", "AUTOMATIC"));
        GpsReading traccarReading = new GpsReading("Peugeot",
                new BigDecimal("19.4326"), new BigDecimal("-99.1332"),
                ZonedDateTime.now(ZoneId.of("America/Mexico_City")), ProviderType.TRACCAR);
        when(gpsProvider.getCoordinates("Peugeot")).thenReturn(traccarReading);

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());
    }

    // ── 9.7.4b ────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withAutomaticMode_andGpsUnavailable_returns503() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        String body = objectMapper.writeValueAsString(Map.of("coordinateMode", "AUTOMATIC"));
        when(gpsProvider.getCoordinates("Peugeot"))
                .thenThrow(new GpsProviderUnavailableException("GPS data is stale for unit: Peugeot"));

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/service-unavailable")));
    }

    // ── 9.7.5 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withManualModeAndMissingLat_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        String body = objectMapper.writeValueAsString(
                Map.of("coordinateMode", "MANUAL", "lon", -99.1332));

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/invalid-coordinates")));
    }

    // ── 9.7.6 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withUnitNotFound_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new UnitNotFoundException("Peugeot")).when(sendPulseUseCase).dispatch(any(), any());

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/unit-not-found")));
    }

    // ── 9.7.7 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withUnitInactive_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new UnitNotActiveException("Peugeot")).when(sendPulseUseCase).dispatch(any(), any());

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/unit-not-active")));
    }

    // ── 9.7.8 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withPulseSendException_returns502() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new PulseSendException("QSolutions rejected")).when(sendPulseUseCase).dispatch(any(), any());

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isBadGateway())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/soap-rejected")));
    }

    // ── 9.7.9 ─────────────────────────────────────────────────────────────────

    @Test
    void forceDispatch_withGpsProviderUnavailableException_returns503() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new GpsProviderUnavailableException("SOAP transport failure"))
                .when(sendPulseUseCase).dispatch(any(), any());

        mockMvc.perform(post("/api/units/Peugeot/pulse/force")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/service-unavailable")));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String manualBody() throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("coordinateMode", "MANUAL", "lat", 19.4326, "lon", -99.1332));
    }
}
