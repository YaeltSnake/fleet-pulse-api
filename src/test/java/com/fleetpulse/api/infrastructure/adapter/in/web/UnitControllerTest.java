package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fleetpulse.api.application.port.in.ConfigureScheduleUseCase;
import com.fleetpulse.api.application.port.in.ManageRoundUseCase;
import com.fleetpulse.api.application.port.in.ManageUnitUseCase;
import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnitControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired TokenService tokenService;

    @MockitoBean ManageUnitUseCase manageUnitUseCase;
    @MockitoBean ConfigureScheduleUseCase configureScheduleUseCase;
    @MockitoBean ManageRoundUseCase manageRoundUseCase;
    @MockitoBean PulseLogRepository pulseLogRepository;
    @MockitoBean TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        when(tokenBlacklist.isBlacklisted(any())).thenReturn(false);
        // manageRoundUseCase.isRoundActive() returns false by default (Mockito default for boolean)
        // manageRoundUseCase.getRoundCoordinateMode() returns null by default
        // pulseLogRepository.findLatestSentAt() returns Optional.empty(), findLatestSentAtForAllUnits()
        // returns an empty Map by default (Mockito RETURNS_DEFAULTS) — lastPulseAt is null unless a
        // test stubs it explicitly
    }

    // ── GET /api/units ────────────────────────────────────────────────────────

    // 9.9.1
    @Test
    void listUnits_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/units"))
                .andExpect(status().isUnauthorized());
    }

    // 9.9.2
    @Test
    void listUnits_withUserRole_returns200WithRoundActiveFlag() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");
        when(manageUnitUseCase.listAllUnits()).thenReturn(List.of(activeUnit("Peugeot")));

        mockMvc.perform(get("/api/units")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$[0].roundActive").value(false))
                .andExpect(jsonPath("$[0].lastPulseAt").value(nullValue()));
    }

    // 9.9.2b — gap 2.1: a unit with pulse history gets its lastPulseAt populated in the list response
    @Test
    void listUnits_withPulseHistory_returnsLastPulseAt() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");
        ZonedDateTime lastPulse = ZonedDateTime.of(2026, 7, 15, 10, 30, 0, 0, ZoneId.of("America/Mexico_City"));
        when(manageUnitUseCase.listAllUnits()).thenReturn(List.of(activeUnit("Peugeot")));
        when(pulseLogRepository.findLatestSentAtForAllUnits())
                .thenReturn(Map.of("Peugeot", lastPulse));

        mockMvc.perform(get("/api/units")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastPulseAt").exists())
                .andExpect(jsonPath("$[0].lastPulseAt").value(org.hamcrest.Matchers.not(nullValue())));
    }

    // ── GET /api/units/{numUnidad} ────────────────────────────────────────────

    // 9.9.3
    @Test
    void getUnit_withExistingUnit_returns200WithUnitDetails() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(manageUnitUseCase.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));

        mockMvc.perform(get("/api/units/Peugeot")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.roundActive").value(false))
                .andExpect(jsonPath("$.lastPulseAt").value(nullValue()));
    }

    // 9.9.3b — gap 2.1: getUnit() looks up the single-unit lastPulseAt, not the bulk map
    @Test
    void getUnit_withPulseHistory_returnsLastPulseAt() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        ZonedDateTime lastPulse = ZonedDateTime.of(2026, 7, 15, 10, 30, 0, 0, ZoneId.of("America/Mexico_City"));
        when(manageUnitUseCase.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));
        when(pulseLogRepository.findLatestSentAt("Peugeot")).thenReturn(Optional.of(lastPulse));

        mockMvc.perform(get("/api/units/Peugeot")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastPulseAt").exists())
                .andExpect(jsonPath("$.lastPulseAt").value(org.hamcrest.Matchers.not(nullValue())));
    }

    // 9.9.4
    @Test
    void getUnit_withUnknownUnit_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(manageUnitUseCase.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/units/Ghost")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/errors/unit-not-found")));
    }

    // ── PUT /api/units/{numUnidad}/activate ───────────────────────────────────

    // 9.9.5
    @Test
    void activateUnit_withAdminRole_returns200() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(manageUnitUseCase.activateUnit("Peugeot")).thenReturn(activeUnit("Peugeot"));

        mockMvc.perform(put("/api/units/Peugeot/activate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$.active").value(true));
    }

    // 9.9.6 — extra case: SecurityConfig blocks USER at filter level → 403 (Spring Security handler)
    @Test
    void activateUnit_withUserRole_returns403() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(put("/api/units/Peugeot/activate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/units/{numUnidad}/deactivate ─────────────────────────────────

    // 9.9.7
    @Test
    void deactivateUnit_withAdminRole_returns200() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        Unit deactivated = new Unit("Peugeot", false, LocalTime.MIN, LocalTime.MAX, null, false);
        when(manageUnitUseCase.deactivateUnit("Peugeot")).thenReturn(deactivated);

        mockMvc.perform(put("/api/units/Peugeot/deactivate")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$.active").value(false));
    }

    // ── PUT /api/units/{numUnidad}/schedule ───────────────────────────────────

    // 9.9.8
    @Test
    void updateSchedule_withAdminRole_returns200() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        Unit before   = activeUnit("Peugeot");
        Unit updated  = new Unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null, true);
        when(manageUnitUseCase.findByNumUnidad("Peugeot")).thenReturn(Optional.of(before));
        when(configureScheduleUseCase.updateSchedule(eq("Peugeot"), anyBoolean(), any(), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/units/Peugeot/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleBody(true, "09:00", "17:00")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numUnidad").value("Peugeot"));
    }

    // 9.9.9
    @Test
    void updateSchedule_withUserRole_withNonFixedSchedule_returns200() throws Exception {
        String token   = tokenService.generateAccessToken(2L, "USER");
        Unit nonFixed  = activeUnit("Peugeot");  // horarioFijo=false → user allowed
        Unit updated   = new Unit("Peugeot", false, LocalTime.of(9, 0), LocalTime.of(17, 0), null, true);
        when(manageUnitUseCase.findByNumUnidad("Peugeot")).thenReturn(Optional.of(nonFixed));
        when(configureScheduleUseCase.updateSchedule(eq("Peugeot"), anyBoolean(), any(), any()))
                .thenReturn(updated);

        mockMvc.perform(put("/api/units/Peugeot/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleBody(false, "09:00", "17:00")))
                .andExpect(status().isOk());
    }

    // 9.9.10
    @Test
    void updateSchedule_withUserRole_withFixedSchedule_returns403() throws Exception {
        String token  = tokenService.generateAccessToken(2L, "USER");
        Unit fixed    = new Unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null, true);
        when(manageUnitUseCase.findByNumUnidad("Peugeot")).thenReturn(Optional.of(fixed));

        mockMvc.perform(put("/api/units/Peugeot/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(scheduleBody(false, "10:00", "16:00")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value(endsWith("/errors/forbidden")));
    }

    // 9.9.11
    @Test
    void updateSchedule_withMissingHorarioFijo_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        String body  = objectMapper.writeValueAsString(Map.of("horaInicio", "09:00"));

        mockMvc.perform(put("/api/units/Peugeot/schedule")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Unit activeUnit(String numUnidad) {
        return new Unit(numUnidad, false, LocalTime.MIN, LocalTime.MAX, null, true);
    }

    private String scheduleBody(boolean horarioFijo, String horaInicio, String horaFin) throws Exception {
        return objectMapper.writeValueAsString(
                Map.of("horarioFijo", horarioFijo, "horaInicio", horaInicio, "horaFin", horaFin));
    }
}
