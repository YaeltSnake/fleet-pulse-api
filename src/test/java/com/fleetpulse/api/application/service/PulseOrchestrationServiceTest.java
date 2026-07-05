package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.PulseLogRepository;
import com.fleetpulse.api.application.port.out.PulseSender;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.PulseSendException;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.domain.model.ScheduledPulse;
import com.fleetpulse.api.domain.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PulseOrchestrationServiceTest {

    private static final ZoneId FLEET_TZ = ZoneId.of("America/Mexico_City");
    // 10:00 AM Mexico City (CST = UTC-6) in January 2026
    private static final Clock FIXED_CLOCK =
            Clock.fixed(Instant.parse("2026-01-01T16:00:00Z"), FLEET_TZ);

    private static final BigDecimal LAT = new BigDecimal("19.4326");
    private static final BigDecimal LON = new BigDecimal("-99.1332");

    @Mock private UnitRepository unitRepository;
    @Mock private GpsCoordinateProvider gpsProvider;
    @Mock private PulseSender pulseSender;
    @Mock private PulseLogRepository pulseLogRepository;

    private PulseOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new PulseOrchestrationService(
                unitRepository, gpsProvider, pulseSender, pulseLogRepository,
                "default-tracking", FIXED_CLOCK);
    }

    // ── sendPulse() ────────────────────────────────────────────────────────────

    // 9.3.1
    @Test
    void sendPulse_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.sendPulse("Ghost"))
                .isInstanceOf(UnitNotFoundException.class);
    }

    // 9.3.2
    @Test
    void sendPulse_withInactiveUnit_silentlySkips() {
        Unit inactive = unit("Peugeot", false, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(inactive));

        service.sendPulse("Peugeot");

        verifyNoInteractions(pulseSender);
    }

    // 9.3.3
    @Test
    void sendPulse_withUnitOutsideActiveWindow_silentlySkips() {
        // FIXED_CLOCK = 10:00 AM; unit window is 14:00–18:00
        Unit outOfWindow = unit("Peugeot", true, LocalTime.of(14, 0), LocalTime.of(18, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(outOfWindow));

        service.sendPulse("Peugeot");

        verifyNoInteractions(pulseSender);
    }

    // 9.3.4 — GPS unavailable: getCoordinates throws → SKIPPED, pulseSender not called
    @Test
    void sendPulse_withGpsNotAvailable_silentlySkips() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        when(gpsProvider.getCoordinates("Peugeot"))
                .thenThrow(new GpsProviderUnavailableException("No GPS data received yet for unit: Peugeot"));

        service.sendPulse("Peugeot");

        verifyNoInteractions(pulseSender);
        verify(pulseLogRepository).save(any());
    }

    // 9.3.5
    @Test
    void sendPulse_withActiveUnitInWindow_callsPulseSender() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        when(gpsProvider.getCoordinates("Peugeot")).thenReturn(reading("Peugeot"));

        service.sendPulse("Peugeot");

        verify(pulseSender).send(any(ScheduledPulse.class));
    }

    // 9.3.6
    @Test
    void sendPulse_withNullUnitTrackingNumber_usesDefaultTrackingNumber() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        when(gpsProvider.getCoordinates("Peugeot")).thenReturn(reading("Peugeot"));

        service.sendPulse("Peugeot");

        ArgumentCaptor<ScheduledPulse> captor = ArgumentCaptor.forClass(ScheduledPulse.class);
        verify(pulseSender).send(captor.capture());
        assertThat(captor.getValue().getEffectiveTrackingNumber()).isEqualTo("default-tracking");
    }

    // 9.3.7
    @Test
    void sendPulse_withUnitTrackingNumber_usesUnitTrackingNumber() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), "unit-tracking");
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        when(gpsProvider.getCoordinates("Peugeot")).thenReturn(reading("Peugeot"));

        service.sendPulse("Peugeot");

        ArgumentCaptor<ScheduledPulse> captor = ArgumentCaptor.forClass(ScheduledPulse.class);
        verify(pulseSender).send(captor.capture());
        assertThat(captor.getValue().getEffectiveTrackingNumber()).isEqualTo("unit-tracking");
    }

    // ── dispatch() ────────────────────────────────────────────────────────────

    // 9.3.8 — extra case discovered: Objects.requireNonNull(gpsReading) guard
    @Test
    void dispatch_withNullGpsReading_throwsNullPointerException() {
        assertThatThrownBy(() -> service.dispatch("Peugeot", null))
                .isInstanceOf(NullPointerException.class);

        verifyNoInteractions(unitRepository);
    }

    // 9.3.9
    @Test
    void dispatch_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dispatch("Ghost", reading("Ghost")))
                .isInstanceOf(UnitNotFoundException.class);
    }

    // 9.3.10
    @Test
    void dispatch_withInactiveUnit_throwsUnitNotActiveException() {
        Unit inactive = unit("Peugeot", false, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(inactive));

        // dispatch() throws — it does NOT silently skip like sendPulse()
        assertThatThrownBy(() -> service.dispatch("Peugeot", reading("Peugeot")))
                .isInstanceOf(UnitNotActiveException.class);

        verifyNoInteractions(pulseSender);
    }

    // 9.3.11
    @Test
    void dispatch_withActiveUnit_callsPulseSender() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));

        service.dispatch("Peugeot", reading("Peugeot"));

        verify(pulseSender).send(any(ScheduledPulse.class));
    }

    // 9.3.12
    @Test
    void dispatch_withNullUnitTrackingNumber_usesDefaultTrackingNumber() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));

        service.dispatch("Peugeot", reading("Peugeot"));

        ArgumentCaptor<ScheduledPulse> captor = ArgumentCaptor.forClass(ScheduledPulse.class);
        verify(pulseSender).send(captor.capture());
        assertThat(captor.getValue().getEffectiveTrackingNumber()).isEqualTo("default-tracking");
    }

    // 9.3.13
    @Test
    void dispatch_withUnitTrackingNumber_usesUnitTrackingNumber() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), "unit-own");
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));

        service.dispatch("Peugeot", reading("Peugeot"));

        ArgumentCaptor<ScheduledPulse> captor = ArgumentCaptor.forClass(ScheduledPulse.class);
        verify(pulseSender).send(captor.capture());
        assertThat(captor.getValue().getEffectiveTrackingNumber()).isEqualTo("unit-own");
    }

    // 9.3.14
    @Test
    void dispatch_propagatesPulseSendException() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        doThrow(new PulseSendException("rejected")).when(pulseSender).send(any());

        assertThatThrownBy(() -> service.dispatch("Peugeot", reading("Peugeot")))
                .isInstanceOf(PulseSendException.class);
    }

    // 9.3.15 — extra case: no window check on dispatch() path
    @Test
    void dispatch_withUnitOutsideWindow_stillCallsPulseSender() {
        // dispatch() bypasses the schedule window — that is the caller's responsibility
        Unit outOfWindow = unit("Peugeot", true, LocalTime.of(14, 0), LocalTime.of(18, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(outOfWindow));

        service.dispatch("Peugeot", reading("Peugeot"));

        verify(pulseSender).send(any(ScheduledPulse.class));
    }

    // 9.3.16 — sendPulse() swallows PulseSendException, saves REJECTED log, does not re-throw
    @Test
    void sendPulse_onPulseSendException_swallowsAndSavesRejectedLog() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        when(gpsProvider.getCoordinates("Peugeot")).thenReturn(reading("Peugeot"));
        doThrow(new PulseSendException("QSolutions rejected pulse")).when(pulseSender).send(any());

        // Must NOT throw — scheduler path is resilient
        service.sendPulse("Peugeot");

        verify(pulseLogRepository).save(any());
    }

    // 9.3.17 — sendPulse() swallows GpsProviderUnavailableException, saves SKIPPED log
    @Test
    void sendPulse_onGpsUnavailable_swallowsAndSavesSkippedLog() {
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));
        when(gpsProvider.getCoordinates("Peugeot"))
                .thenThrow(new GpsProviderUnavailableException("No GPS data received yet for unit: Peugeot"));

        // Must NOT throw
        service.sendPulse("Peugeot");

        verify(pulseLogRepository).save(any());
        verifyNoInteractions(pulseSender);
    }

    // 9.3.18 — dispatch() must never touch the GPS provider (ADR-014 / ADR-016)
    @Test
    void dispatch_neverCallsGpsProvider() {
        // Arrange
        Unit active = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(active));

        // Act
        service.dispatch("Peugeot", reading("Peugeot"));

        // Assert: dispatch() uses the pre-assembled GpsReading — gpsProvider is bypassed entirely
        verify(gpsProvider, never()).isAvailable(any());
        verify(gpsProvider, never()).getCoordinates(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Unit unit(String numUnidad, boolean active, LocalTime inicio, LocalTime fin,
                      String trackingNumber) {
        return new Unit(numUnidad, false, inicio, fin, trackingNumber, active);
    }

    private GpsReading reading(String numUnidad) {
        return new GpsReading(numUnidad, LAT, LON,
                ZonedDateTime.now(FIXED_CLOCK), ProviderType.MANUAL);
    }
}
