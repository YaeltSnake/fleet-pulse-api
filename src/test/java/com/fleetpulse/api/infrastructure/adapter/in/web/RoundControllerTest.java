package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.ManageRoundUseCase;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.exception.RoundAlreadyActiveException;
import com.fleetpulse.api.domain.exception.RoundNotActiveException;
import com.fleetpulse.api.domain.exception.ScheduleNotConfiguredException;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
class RoundControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;

    @MockitoBean ManageRoundUseCase manageRoundUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;

    @BeforeEach
    void allowAllTokens() {
        when(tokenBlacklist.isBlacklisted(any())).thenReturn(false);
    }

    // ── POST /api/units/{numUnidad}/round/start ───────────────────────────────

    // 9.8.1
    @Test
    void startRound_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isUnauthorized());
    }

    // 9.8.2
    @Test
    void startRound_withUserRole_withValidManualCoords_returns204() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isNoContent());
    }

    // 9.8.3
    @Test
    void startRound_withAdminRole_withValidManualCoords_returns204() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isNoContent());
    }

    // 9.8.4 — Jakarta Validation: coordinateMode is @NotNull
    @Test
    void startRound_withNullCoordinateMode_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        String body = objectMapper.writeValueAsString(Map.of("lat", 19.4326, "lon", -99.1332));

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // 9.8.5 — controller propagates InvalidCoordinateException from use case → 400
    @Test
    void startRound_withAutomaticMode_andUseCaseRejectsCoords_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new InvalidCoordinateException("Coordinates out of range"))
                .when(manageRoundUseCase).startRound(any(), any(), any(), any());

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("coordinateMode", "AUTOMATIC"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/invalid-coordinates")));
    }

    // 9.8.6
    @Test
    void startRound_withManualModeAndMissingCoords_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new InvalidCoordinateException("MANUAL mode requires lat and lon"))
                .when(manageRoundUseCase).startRound(any(), any(), any(), any());

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("coordinateMode", "MANUAL"))))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/invalid-coordinates")));
    }

    // 9.8.7
    @Test
    void startRound_withUnitNotFound_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new UnitNotFoundException("Peugeot"))
                .when(manageRoundUseCase).startRound(any(), any(), any(), any());

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/unit-not-found")));
    }

    // 9.8.8
    @Test
    void startRound_whenRoundAlreadyActive_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new RoundAlreadyActiveException("Peugeot"))
                .when(manageRoundUseCase).startRound(any(), any(), any(), any());

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/round-already-active")));
    }

    // 9.8.9
    @Test
    void startRound_withInactiveUnit_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new UnitNotActiveException("Peugeot"))
                .when(manageRoundUseCase).startRound(any(), any(), any(), any());

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/unit-not-active")));
    }

    // 9.8.10
    @Test
    void startRound_withScheduleNotConfigured_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new ScheduleNotConfiguredException("Peugeot"))
                .when(manageRoundUseCase).startRound(any(), any(), any(), any());

        mockMvc.perform(post("/api/units/Peugeot/round/start")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(manualBody()))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/schedule-not-configured")));
    }

    // ── POST /api/units/{numUnidad}/round/stop ────────────────────────────────

    // 9.8.11
    @Test
    void stopRound_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/units/Peugeot/round/stop"))
                .andExpect(status().isUnauthorized());
    }

    // 9.8.12
    @Test
    void stopRound_withActiveRound_returns204() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(post("/api/units/Peugeot/round/stop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    // 9.8.13
    @Test
    void stopRound_withNoActiveRound_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        doThrow(new RoundNotActiveException("Peugeot"))
                .when(manageRoundUseCase).stopRound(any());

        mockMvc.perform(post("/api/units/Peugeot/round/stop")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isConflict())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(org.hamcrest.Matchers.endsWith("/errors/round-not-active")));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private String manualBody() throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("coordinateMode", "MANUAL", "lat", 19.4326, "lon", -99.1332));
    }
}
