package com.fleetpulse.api.infrastructure.adapter.out.persistence.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Builder

@Entity
@Table(name = "units")
public class UnitEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "num_unidad")
    private String numUnidad;
    @Column(name = "horario_fijo")
    private boolean horarioFijo;
    @Column(name = "hora_inicio")
    private LocalTime horaInicio;
    @Column(name = "hora_fin")
    private LocalTime horaFin;
    @Column(name = "tracking_number")
    private String trackingNumber;
    @Column(name = "active")
    private boolean active;

}
