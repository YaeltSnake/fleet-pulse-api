package com.fleetpulse.api.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "gps.manual")      // (A)
public class ManualCoordinateProperties {

    private Map<String, UnitCoordinate> units = new LinkedHashMap<>();  // (B)

    public record UnitCoordinate(BigDecimal lat, BigDecimal lon) {}     // (C)

    public Map<String, UnitCoordinate> getUnits() { return units; }     // (D)
    public void setUnits(Map<String, UnitCoordinate> units) {           // (D)
        this.units = units;
    }
}
