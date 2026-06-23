package com.fleetpulse.api.infrastructure.adapter.out.gps;

import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.infrastructure.config.ManualCoordinateProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManualCoordinateAdapterTest {

    private ManualCoordinateAdapter adapter;

    @BeforeEach
    void setUp() {
        ManualCoordinateProperties props = new ManualCoordinateProperties();
        props.setUnits(Map.of(
                "Peugeot", new ManualCoordinateProperties.UnitCoordinate(
                        new BigDecimal("19.4326"), new BigDecimal("-99.1332"))
        ));
        adapter = new ManualCoordinateAdapter(props);
    }

    // ── 9.2.1 ─────────────────────────────────────────────────────────────────

    @Test
    void isAvailable_withConfiguredUnit_returnsTrue() {
        assertThat(adapter.isAvailable("Peugeot")).isTrue();
    }

    // ── 9.2.2 ─────────────────────────────────────────────────────────────────

    @Test
    void isAvailable_withUnknownUnit_returnsFalse() {
        assertThat(adapter.isAvailable("Unknown")).isFalse();
    }

    // ── 9.2.3 ─────────────────────────────────────────────────────────────────

    @Test
    void getCoordinates_withConfiguredUnit_returnsGpsReadingWithExpectedCoordinates() {
        GpsReading reading = adapter.getCoordinates("Peugeot");

        assertThat(reading.getLatitud()).isEqualByComparingTo("19.4326");
        assertThat(reading.getLongitud()).isEqualByComparingTo("-99.1332");
    }

    // ── 9.2.4 ─────────────────────────────────────────────────────────────────

    @Test
    void getCoordinates_withUnknownUnit_throwsGpsProviderUnavailableException() {
        assertThatThrownBy(() -> adapter.getCoordinates("Unknown"))
                .isInstanceOf(GpsProviderUnavailableException.class)
                .hasMessageContaining("Unknown");
    }

    // ── 9.2.5 ─────────────────────────────────────────────────────────────────

    @Test
    void getCoordinates_returnsReadingWithCorrectNumUnidad() {
        GpsReading reading = adapter.getCoordinates("Peugeot");

        assertThat(reading.getNumUnidad()).isEqualTo("Peugeot");
    }

    // ── 9.2.6 ─────────────────────────────────────────────────────────────────

    @Test
    void getCoordinates_returnsReadingWithManualProviderType() {
        GpsReading reading = adapter.getCoordinates("Peugeot");

        assertThat(reading.getProviderType()).isEqualTo(ProviderType.MANUAL);
    }

    // ── 9.2.7 — timestamp freshness (no Clock injection — wall-clock assertion) ──

    @Test
    void getCoordinates_timestampReflectsCurrentTime() {
        // Arrange: capture the wall-clock window around the call
        ZonedDateTime before = ZonedDateTime.now(ZoneId.of("America/Mexico_City"));

        // Act
        GpsReading reading = adapter.getCoordinates("Peugeot");

        // Assert: fechaHoraEvento is set at dispatch time via ZonedDateTime.now(FLEET_TIMEZONE)
        ZonedDateTime after = ZonedDateTime.now(ZoneId.of("America/Mexico_City"));
        assertThat(reading.getFechaHoraEvento())
                .isAfterOrEqualTo(before)
                .isBeforeOrEqualTo(after);
    }

    // ── 9.2.8 — all 5 fleet units configured ─────────────────────────────────

    @Test
    void isAvailable_forAllFiveFleetUnits_returnsTrue() {
        // Arrange: full fleet configuration
        ManualCoordinateProperties allUnits = new ManualCoordinateProperties();
        allUnits.setUnits(Map.of(
                "Peugeot",  new ManualCoordinateProperties.UnitCoordinate(
                        new BigDecimal("19.4326"), new BigDecimal("-99.1332")),
                "Kangoo",   new ManualCoordinateProperties.UnitCoordinate(
                        new BigDecimal("19.4300"), new BigDecimal("-99.1300")),
                "Tr-02",    new ManualCoordinateProperties.UnitCoordinate(
                        new BigDecimal("19.4280"), new BigDecimal("-99.1280")),
                "Attitude", new ManualCoordinateProperties.UnitCoordinate(
                        new BigDecimal("19.4260"), new BigDecimal("-99.1260")),
                "Sentra",   new ManualCoordinateProperties.UnitCoordinate(
                        new BigDecimal("19.4240"), new BigDecimal("-99.1240"))
        ));
        ManualCoordinateAdapter allUnitsAdapter = new ManualCoordinateAdapter(allUnits);

        // Act / Assert: every fleet unit must resolve to a configured coordinate entry
        assertThat(allUnitsAdapter.isAvailable("Peugeot")).isTrue();
        assertThat(allUnitsAdapter.isAvailable("Kangoo")).isTrue();
        assertThat(allUnitsAdapter.isAvailable("Tr-02")).isTrue();
        assertThat(allUnitsAdapter.isAvailable("Attitude")).isTrue();
        assertThat(allUnitsAdapter.isAvailable("Sentra")).isTrue();
    }
}
