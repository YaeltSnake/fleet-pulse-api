package com.fleetpulse.api.application.port.in;

import com.fleetpulse.api.domain.model.CoordinateMode;

import java.math.BigDecimal;

public interface ManageRoundUseCase {

    /**
     * Starts a per-unit dispatch round. Schedule window is read from DB at call time.
     * Configure the window via PUT /api/units/{numUnidad}/schedule before calling.
     * manualLat and manualLon are required when coordinateMode == MANUAL; null is valid for AUTOMATIC.
     */
    void startRound(String numUnidad, CoordinateMode coordinateMode,
                    BigDecimal manualLat, BigDecimal manualLon);

    /** Stops the active round. Throws RoundNotActiveException if no round is active. */
    void stopRound(String numUnidad);

    boolean isRoundActive(String numUnidad);

    /** Returns the coordinate mode of the active round, or {@code null} if no round is active. */
    CoordinateMode getRoundCoordinateMode(String numUnidad);

    /**
     * Evaluates all active rounds and dispatches units whose window is open
     * and whose cooldown has elapsed. Called by RoundTickService on each scheduler tick.
     */
    void processTick();
}
