package com.fleetpulse.api.infrastructure.config;

import com.fleetpulse.api.application.port.out.GpsCoordinateProvider;
import com.fleetpulse.api.infrastructure.adapter.out.gps.ManualCoordinateAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ManualCoordinateProperties.class)
public class GpsProviderConfig {

    @Bean
    @ConditionalOnProperty(
            name = "gps.provider",
            havingValue = "manual",
            matchIfMissing = true
    )
    public GpsCoordinateProvider manualCoordinateAdapter(
            ManualCoordinateProperties props
    ){
        return new ManualCoordinateAdapter(props);
    }

    // Phase 6 — uncomment when Traccar is introduced:
    // @Bean
    // @ConditionalOnProperty(name = "gps.provider", havingValue = "traccar")
    // public GpsCoordinateProvider traccarCoordinateAdapter(GpsPositionCache cache) {
    //     return new TraccarCoordinateAdapter(cache);
    // }
}
