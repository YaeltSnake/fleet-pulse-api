package com.fleetpulse.api.infrastructure.adapter.out.soap;

import com.fleetpulse.api.application.port.out.PulseSender;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.exception.PulseSendException;
import com.fleetpulse.api.domain.model.ScheduledPulse;
import jakarta.xml.ws.WebServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tempuri.GPSInfo;
import org.tempuri.Protocolo;
import org.tempuri.ReceiveGPSInfoSoap;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.Objects;

public class QSolutionsSoapAdapter implements PulseSender {

    private static final Logger log = LoggerFactory.getLogger(QSolutionsSoapAdapter.class);
    private static final ZoneId FLEET_TIMEZONE = ZoneId.of("America/Mexico_City");

    // Initialized once — DatatypeFactory construction throws checked exception
    // If this fails at startup, the app cannot send pulses — fail fast is correct
    private static final DatatypeFactory DATATYPE_FACTORY;
    static {
        try {
            DATATYPE_FACTORY = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private final ReceiveGPSInfoSoap soapPort;
    private final String username;
    private final String password;
    private final String proveedor;

    public QSolutionsSoapAdapter(ReceiveGPSInfoSoap soapPort,
                                 String username,
                                 String password,
                                 String proveedor) {
        this.soapPort  = Objects.requireNonNull(soapPort,  "soapPort must not be null");
        this.username  = Objects.requireNonNull(username,  "username must not be null");
        this.password  = Objects.requireNonNull(password,  "password must not be null");
        this.proveedor = Objects.requireNonNull(proveedor, "proveedor must not be null");
    }

    @Override
    public void send(ScheduledPulse pulse) throws PulseSendException {
        GPSInfo info = buildGpsInfo(pulse);
        String numUnidad = pulse.getGpsReading().getNumUnidad();

        try {
            Protocolo response = soapPort.receiveGPSInformationObjeto(info);

            if (response.isProcessed()) {
                log.info("PULSE_SENT numUnidad={} tracking={} receptionDate={}",
                        numUnidad,
                        pulse.getEffectiveTrackingNumber(),
                        pulse.getFechaRecepcion());
            } else {
                log.warn("PULSE_REJECTED numUnidad={} message={}",
                        numUnidad, response.getMessage());
                throw new PulseSendException(
                        "QSolutions rejected pulse for " + numUnidad
                                + ": " + response.getMessage());
            }

        } catch (WebServiceException e) {
            log.error("PULSE_ERROR numUnidad={} reason={}", numUnidad, e.getMessage());

            // FIXME-SOAP-EX: roadmap specifies ExternalServiceUnavailableException for transport failures
            // (ADR-007 infra-to-infra pattern). GpsProviderUnavailableException is semantically more
            // precise and both map to 503 in GlobalExceptionHandler — functionally equivalent today
            // Evaluate aligning to ADR-007 pattern before Phase 5.
            throw new GpsProviderUnavailableException(
                    "SOAP transport failure for " + numUnidad + ": " + e.getMessage(), e);
        }
    }

    private GPSInfo buildGpsInfo(ScheduledPulse pulse) {
        GPSInfo info = new GPSInfo();
        info.setUsername(username);
        info.setPassword(password);                             // never logged — only set here
        info.setProveedor(proveedor);
        info.setNumUnidad(pulse.getGpsReading().getNumUnidad());
        info.setLatitud(pulse.getGpsReading().getLatitud());
        info.setLongitud(pulse.getGpsReading().getLongitud());
        info.setTrackingnumber(pulse.getEffectiveTrackingNumber());
        info.setFechaHoraEvento(toXmlCalendar(pulse.getGpsReading().getFechaHoraEvento()));
        info.setFechaRecepcion(toXmlCalendar(pulse.getFechaRecepcion()));
        return info;
    }

    private XMLGregorianCalendar toXmlCalendar(ZonedDateTime zdt) {
        ZonedDateTime atFleet = zdt.withZoneSameInstant(FLEET_TIMEZONE);
        GregorianCalendar gc = GregorianCalendar.from(atFleet);
        return DATATYPE_FACTORY.newXMLGregorianCalendar(gc);
    }
}
