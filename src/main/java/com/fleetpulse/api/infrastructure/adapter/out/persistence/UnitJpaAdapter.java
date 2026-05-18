package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.application.port.out.UnitRepository;
import com.fleetpulse.api.domain.exception.UnitNotFoundException;
import com.fleetpulse.api.domain.model.Unit;
import com.fleetpulse.api.infrastructure.adapter.out.persistence.entity.UnitEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Component
public class UnitJpaAdapter implements UnitRepository{

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
    @Transactional
    public void deactivateByNumUnidad(String numUnidad) {
        UnitEntity entity = jpaRepository.findByNumUnidad(numUnidad)
                .orElseThrow(() -> new UnitNotFoundException(numUnidad));

        entity.setActive(false);

        jpaRepository.save(entity);

    }

    @Override
    public boolean existsByNumUnidad(String numUnidad) {
        return jpaRepository.existsByNumUnidad(numUnidad);
    }

    private Unit toDomain(UnitEntity entity){
        return new Unit(
                entity.getNumUnidad(),
                entity.isHorarioFijo(),
                entity.getHoraInicio(),
                entity.getHoraFin(),
                entity.getTrackingNumber(),
                entity.isActive()
        );
    }

    private UnitEntity toEntity(Unit unit){
        return UnitEntity.builder()
                .numUnidad(unit.getNumUnidad())
                .horarioFijo(unit.isHorarioFijo())
                .horaInicio(unit.getHoraInicio())
                .horaFin(unit.getHoraFin())
                .trackingNumber(unit.getTrackingNumber())
                .active(unit.isActive())
                .build();
    }
}
