package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.domain.model.Unit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(UnitJpaAdapter.class)
public class UnitJpaAdapterTest {

    @Autowired
    private UnitJpaAdapter adapter;

    @Test
    void saveAndFindByNumUnidad_returnsAllFields() {
        Unit unit = new Unit("TestUnit", false,
                LocalTime.of(8, 0), LocalTime.of(18, 0),
                null, true);

        adapter.save(unit);



        Optional<Unit> result = adapter.findByNumUnidad("TestUnit");

        assertThat(result).isPresent();
        assertThat(result.get().getNumUnidad()).isEqualTo("TestUnit");
        assertThat(result.get().getHoraInicio()).isEqualTo(LocalTime.of(8, 0));
        assertThat(result.get().isActive()).isTrue();
        assertThat(result.get().isHorarioFijo()).isFalse();
        assertThat(result.get().getHoraFin()).isEqualTo(LocalTime.of(18,0));
        assertThat(result.get().getTrackingNumber()).isEqualTo(null);
    }

    @Test
    void findAll_returnsAllSavedUnits() {
        Unit unit2 = new Unit("TestUnit2", false,
                LocalTime.of(10,0), LocalTime.of(20,0),
                null, true);
        Unit unit3 = new Unit("TestUnit3", false,
                LocalTime.of(11,0), LocalTime.of(22,0),
                null, true);

        adapter.save(unit2);
        adapter.save(unit3);

        List<Unit> units = adapter.findAll();

        assertThat(units).hasSize(2);
    }

    @Test
    void existsByNumUnidad_returnsTrueWhenExists() {
        Unit unit = new Unit("TestUnit2", false,
                LocalTime.of(10,0), LocalTime.of(20,0),
                null, true);

        adapter.save(unit);

        assertThat(adapter.existsByNumUnidad("TestUnit2")).isTrue();

    }

    @Test
    void existsByNumUnidad_returnsFalseWhenNotExists() {
        boolean result = adapter.existsByNumUnidad("TestUnit2");
        assertThat(result).isFalse();
    }

    @Test
    void deactivateByNumUnidad_setsActiveFalse() {
        Unit unit = new Unit("TestUnit2", false,
                LocalTime.of(10,0), LocalTime.of(20,0),
                null, true);

        adapter.save(unit);
        adapter.deactivateByNumUnidad("TestUnit2");
        Optional<Unit> result = adapter.findByNumUnidad("TestUnit2");

        assertThat(result.get().isActive()).isFalse();
    }

    @Test
    void findByNumUnidad_returnsEmptyWhenNotFound() {
        Optional<Unit> result = adapter.findByNumUnidad("Empty");
        assertThat(result).isEmpty();
    }

}
