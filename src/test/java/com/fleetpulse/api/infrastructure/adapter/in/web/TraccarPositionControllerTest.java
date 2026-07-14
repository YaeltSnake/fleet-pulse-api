package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.infrastructure.adapter.out.cache.GpsPositionCache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TraccarPositionControllerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean GpsPositionCache gpsPositionCache;
    @MockitoBean TokenBlacklist tokenBlacklist;

    // 9.6.1 — valid GPS push accepted; reading stored in cache
    @Test
    void receivePosition_withValidCoordinates_returns200AndStoresReading() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "19.4326")
                        .param("lon", "-99.1332"))
                .andExpect(status().isOk());

        verify(gpsPositionCache).store(eq("Peugeot"), any());
    }

    // 9.6.2 — latitude out of range rejected
    @Test
    void receivePosition_withLatitudeOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "91.0")
                        .param("lon", "-99.1332"))
                .andExpect(status().isBadRequest());
    }

    // 9.6.3 — longitude out of range rejected
    @Test
    void receivePosition_withLongitudeOutOfRange_returns400() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "19.4326")
                        .param("lon", "181.0"))
                .andExpect(status().isBadRequest());
    }

    // 9.6.4 — (0,0) GPS not locked
    @Test
    void receivePosition_withZeroZeroCoordinates_returns400() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "0.0")
                        .param("lon", "0.0"))
                .andExpect(status().isBadRequest());
    }

    // 9.6.5 — blank id rejected (prevents storing empty key in cache)
    @Test
    void receivePosition_withBlankId_returns400() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "")
                        .param("lat", "19.4326")
                        .param("lon", "-99.1332"))
                .andExpect(status().isBadRequest());
    }

    // 9.6.6 — unknown unit ID silently accepted (200, not 404) — prevents unit enumeration
    @Test
    void receivePosition_withUnknownUnitId_returns200() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "UnknownUnit99")
                        .param("lat", "19.4326")
                        .param("lon", "-99.1332"))
                .andExpect(status().isOk());
    }

    // 9.6.7 — endpoint requires no Authorization header (OsmAnd push protocol)
    @Test
    void receivePosition_withoutAuthHeader_returns200() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "19.4326")
                        .param("lon", "-99.1332"))
                .andExpect(status().isOk());
    }

    // 9.6.8 — missing required params returns 400
    @Test
    void receivePosition_withMissingLatParam_returns400() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lon", "-99.1332"))
                .andExpect(status().isBadRequest());
    }

    // 9.6.9 — negative (southern hemisphere) coordinates accepted
    @Test
    void receivePosition_withSouthernHemisphereCoordinates_returns200() throws Exception {
        mockMvc.perform(get("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "-33.8688")
                        .param("lon", "151.2093"))
                .andExpect(status().isOk());

        verify(gpsPositionCache).store(eq("Peugeot"), any());
    }

    // 9.6.10 — real Traccar Client POST (application/x-www-form-urlencoded, OsmAnd-style fields)
    // accepted and stored in cache. Matches the actual client payload captured in production
    // (id, lat, lon, timestamp, accuracy, altitude, batt, charge) — NOT a JSON body.
    @Test
    void receivePositionForm_withValidFormParams_returns200AndStoresReading() throws Exception {
        mockMvc.perform(post("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "19.5709")
                        .param("lon", "-99.2406")
                        .param("timestamp", "1783559346")
                        .param("accuracy", "10.97")
                        .param("altitude", "2278.62")
                        .param("batt", "85")
                        .param("charge", "false"))
                .andExpect(status().isOk());

        verify(gpsPositionCache).store(eq("Peugeot"), any());
    }

    // 9.6.11 — POST with invalid coordinates returns 400
    @Test
    void receivePositionForm_withInvalidLatitude_returns400() throws Exception {
        mockMvc.perform(post("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "95.0")
                        .param("lon", "-99.2406"))
                .andExpect(status().isBadRequest());
    }

    // 9.6.12 — POST without auth header is accepted (public endpoint)
    @Test
    void receivePositionForm_withoutAuthHeader_returns200() throws Exception {
        mockMvc.perform(post("/api/gps/position")
                        .param("id", "Peugeot")
                        .param("lat", "19.5709")
                        .param("lon", "-99.2406"))
                .andExpect(status().isOk());
    }
}
