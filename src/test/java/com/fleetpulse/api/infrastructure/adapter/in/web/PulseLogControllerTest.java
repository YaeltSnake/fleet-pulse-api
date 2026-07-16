package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.domain.model.PulseLog;
import com.fleetpulse.api.domain.model.PulseLogStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PulseLogControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean PulseLogRepository pulseLogRepository;
    @MockitoBean TokenBlacklist tokenBlacklist;

    private static PulseLog sentLog(String numUnidad) {
        return new PulseLog(numUnidad, PulseLogStatus.SENT,
                null, null, null, null, ZonedDateTime.now(), null);
    }

    // 9.9.1 — ADMIN can list pulse log
    @Test
    @WithMockUser(authorities = "ADMIN")
    void listLogs_asAdmin_returns200WithContent() throws Exception {
        when(pulseLogRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(sentLog("Peugeot")));
        when(pulseLogRepository.countByFilters(isNull(), isNull(), isNull(), isNull()))
                .thenReturn(1L);

        mockMvc.perform(get("/api/pulse-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[0].numUnidad").value("Peugeot"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 9.9.2 — USER can list pulse log (dashboard access)
    @Test
    @WithMockUser(authorities = "USER")
    void listLogs_asUser_returns200() throws Exception {
        when(pulseLogRepository.findByFilters(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(List.of());
        when(pulseLogRepository.countByFilters(any(), any(), any(), any()))
                .thenReturn(0L);

        mockMvc.perform(get("/api/pulse-log"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    // 9.9.3 — unauthenticated returns 401
    @Test
    void listLogs_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/pulse-log"))
                .andExpect(status().isUnauthorized());
    }

    // 9.9.4 — filter by numUnidad forwarded to repository
    @Test
    @WithMockUser(authorities = "ADMIN")
    void listLogs_withNumUnidadFilter_passesFilterToRepository() throws Exception {
        when(pulseLogRepository.findByFilters(eq("Peugeot"), isNull(), isNull(), isNull(), eq(0), eq(20)))
                .thenReturn(List.of(sentLog("Peugeot")));
        when(pulseLogRepository.countByFilters(eq("Peugeot"), isNull(), isNull(), isNull()))
                .thenReturn(1L);

        mockMvc.perform(get("/api/pulse-log").param("numUnidad", "Peugeot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    // 9.9.5 — size > 100 rejected with 400 (gap 2.4: was a silent clamp, now consistent with
    // GET /api/users which already rejects via @Max(100))
    @Test
    @WithMockUser(authorities = "ADMIN")
    void listLogs_withOversizedPage_returns400() throws Exception {
        mockMvc.perform(get("/api/pulse-log").param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.fleetpulse.com/errors/validation-failed"));
    }

    // 9.9.5b — negative page rejected with 400
    @Test
    @WithMockUser(authorities = "ADMIN")
    void listLogs_withNegativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/pulse-log").param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.fleetpulse.com/errors/validation-failed"));
    }

    // 9.9.6 — filter by status forwarded to repository
    @Test
    @WithMockUser(authorities = "ADMIN")
    void listLogs_withStatusFilter_passesStatusToRepository() throws Exception {
        when(pulseLogRepository.findByFilters(isNull(), eq(PulseLogStatus.SENT), isNull(), isNull(), anyInt(), anyInt()))
                .thenReturn(List.of(sentLog("Peugeot")));
        when(pulseLogRepository.countByFilters(isNull(), eq(PulseLogStatus.SENT), isNull(), isNull()))
                .thenReturn(1L);

        mockMvc.perform(get("/api/pulse-log").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("SENT"));
    }

    // 9.9.7 — page and size reflected in response envelope
    @Test
    @WithMockUser(authorities = "ADMIN")
    void listLogs_withPageAndSize_reflectedInResponse() throws Exception {
        when(pulseLogRepository.findByFilters(isNull(), isNull(), isNull(), isNull(), eq(2), eq(10)))
                .thenReturn(List.of());
        when(pulseLogRepository.countByFilters(any(), any(), any(), any()))
                .thenReturn(0L);

        mockMvc.perform(get("/api/pulse-log").param("page", "2").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.size").value(10));
    }

    // ── GET /api/pulse-log/stats (gap 2.2) ───────────────────────────────────

    // 9.9.8 — ADMIN gets grouped counts by status
    @Test
    @WithMockUser(authorities = "ADMIN")
    void getStats_asAdmin_returns200WithGroupedCounts() throws Exception {
        when(pulseLogRepository.countGroupedByStatus(any(), any()))
                .thenReturn(Map.of(PulseLogStatus.SENT, 3L, PulseLogStatus.SKIPPED_STALE, 1L));

        mockMvc.perform(get("/api/pulse-log/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.countsByStatus.SENT").value(3))
                .andExpect(jsonPath("$.countsByStatus.SKIPPED_STALE").value(1))
                .andExpect(jsonPath("$.total").value(4));
    }

    // 9.9.9 — USER can also read stats (dashboard access, same as the list endpoint)
    @Test
    @WithMockUser(authorities = "USER")
    void getStats_asUser_returns200() throws Exception {
        when(pulseLogRepository.countGroupedByStatus(any(), any()))
                .thenReturn(Map.of());

        mockMvc.perform(get("/api/pulse-log/stats"))
                .andExpect(status().isOk());
    }

    // 9.9.10 — unauthenticated returns 401
    @Test
    void getStats_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/pulse-log/stats"))
                .andExpect(status().isUnauthorized());
    }

    // 9.9.11 — no date param defaults to today in fleet timezone
    @Test
    @WithMockUser(authorities = "ADMIN")
    void getStats_withNoDateParam_defaultsToToday() throws Exception {
        when(pulseLogRepository.countGroupedByStatus(any(), any())).thenReturn(Map.of());
        String today = LocalDate.now(ZoneId.of("America/Mexico_City")).toString();

        mockMvc.perform(get("/api/pulse-log/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value(today));
    }

    // 9.9.12 — explicit date param is respected
    @Test
    @WithMockUser(authorities = "ADMIN")
    void getStats_withExplicitDate_respectsParam() throws Exception {
        when(pulseLogRepository.countGroupedByStatus(any(), any())).thenReturn(Map.of());

        mockMvc.perform(get("/api/pulse-log/stats").param("date", "2026-07-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.date").value("2026-07-01"));
    }

    // 9.9.13 — no pulses that day returns an empty map and zero total, not an error
    @Test
    @WithMockUser(authorities = "ADMIN")
    void getStats_withNoPulsesThatDay_returnsZeroTotal() throws Exception {
        when(pulseLogRepository.countGroupedByStatus(any(), any())).thenReturn(Map.of());

        mockMvc.perform(get("/api/pulse-log/stats").param("date", "2020-01-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }
}
