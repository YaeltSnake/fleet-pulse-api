package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.domain.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProviderTestServiceTest {

    private static final ZoneId FLEET_TZ = ZoneId.of("America/Mexico_City");
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T16:00:00Z"), FLEET_TZ);

    private static final BigDecimal LAT = new BigDecimal("19.4326");
    private static final BigDecimal LON = new BigDecimal("-99.1332");

    @Mock private UnitRepository unitRepository;
    @Mock private GpsCoordinateProvider gpsProvider;

    private ProviderTestService service;

    @BeforeEach
    void setUp() {
        service = new ProviderTestService(unitRepository, gpsProvider, FIXED_CLOCK);
    }

    // ── testProvider() ────────────────────────────────────────────────────────

    // 9.6.1
    @Test
    void testProvider_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.testProvider("Ghost"))
                .isInstanceOf(UnitNotFoundException.class);
    }

    // 9.6.2
    @Test
    void testProvider_withProviderNotAvailable_throwsGpsProviderUnavailableException() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));
        when(gpsProvider.isAvailable("Peugeot")).thenReturn(false);

        assertThatThrownBy(() -> service.testProvider("Peugeot"))
                .isInstanceOf(GpsProviderUnavailableException.class)
                .hasMessageContaining("Peugeot");
    }

    // 9.6.3
    @Test
    void testProvider_withAvailableProvider_returnsGpsReadingFromProvider() {
        GpsReading expected = new GpsReading("Peugeot", LAT, LON,
                ZonedDateTime.now(FIXED_CLOCK), ProviderType.MANUAL);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));
        when(gpsProvider.isAvailable("Peugeot")).thenReturn(true);
        when(gpsProvider.getCoordinates("Peugeot")).thenReturn(expected);

        GpsReading result = service.testProvider("Peugeot");

        assertThat(result.getNumUnidad()).isEqualTo("Peugeot");
        assertThat(result.getLatitud()).isEqualByComparingTo(LAT);
    }

    // 9.6.4 — structural: gpsProvider.getCoordinates() is never called when provider unavailable
    @Test
    void testProvider_whenProviderUnavailable_neverCallsGetCoordinates() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));
        when(gpsProvider.isAvailable("Peugeot")).thenReturn(false);

        assertThatThrownBy(() -> service.testProvider("Peugeot"))
                .isInstanceOf(GpsProviderUnavailableException.class);

        verify(gpsProvider, never()).getCoordinates(any());
    }

    // ── testProviderWithOverride() ────────────────────────────────────────────

    // 9.6.5
    @Test
    void testProviderWithOverride_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.testProviderWithOverride("Ghost", LAT, LON))
                .isInstanceOf(UnitNotFoundException.class);
    }

    // 9.6.6
    @Test
    void testProviderWithOverride_withValidCoords_returnsReadingWithManualProviderType() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));

        GpsReading result = service.testProviderWithOverride("Peugeot", LAT, LON);

        assertThat(result.getNumUnidad()).isEqualTo("Peugeot");
        assertThat(result.getLatitud()).isEqualByComparingTo(LAT);
        assertThat(result.getLongitud()).isEqualByComparingTo(LON);
        assertThat(result.getProviderType()).isEqualTo(ProviderType.MANUAL);
    }

    // 9.6.7
    @Test
    void testProviderWithOverride_withInvalidCoords_throwsInvalidCoordinateException() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));

        assertThatThrownBy(() -> service.testProviderWithOverride("Peugeot",
                new BigDecimal("91.0"), LON))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    // 9.6.8 — structural: gpsProvider is never called in the override path
    @Test
    void testProviderWithOverride_neverCallsConfiguredGpsProvider() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));

        service.testProviderWithOverride("Peugeot", LAT, LON);

        verify(gpsProvider, never()).isAvailable(any());
        verify(gpsProvider, never()).getCoordinates(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Unit activeUnit(String numUnidad) {
        return new Unit(numUnidad, false, LocalTime.of(9, 0), LocalTime.of(17, 0), null, true);
    }
}
