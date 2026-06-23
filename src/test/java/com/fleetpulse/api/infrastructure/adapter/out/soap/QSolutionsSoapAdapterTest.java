package com.fleetpulse.api.infrastructure.adapter.out.soap;

import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.PulseSendException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.domain.model.ScheduledPulse;
import com.fleetpulse.api.domain.model.Unit;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.xml.ws.WebServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.tempuri.GPSInfo;
import org.tempuri.Protocolo;
import org.tempuri.ReceiveGPSInfoSoap;

import javax.xml.datatype.XMLGregorianCalendar;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QSolutionsSoapAdapterTest {

    private static final ZoneId FLEET_TZ = ZoneId.of("America/Mexico_City");

    private static final String TEST_USERNAME  = "test-user";
    private static final String TEST_PASSWORD  = "test-pass";
    private static final String TEST_PROVEEDOR = "test-proveedor";

    @Mock
    private ReceiveGPSInfoSoap soapPort;

    private QSolutionsSoapAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QSolutionsSoapAdapter(soapPort, TEST_USERNAME, TEST_PASSWORD, TEST_PROVEEDOR);
    }

    // ── 9.1.1 ─────────────────────────────────────────────────────────────────

    @Test
    void send_withProcessedResponse_doesNotThrow() {
        Protocolo processed = new Protocolo();
        processed.setProcessed(true);
        when(soapPort.receiveGPSInformationObjeto(any())).thenReturn(processed);

        assertThatCode(() -> adapter.send(buildPulse("Peugeot", "tracking-001")))
                .doesNotThrowAnyException();
    }

    // ── 9.1.2 ─────────────────────────────────────────────────────────────────

    @Test
    void send_withRejectedResponse_throwsPulseSendException() {
        Protocolo rejected = new Protocolo();
        rejected.setProcessed(false);
        rejected.setMessage("Unit not registered");
        when(soapPort.receiveGPSInformationObjeto(any())).thenReturn(rejected);

        assertThatThrownBy(() -> adapter.send(buildPulse("Peugeot", "tracking-001")))
                .isInstanceOf(PulseSendException.class)
                .hasMessageContaining("Peugeot");
    }

    // ── 9.1.3 ─────────────────────────────────────────────────────────────────

    @Test
    void send_onWebServiceException_throwsGpsProviderUnavailableException() {
        when(soapPort.receiveGPSInformationObjeto(any()))
                .thenThrow(new WebServiceException("Connection timed out"));

        assertThatThrownBy(() -> adapter.send(buildPulse("Peugeot", "tracking-001")))
                .isInstanceOf(GpsProviderUnavailableException.class)
                .hasMessageContaining("Peugeot");
    }

    // ── 9.1.4 ─────────────────────────────────────────────────────────────────

    @Test
    void send_populatesGpsInfoWithAllRequiredFields() {
        Protocolo processed = new Protocolo();
        processed.setProcessed(true);
        when(soapPort.receiveGPSInformationObjeto(any())).thenReturn(processed);

        ArgumentCaptor<GPSInfo> captor = ArgumentCaptor.forClass(GPSInfo.class);
        adapter.send(buildPulse("Peugeot", "tracking-001"));

        verify(soapPort).receiveGPSInformationObjeto(captor.capture());
        GPSInfo captured = captor.getValue();

        assertThat(captured.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(captured.getProveedor()).isEqualTo(TEST_PROVEEDOR);
        assertThat(captured.getNumUnidad()).isEqualTo("Peugeot");
        assertThat(captured.getLatitud()).isEqualByComparingTo("19.4326");
        assertThat(captured.getLongitud()).isEqualByComparingTo("-99.1332");
        assertThat(captured.getTrackingnumber()).isEqualTo("tracking-001");
        assertThat(captured.getFechaHoraEvento()).isNotNull();
        assertThat(captured.getFechaRecepcion()).isNotNull();
        // Password is set (required by SOAP contract) but NEVER logged
        assertThat(captured.getPassword()).isEqualTo(TEST_PASSWORD);
    }

    // ── 9.1.5 — @Disabled live gate (requires real QSolutions credentials) ───

    @Disabled("Live gate: requires QSOLUTIONS_* env vars and network access to QSolutions endpoint. " +
              "Run manually after setting credentials. Confirms isProcessed()==true.")
    @Test
    void send_realPulse_confirmsIsProcessedTrue() {
        // This test is intentionally disabled.
        // To run: set QSOLUTIONS_USERNAME, QSOLUTIONS_PASSWORD, QSOLUTIONS_PROVEEDOR,
        // QSOLUTIONS_ENDPOINT env vars, remove @Disabled, and execute this test in isolation.
        // Expected: no exception, server log shows PULSE_SENT with isProcessed=true.
    }

    // ── 9.1.6 — FLEET_TIMEZONE conversion (ADR-013 / FIXME-CLOCK) ────────────

    @Test
    void send_toXmlCalendar_usesFleetTimezone_notUtc() {
        // Arrange: 2026-01-01T20:00:00Z = 14:00 Mexico City (CST = UTC-6 in January)
        ZonedDateTime utcTimestamp = ZonedDateTime.of(2026, 1, 1, 20, 0, 0, 0, ZoneId.of("UTC"));
        Protocolo processed = new Protocolo();
        processed.setProcessed(true);
        when(soapPort.receiveGPSInformationObjeto(any())).thenReturn(processed);
        ArgumentCaptor<GPSInfo> captor = ArgumentCaptor.forClass(GPSInfo.class);

        // Act
        adapter.send(buildPulseWithTimestamp("Peugeot", utcTimestamp));

        // Assert: XMLGregorianCalendar hour must be 14 (CST), not 20 (UTC)
        verify(soapPort).receiveGPSInformationObjeto(captor.capture());
        XMLGregorianCalendar fechaEvento = captor.getValue().getFechaHoraEvento();
        assertThat(fechaEvento.getHour()).isEqualTo(14);
        assertThat(fechaEvento.getTimezone()).isEqualTo(-360); // UTC-6 = -360 minutes
    }

    // ── 9.1.7 — password never appears in logs (Q7 / Skill-01) ──────────────

    @Test
    void send_doesNotLogPassword() {
        // Arrange: attach Logback appender to capture log output
        Logger classLogger = (Logger) LoggerFactory.getLogger(QSolutionsSoapAdapter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        classLogger.addAppender(appender);

        Protocolo processed = new Protocolo();
        processed.setProcessed(true);
        when(soapPort.receiveGPSInformationObjeto(any())).thenReturn(processed);

        try {
            // Act
            adapter.send(buildPulse("Peugeot", "tracking-001"));

            // Assert: password value must not appear in any log message
            boolean passwordInLogs = appender.list.stream()
                    .anyMatch(event -> event.getFormattedMessage().contains(TEST_PASSWORD));
            assertThat(passwordInLogs)
                    .as("Password value must not appear in any log message (ASVS V2.7.1)")
                    .isFalse();
        } finally {
            classLogger.detachAppender(appender);
        }
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ScheduledPulse buildPulse(String numUnidad, String trackingNumber) {
        Unit unit = new Unit(numUnidad, false, LocalTime.of(9, 0), LocalTime.of(17, 0), trackingNumber, true);
        GpsReading reading = new GpsReading(numUnidad,
                new BigDecimal("19.4326"), new BigDecimal("-99.1332"),
                ZonedDateTime.now(FLEET_TZ), ProviderType.MANUAL);
        return new ScheduledPulse(unit, reading, ZonedDateTime.now(FLEET_TZ), trackingNumber);
    }

    private ScheduledPulse buildPulseWithTimestamp(String numUnidad, ZonedDateTime timestamp) {
        Unit unit = new Unit(numUnidad, false, LocalTime.of(9, 0), LocalTime.of(17, 0), "tracking-001", true);
        GpsReading reading = new GpsReading(numUnidad,
                new BigDecimal("19.4326"), new BigDecimal("-99.1332"),
                timestamp, ProviderType.MANUAL);
        return new ScheduledPulse(unit, reading, timestamp, "tracking-001");
    }
}
