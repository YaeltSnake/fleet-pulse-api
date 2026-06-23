package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import com.fleetpulse.api.domain.exception.RoundAlreadyActiveException;
import com.fleetpulse.api.domain.exception.RoundNotActiveException;
import com.fleetpulse.api.domain.exception.ScheduleNotConfiguredException;
import com.fleetpulse.api.domain.exception.UnitNotActiveException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.CoordinateMode;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoundManagementServiceTest {

    private static final ZoneId FLEET_TZ = ZoneId.of("America/Mexico_City");
    // 10:00 AM Mexico City (CST=UTC-6, January 2026) — inside 09:00–17:00 window
    private static final Clock CLOCK_AT_10AM =
            Clock.fixed(Instant.parse("2026-01-01T16:00:00Z"), FLEET_TZ);
    // 08:00 AM Mexico City — before 09:00 window opens
    private static final Clock CLOCK_AT_8AM =
            Clock.fixed(Instant.parse("2026-01-01T14:00:00Z"), FLEET_TZ);

    private static final BigDecimal LAT = new BigDecimal("19.4326");
    private static final BigDecimal LON = new BigDecimal("-99.1332");

    private static final long ROUND_INTERVAL_MS = 900_000L;

    @Mock private UnitRepository unitRepository;
    @Mock private SendPulseUseCase sendPulseUseCase;
    @Mock private GpsCoordinateProvider gpsProvider;

    private RoundManagementService service;

    @BeforeEach
    void setUp() {
        service = new RoundManagementService(
                unitRepository, sendPulseUseCase, gpsProvider, CLOCK_AT_10AM, ROUND_INTERVAL_MS);
    }

    // ── startRound() validation chain ─────────────────────────────────────────

    // 9.4.1
    @Test
    void startRound_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.startRound("Ghost", CoordinateMode.MANUAL, LAT, LON))
                .isInstanceOf(UnitNotFoundException.class);
    }

    // 9.4.2
    @Test
    void startRound_withInactiveUnit_throwsUnitNotActiveException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(unit("Peugeot", false, LocalTime.of(9, 0), LocalTime.of(17, 0))));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON))
                .isInstanceOf(UnitNotActiveException.class);
    }

    // 9.4.3
    @Test
    void startRound_whenRoundAlreadyActive_throwsRoundAlreadyActiveException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON))
                .isInstanceOf(RoundAlreadyActiveException.class);
    }

    // 9.4.4
    @Test
    void startRound_withAutomaticMode_throwsInvalidCoordinateException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.AUTOMATIC, null, null))
                .isInstanceOf(InvalidCoordinateException.class)
                .hasMessageContaining("AUTOMATIC");
    }

    // 9.4.5
    @Test
    void startRound_withManualModeAndNullLat_throwsInvalidCoordinateException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL, null, LON))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    // 9.4.6
    @Test
    void startRound_withManualModeAndNullLon_throwsInvalidCoordinateException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, null))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    // 9.4.7
    @Test
    void startRound_withOutOfBoundsCoordinates_throwsInvalidCoordinateException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL,
                new BigDecimal("91.0"), LON))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    // 9.4.8 — extra case: GpsReading constructor rejects (0,0) as "no GPS fix"
    @Test
    void startRound_withZeroZeroCoordinates_throwsInvalidCoordinateException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL,
                BigDecimal.ZERO, BigDecimal.ZERO))
                .isInstanceOf(InvalidCoordinateException.class);
    }

    // 9.4.9
    @Test
    void startRound_withSentinelSchedule_throwsScheduleNotConfiguredException() {
        Unit sentinelUnit = new Unit("Peugeot", false, LocalTime.MIN, LocalTime.MAX, null, true);
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(sentinelUnit));

        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON))
                .isInstanceOf(ScheduleNotConfiguredException.class);
    }

    // 9.4.10
    @Test
    void startRound_happyPath_registersRoundInActiveMap() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        assertThat(service.activeRounds()).containsKey("Peugeot");
        RoundState state = service.activeRounds().get("Peugeot");
        assertThat(state.coordinateMode()).isEqualTo(CoordinateMode.MANUAL);
        assertThat(state.manualLat()).isEqualByComparingTo(LAT);
        assertThat(state.ultimoEnvio()).isNull();
    }

    // 9.4.11 — putIfAbsent race guard: second startRound that sneaks past containsKey is rejected
    @Test
    void startRound_concurrentStartAfterContainsKey_throwsRoundAlreadyActiveException() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));

        // First call registers the round
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        // Second call (same sequence: containsKey = true → direct throw)
        assertThatThrownBy(() -> service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON))
                .isInstanceOf(RoundAlreadyActiveException.class);
    }

    // ── stopRound() ───────────────────────────────────────────────────────────

    // 9.4.12
    @Test
    void stopRound_withActiveRound_removesFromMap() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        service.stopRound("Peugeot");

        assertThat(service.activeRounds()).doesNotContainKey("Peugeot");
    }

    // 9.4.13
    @Test
    void stopRound_withNoActiveRound_throwsRoundNotActiveException() {
        assertThatThrownBy(() -> service.stopRound("Peugeot"))
                .isInstanceOf(RoundNotActiveException.class);
    }

    // ── isRoundActive() / getRoundCoordinateMode() ────────────────────────────

    // 9.4.14
    @Test
    void isRoundActive_withActiveRound_returnsTrue() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        assertThat(service.isRoundActive("Peugeot")).isTrue();
    }

    // 9.4.15
    @Test
    void isRoundActive_withNoActiveRound_returnsFalse() {
        assertThat(service.isRoundActive("Peugeot")).isFalse();
    }

    // 9.4.16
    @Test
    void getRoundCoordinateMode_withActiveRound_returnsModeFromState() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        assertThat(service.getRoundCoordinateMode("Peugeot")).isEqualTo(CoordinateMode.MANUAL);
    }

    // 9.4.17
    @Test
    void getRoundCoordinateMode_withNoActiveRound_returnsNull() {
        assertThat(service.getRoundCoordinateMode("Peugeot")).isNull();
    }

    // ── processTick() ─────────────────────────────────────────────────────────

    // 9.4.18 — also covers extra case: null ultimoEnvio dispatches immediately
    @Test
    void processTick_withUnitInWindowAndNullUltimoEnvio_dispatchesAndUpdatesTimestamp() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        service.processTick();

        verify(sendPulseUseCase).dispatch(eq("Peugeot"), any());
        assertThat(service.activeRounds().get("Peugeot").ultimoEnvio()).isNotNull();
    }

    // 9.4.19
    @Test
    void processTick_withUnitOutsideWindow_doesNotDispatch() {
        RoundManagementService serviceAt8 = new RoundManagementService(
                unitRepository, sendPulseUseCase, gpsProvider, CLOCK_AT_8AM, ROUND_INTERVAL_MS);
        Unit unitWithWindow = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(unitWithWindow));
        serviceAt8.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        serviceAt8.processTick();

        verifyNoInteractions(sendPulseUseCase);
    }

    // 9.4.20
    @Test
    void processTick_withCooldownNotElapsed_doesNotDispatchOnSecondTick() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        service.processTick(); // first tick — dispatches, sets ultimoEnvio = fixed instant T
        service.processTick(); // second tick — Duration.between(T, T) = 0ms < intervalMs → skip

        // dispatch called exactly once across two ticks
        verify(sendPulseUseCase, times(1)).dispatch(any(), any());
    }

    // 9.4.21
    @Test
    void processTick_onGpsProviderUnavailableException_removesRoundFromMap() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        doThrow(new GpsProviderUnavailableException("SOAP failure"))
                .when(sendPulseUseCase).dispatch(any(), any());

        service.processTick();

        assertThat(service.activeRounds()).doesNotContainKey("Peugeot");
    }

    // 9.4.22
    @Test
    void processTick_onUnitNotActiveException_removesRoundFromMap() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        doThrow(new UnitNotActiveException("Peugeot"))
                .when(sendPulseUseCase).dispatch(any(), any());

        service.processTick();

        assertThat(service.activeRounds()).doesNotContainKey("Peugeot");
    }

    // 9.4.23
    @Test
    void processTick_onGenericException_logsButDoesNotRemoveRound() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        doThrow(new RuntimeException("unexpected infra failure"))
                .when(sendPulseUseCase).dispatch(any(), any());

        service.processTick();

        // generic exception only logs — round stays active
        assertThat(service.activeRounds()).containsKey("Peugeot");
    }

    // 9.4.24
    @Test
    void processTick_withMultipleUnits_processesAllAndDoesNotAbortOnOneFailure() {
        Unit peugeot = unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0));
        Unit kangoo  = unit("Kangoo",  true, LocalTime.of(9, 0), LocalTime.of(17, 0));
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(peugeot));
        when(unitRepository.findByNumUnidad("Kangoo")).thenReturn(Optional.of(kangoo));

        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);
        service.startRound("Kangoo",  CoordinateMode.MANUAL, LAT, LON);

        doThrow(new GpsProviderUnavailableException("GPS down"))
                .when(sendPulseUseCase).dispatch(eq("Kangoo"), any());

        service.processTick();

        // Both units were attempted
        verify(sendPulseUseCase).dispatch(eq("Peugeot"), any());
        verify(sendPulseUseCase).dispatch(eq("Kangoo"), any());

        // Peugeot succeeds and remains in activeRounds; Kangoo is removed
        assertThat(service.activeRounds()).containsKey("Peugeot");
        assertThat(service.activeRounds()).doesNotContainKey("Kangoo");
    }

    // 9.4.25 — extra case: first tick on a new round (ultimoEnvio == null) skips cooldown gate
    @Test
    void processTick_whenUltimoEnvioIsNull_immediatelyDispatchesWithoutWaitingForCooldown() {
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(activeUnit("Peugeot")));
        service.startRound("Peugeot", CoordinateMode.MANUAL, LAT, LON);

        assertThat(service.activeRounds().get("Peugeot").ultimoEnvio()).isNull();

        service.processTick();

        // Dispatch fired without needing to wait for any cooldown
        verify(sendPulseUseCase).dispatch(eq("Peugeot"), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Unit activeUnit(String numUnidad) {
        return new Unit(numUnidad, false, LocalTime.of(9, 0), LocalTime.of(17, 0), null, true);
    }

    private Unit unit(String numUnidad, boolean active, LocalTime inicio, LocalTime fin) {
        return new Unit(numUnidad, false, inicio, fin, null, active);
    }
}
