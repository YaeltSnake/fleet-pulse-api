package com.fleetpulse.api.infrastructure.scheduler;

import com.fleetpulse.api.application.port.in.ManageRoundUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Objects;

public class RoundTickService {

    private static final Logger log = LoggerFactory.getLogger(RoundTickService.class);

    private final ManageRoundUseCase manageRoundUseCase;

    public RoundTickService(ManageRoundUseCase manageRoundUseCase) {
        this.manageRoundUseCase = Objects.requireNonNull(manageRoundUseCase,
                "manageRoundUseCase must not be null");
    }

    @Scheduled(fixedRateString = "${scheduler.round.tick-ms:30000}")
    public void tick() {
        log.debug("ROUND_TICK_START");
        manageRoundUseCase.processTick();
    }
}
