package com.fleetpulse.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;


// FIXME-CONFIG-PROPS: this class is NOT registered as a Spring bean.
// @ConfigurationProperties alone is insufficient — missing @EnableConfigurationProperties
// in ApplicationConfig or @ConfigurationPropertiesScan on FleetPulseApiApplication.
// Spring will not bind application.properties to this class until one is added.
// MANDATORY fix before ManualCoordinateAdapter injects this in Phase 5.

@ConfigurationProperties(prefix = "gps.manual")      // (A)
public class ManualCoordinateProperties {

    private Map<String, UnitCoordinate> units = new LinkedHashMap<>();  // (B)

    public record UnitCoordinate(BigDecimal lat, BigDecimal lon) {}     // (C)

    public Map<String, UnitCoordinate> getUnits() { return units; }     // (D)
    public void setUnits(Map<String, UnitCoordinate> units) {           // (D)
        this.units = units;
    }
}
