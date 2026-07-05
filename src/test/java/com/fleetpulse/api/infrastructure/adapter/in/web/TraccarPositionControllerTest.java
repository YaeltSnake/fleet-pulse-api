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

import org.springframework.http.MediaType;

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

    // 9.6.10 — Traccar Client POST JSON body accepted and stored in cache
    @Test
    void receivePositionFromClient_withValidJsonBody_returns200AndStoresReading() throws Exception {
        String body = """
                {
                  "device_id": "Peugeot",
                  "location": {
                    "coords": {
                      "latitude": 19.5709,
                      "longitude": -99.2406,
                      "accuracy": 10.97,
                      "altitude": 2278.62,
                      "speed": -1,
                      "heading": -1
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/gps/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        verify(gpsPositionCache).store(eq("Peugeot"), any());
    }

    // 9.6.11 — POST with invalid coordinates returns 400
    @Test
    void receivePositionFromClient_withInvalidLatitude_returns400() throws Exception {
        String body = """
                {
                  "device_id": "Peugeot",
                  "location": {
                    "coords": {
                      "latitude": 95.0,
                      "longitude": -99.2406,
                      "accuracy": 10.0,
                      "altitude": 0,
                      "speed": 0,
                      "heading": 0
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/gps/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    // 9.6.12 — POST without auth header is accepted (public endpoint)
    @Test
    void receivePositionFromClient_withoutAuthHeader_returns200() throws Exception {
        String body = """
                {
                  "device_id": "Peugeot",
                  "location": {
                    "coords": {
                      "latitude": 19.5709,
                      "longitude": -99.2406,
                      "accuracy": 5.0,
                      "altitude": 2200.0,
                      "speed": 0,
                      "heading": 0
                    }
                  }
                }
                """;

        mockMvc.perform(post("/api/gps/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }
}
