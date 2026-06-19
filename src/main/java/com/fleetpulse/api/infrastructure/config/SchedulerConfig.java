package com.fleetpulse.api.infrastructure.config;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.infrastructure.scheduler.PulseSchedulerService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    @ConditionalOnProperty(name = "scheduler.pulse.global-enabled", havingValue = "true")
    public PulseSchedulerService pulseSchedulerService(
            SendPulseUseCase sendPulseUseCase,
            UnitRepository unitRepository) {
        return new PulseSchedulerService(sendPulseUseCase, unitRepository);
    }
}
