package com.fleetpulse.api.domain.model;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnitTest {

    private static final LocalTime HORA_INICIO = LocalTime.of(8, 0);
    private static final LocalTime HORA_FIN = LocalTime.of(18, 0);

    private Unit activeUnit(boolean horarioFijo) {
        return new Unit("U001", horarioFijo, HORA_INICIO, HORA_FIN, null, true);
    }

    @Test
    void nowBeforeHoraInicioReturnsFalse() {
        assertFalse(activeUnit(true).isWithinActiveWindow(LocalTime.of(7, 59)));
    }

    @Test
    void nowAfterHoraFinReturnsFalse() {
        assertFalse(activeUnit(true).isWithinActiveWindow(LocalTime.of(18, 1)));
    }

    @Test
    void nowExactlyAtHoraInicioReturnsTrue() {
        assertTrue(activeUnit(true).isWithinActiveWindow(HORA_INICIO));
    }

    @Test
    void nowExactlyAtHoraFinReturnsTrue() {
        assertTrue(activeUnit(true).isWithinActiveWindow(HORA_FIN));
    }

    @Test
    void nowWithinWindowReturnsTrue() {
        assertTrue(activeUnit(true).isWithinActiveWindow(LocalTime.of(12, 0)));
    }

    @Test
    void inactiveUnitReturnsFalseRegardlessOfTime() {
        Unit inactiveUnit = new Unit("U001", true, HORA_INICIO, HORA_FIN, null, false);
        assertFalse(inactiveUnit.isWithinActiveWindow(LocalTime.of(12, 0)));
    }

    @Test
    void horarioFijoFalseDoesNotAffectWindowCheck() {
        Unit unit = activeUnit(false);
        assertTrue(unit.isWithinActiveWindow(LocalTime.of(12, 0)));
        assertFalse(unit.isWithinActiveWindow(LocalTime.of(7, 59)));
    }
}
