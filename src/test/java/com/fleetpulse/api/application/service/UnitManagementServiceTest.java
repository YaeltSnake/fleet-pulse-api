package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.ScheduleConflictException;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.Unit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UnitManagementServiceTest {

    @Mock
    private UnitRepository unitRepository;

    private UnitManagementService service;

    @BeforeEach
    void setUp() {
        service = new UnitManagementService(unitRepository);
    }

    // ── listAllUnits() ────────────────────────────────────────────────────────

    // 9.5.1
    @Test
    void listAllUnits_returnsAllUnitsFromRepository() {
        List<Unit> units = List.of(
                activeUnit("Peugeot"),
                activeUnit("Kangoo")
        );
        when(unitRepository.findAll()).thenReturn(units);

        List<Unit> result = service.listAllUnits();

        assertThat(result).hasSize(2);
    }

    // ── findByNumUnidad() ─────────────────────────────────────────────────────

    // 9.5.2
    @Test
    void findByNumUnidad_withKnownUnit_returnsUnit() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));

        Optional<Unit> result = service.findByNumUnidad("Peugeot");

        assertThat(result).isPresent();
        assertThat(result.get().getNumUnidad()).isEqualTo("Peugeot");
    }

    // 9.5.3
    @Test
    void findByNumUnidad_withUnknownUnit_returnsEmpty() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        Optional<Unit> result = service.findByNumUnidad("Ghost");

        assertThat(result).isEmpty();
    }

    // ── activateUnit() ────────────────────────────────────────────────────────

    // 9.5.4
    @Test
    void activateUnit_withKnownUnit_callsSetActiveAndReturnsUnit() {
        Unit before = unit("Peugeot", false);
        Unit after  = activeUnit("Peugeot");
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(before))
                .thenReturn(Optional.of(after));

        Unit result = service.activateUnit("Peugeot");

        verify(unitRepository).setActive("Peugeot", true);
        assertThat(result.isActive()).isTrue();
    }

    // 9.5.5
    @Test
    void activateUnit_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.activateUnit("Ghost"))
                .isInstanceOf(UnitNotFoundException.class);

        verify(unitRepository, never()).setActive(any(), anyBoolean());
    }

    // ── deactivateUnit() ──────────────────────────────────────────────────────

    // 9.5.6
    @Test
    void deactivateUnit_withKnownUnit_callsSetInactiveAndReturnsUnit() {
        Unit before = activeUnit("Peugeot");
        Unit after  = unit("Peugeot", false);
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(before))
                .thenReturn(Optional.of(after));

        Unit result = service.deactivateUnit("Peugeot");

        verify(unitRepository).setActive("Peugeot", false);
        assertThat(result.isActive()).isFalse();
    }

    // 9.5.7
    @Test
    void deactivateUnit_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivateUnit("Ghost"))
                .isInstanceOf(UnitNotFoundException.class);

        verify(unitRepository, never()).setActive(any(), anyBoolean());
    }

    // ── updateSchedule() ──────────────────────────────────────────────────────

    // 9.5.8
    @Test
    void updateSchedule_withValidWindow_updatesRepositoryAndReturnsUnit() {
        Unit before  = activeUnit("Peugeot");
        Unit updated = new Unit("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0), null, true);
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(before))
                .thenReturn(Optional.of(updated));

        Unit result = service.updateSchedule("Peugeot", true, LocalTime.of(9, 0), LocalTime.of(17, 0));

        verify(unitRepository).updateSchedule("Peugeot", LocalTime.of(9, 0), LocalTime.of(17, 0));
        verify(unitRepository).setHorarioFijo("Peugeot", true);
        assertThat(result.isHorarioFijo()).isTrue();
    }

    // 9.5.9
    @Test
    void updateSchedule_whenHoraInicioNotBeforeHoraFin_throwsScheduleConflictException() {
        when(unitRepository.findByNumUnidad("Peugeot")).thenReturn(Optional.of(activeUnit("Peugeot")));

        // horaInicio == horaFin → not strictly before → conflict
        assertThatThrownBy(() -> service.updateSchedule("Peugeot", false,
                LocalTime.of(10, 0), LocalTime.of(10, 0)))
                .isInstanceOf(ScheduleConflictException.class);

        verify(unitRepository, never()).updateSchedule(any(), any(), any());
    }

    // 9.5.10
    @Test
    void updateSchedule_withHorarioFijoFalseAndNullHours_usesSentinelMinMax() {
        Unit before  = activeUnit("Peugeot");
        Unit updated = new Unit("Peugeot", false, LocalTime.MIN, LocalTime.MAX, null, true);
        when(unitRepository.findByNumUnidad("Peugeot"))
                .thenReturn(Optional.of(before))
                .thenReturn(Optional.of(updated));

        service.updateSchedule("Peugeot", false, null, null);

        verify(unitRepository).updateSchedule("Peugeot", LocalTime.MIN, LocalTime.MAX);
    }

    // 9.5.11 — extra case: unit lookup before update
    @Test
    void updateSchedule_withUnknownUnit_throwsUnitNotFoundException() {
        when(unitRepository.findByNumUnidad("Ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateSchedule("Ghost", false,
                LocalTime.of(9, 0), LocalTime.of(17, 0)))
                .isInstanceOf(UnitNotFoundException.class);

        verify(unitRepository, never()).updateSchedule(any(), any(), any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Unit activeUnit(String numUnidad) {
        return new Unit(numUnidad, false, LocalTime.MIN, LocalTime.MAX, null, true);
    }

    private Unit unit(String numUnidad, boolean active) {
        return new Unit(numUnidad, false, LocalTime.MIN, LocalTime.MAX, null, active);
    }
}
