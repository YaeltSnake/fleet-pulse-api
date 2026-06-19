package com.fleetpulse.api.infrastructure.adapter.out.gps;

import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.domain.exception.GpsProviderUnavailableException;
import com.fleetpulse.api.domain.model.GpsReading;
import com.fleetpulse.api.domain.model.ProviderType;
import com.fleetpulse.api.infrastructure.config.ManualCoordinateProperties;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;

public class ManualCoordinateAdapter implements GpsCoordinateProvider {

    private static final ZoneId FLEET_TIMEZONE = ZoneId.of("America/Mexico_City");

    private final Map<String, ManualCoordinateProperties.UnitCoordinate> units;

    public ManualCoordinateAdapter(ManualCoordinateProperties props){
        this.units = Map.copyOf(props.getUnits());
    }

    @Override
    public boolean isAvailable(String numUnidad) {
        return units.containsKey(numUnidad);
    }

    @Override
    public GpsReading getCoordinates(String numUnidad) throws GpsProviderUnavailableException {
        ManualCoordinateProperties.UnitCoordinate coord = units.get(numUnidad);

        if(coord == null){
            throw new GpsProviderUnavailableException(
                    "No manual coordinates configured for unit: " + numUnidad
            );
        }

        return new GpsReading(
                numUnidad,
                coord.lat(),
                coord.lon(),
                ZonedDateTime.now(FLEET_TIMEZONE),
                ProviderType.MANUAL
        );
    }

}
