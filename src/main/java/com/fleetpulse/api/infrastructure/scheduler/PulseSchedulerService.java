package com.fleetpulse.api.infrastructure.scheduler;

import com.fleetpulse.api.application.port.in.SendPulseUseCase;
import com.fleetpulse.api.application.port.out.UnitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.List;
import java.util.Objects;

public class PulseSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(PulseSchedulerService.class);

    private final SendPulseUseCase sendPulseUseCase;
    private final UnitRepository unitRepository;

    public PulseSchedulerService(SendPulseUseCase sendPulseUseCase, UnitRepository unitRepository) {
        this.sendPulseUseCase = Objects.requireNonNull(sendPulseUseCase);
        this.unitRepository   = Objects.requireNonNull(unitRepository);
    }

    @Scheduled(fixedRateString = "${scheduler.pulse.interval-ms}")
    public void dispatchAllActiveUnits() {
        List<String> activeUnits = unitRepository.findAllActiveNumUnidades();
        log.info("SCHEDULER_TICK_START unitsToProcess={}", activeUnits.size());

        for (String numUnidad : activeUnits) {
            try {
                sendPulseUseCase.sendPulse(numUnidad);
            } catch (Exception e) {
                log.error("SCHEDULER_UNIT_FAILED numUnidad={} reason={}", numUnidad, e.getMessage());
                // Failure of one unit MUST NOT abort the cycle for remaining units
            }
        }

        log.info("SCHEDULER_TICK_END unitsProcessed={}", activeUnits.size());
    }
}
