package com.fleetpulse.api.domain.model;

import com.fleetpulse.api.domain.exception.InvalidCoordinateException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GpsReadingTest {

    private static final ZonedDateTime NOW = ZonedDateTime.now();
    private static final ProviderType PROVIDER = ProviderType.MANUAL;

    @Test
    void validCoordinatesConstructSuccessfully() {
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("19.432608"), new BigDecimal("-99.133209"), NOW, PROVIDER));
    }

    @Test
    void latitudBelowMinus90ThrowsInvalidCoordinateException() {
        assertThrows(InvalidCoordinateException.class, () ->
                new GpsReading("U001", new BigDecimal("-90.0001"), new BigDecimal("0.0001"), NOW, PROVIDER));
    }

    @Test
    void latitudAbove90ThrowsInvalidCoordinateException() {
        assertThrows(InvalidCoordinateException.class, () ->
                new GpsReading("U001", new BigDecimal("90.0001"), new BigDecimal("0.0001"), NOW, PROVIDER));
    }

    @Test
    void longitudBelowMinus180ThrowsInvalidCoordinateException() {
        assertThrows(InvalidCoordinateException.class, () ->
                new GpsReading("U001", new BigDecimal("0.0001"), new BigDecimal("-180.0001"), NOW, PROVIDER));
    }

    @Test
    void longitudAbove180ThrowsInvalidCoordinateException() {
        assertThrows(InvalidCoordinateException.class, () ->
                new GpsReading("U001", new BigDecimal("0.0001"), new BigDecimal("180.0001"), NOW, PROVIDER));
    }

    @Test
    void bothZeroThrowsInvalidCoordinateException() {
        assertThrows(InvalidCoordinateException.class, () ->
                new GpsReading("U001", new BigDecimal("0.0"), new BigDecimal("0.0"), NOW, PROVIDER));
    }

    @Test
    void latitudZeroWithValidNonZeroLongitudConstructsSuccessfully() {
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("0.0"), new BigDecimal("-99.133209"), NOW, PROVIDER));
    }

    @Test
    void longitudZeroWithValidNonZeroLatitudConstructsSuccessfully() {
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("19.432608"), new BigDecimal("0.0"), NOW, PROVIDER));
    }

    @Test
    void boundaryValuesConstructSuccessfully() {
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("-90"), new BigDecimal("-180"), NOW, PROVIDER));
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("90"), new BigDecimal("180"), NOW, PROVIDER));
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("-90"), new BigDecimal("180"), NOW, PROVIDER));
        assertDoesNotThrow(() ->
                new GpsReading("U001", new BigDecimal("90"), new BigDecimal("-180"), NOW, PROVIDER));
    }
}
