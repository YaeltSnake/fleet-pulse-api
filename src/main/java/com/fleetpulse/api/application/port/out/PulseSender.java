package com.fleetpulse.api.application.port.out;

import com.fleetpulse.api.domain.exception.PulseSendException;
import com.fleetpulse.api.domain.model.ScheduledPulse;

public interface PulseSender {
    void send(ScheduledPulse pulse) throws PulseSendException;
}
