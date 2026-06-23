package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.model.Unit;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UnitEntity;
import org.springframework.stereotype.Component;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Component
public class UnitJpaAdapter implements UnitRepository {

    private final UnitJpaRepository jpaRepository;

    public UnitJpaAdapter(UnitJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Unit save(Unit unit) {
        UnitEntity saved = jpaRepository.save(toEntity(unit));
        return toDomain(saved);
    }

    @Override
    public Optional<Unit> findByNumUnidad(String numUnidad) {
        return jpaRepository.findByNumUnidad(numUnidad).map(this::toDomain);
    }

    @Override
    public List<Unit> findAll() {
        return jpaRepository.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void deactivateByNumUnidad(String numUnidad) {
        jpaRepository.deactivateByNumUnidad(numUnidad);
    }

    @Override
    public boolean existsByNumUnidad(String numUnidad) {
        return jpaRepository.existsByNumUnidad(numUnidad);
    }

    @Override
    public List<String> findAllActiveNumUnidades() {
        return jpaRepository.findAllActiveNumUnidades();
    }

    @Override
    public void setActive(String numUnidad, boolean active) {
        jpaRepository.setActive(numUnidad, active);
    }

    @Override
    public void setHorarioFijo(String numUnidad, boolean horarioFijo) {
        jpaRepository.setHorarioFijo(numUnidad, horarioFijo);
    }

    @Override
    public void updateSchedule(String numUnidad, LocalTime horaInicio, LocalTime horaFin) {
        jpaRepository.updateSchedule(numUnidad, horaInicio, horaFin);
    }

    private Unit toDomain(UnitEntity entity) {
        return new Unit(
                entity.getNumUnidad(),
                entity.isHorarioFijo(),
                entity.getHoraInicio(),
                entity.getHoraFin(),
                entity.getTrackingNumber(),
                entity.isActive()
        );
    }

    private UnitEntity toEntity(Unit unit) {
        return UnitEntity.builder()
                .id(null)
                .numUnidad(unit.getNumUnidad())
                .horarioFijo(unit.isHorarioFijo())
                .horaInicio(unit.getHoraInicio())
                .horaFin(unit.getHoraFin())
                .trackingNumber(unit.getTrackingNumber())
                .active(unit.isActive())
                .build();
    }
}
