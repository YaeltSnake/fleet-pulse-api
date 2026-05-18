package com.fleetpulse.api.infrastructure.adapter.out.persistence;

import com.fleetpulse.api.domain.exception.UserNotFoundException;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
@Import(UserJpaAdapter.class)
public class UserJpaAdapterTest {

    @Autowired
    private UserJpaAdapter adapter;

    @Test
    void saveAndFindByUsername_returnsAllFields(){

        User user = new User(null, "juanGarcia@gmail.com", "12sajodlfnniueh", Role.USER, true);

        adapter.save(user);

        Optional<User> result = adapter.findByUsername("juanGarcia@gmail.com");

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isNotNull();
        assertThat(result.get().getUsername()).isEqualTo("juanGarcia@gmail.com");
        assertThat(result.get().getPasswordHash()).isEqualTo("12sajodlfnniueh");
        assertThat(result.get().getRole()).isEqualByComparingTo(Role.USER);
        assertThat(result.get().isActive()).isTrue();

    }

    @Test
    void findAll_returnsAllSavedUsers(){
        adapter.save(new User(null, "juanGarcia@gmail.com", "12sajodlfnniueh", Role.USER, true));
        adapter.save(new User(null, "carlosAlberto@gmial.com", "15F&3ndjs_/hjJUisÑL", Role.USER, true));

        List<User> users = adapter.findAll();

        assertThat(users).hasSize(2);
        assertThat(users.getFirst().getUsername()).isEqualTo("juanGarcia@gmail.com");
    }

    @Test
    void existsByUsername_returnsTrueWhenExists(){
        adapter.save(new User(null, "carlosAlberto@gmial.com", "15F&3ndjs_/hjJUisÑL", Role.USER, true));

        assertThat(adapter.existsByUsername("carlosAlberto@gmial.com")).isTrue();

    }

    @Test
    void existsByUsername_returnsFalseWhenNotExists(){

        assertThat(adapter.existsByUsername("TestUser")).isFalse();

    }

    @Test
    void deactivateByUsername_setsActiveFalse(){

        adapter.save(new User(null, "carlosAlberto@gmial.com", "15F&3ndjs_/hjJUisÑL", Role.USER, true));
        adapter.deactivateByUsername("carlosAlberto@gmial.com");

        Optional<User> user = adapter.findByUsername("carlosAlberto@gmial.com");

        assertThat(user).isPresent();
        assertThat(user.get().isActive()).isFalse();

    }

    @Test
    void findByUsername_returnsEmptyWhenNotFound(){

        assertThat(adapter.findByUsername("Null")).isEmpty();

    }

    @Test
    void deactivateByUsername_throwsWhenNotFound(){

        assertThatThrownBy(() -> adapter.deactivateByUsername("None"))
                .isInstanceOf(UserNotFoundException.class);

    }

}
