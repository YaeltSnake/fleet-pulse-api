package com.fleetpulse.api.infrastructure.adapter.out.persistence.entity;

import com.fleetpulse.api.domain.model.PulseLogStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder

@Entity
@Table(name = "pulse_log")
public class PulseLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_unidad", length = 100, nullable = false)
    private String numUnidad;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PulseLogStatus status;

    @Column(name = "lat", precision = 9, scale = 6)
    private BigDecimal lat;

    @Column(name = "lon", precision = 9, scale = 6)
    private BigDecimal lon;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "tracking_number", length = 255)
    private String trackingNumber;

    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;
}
