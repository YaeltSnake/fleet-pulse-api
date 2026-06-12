package com.fleetpulse.api.infrastructure.init;

import com.fleetpulse.api.application.port.out.PasswordHasher;
import com.fleetpulse.api.application.port.out.UserRepository;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.ApplicationArguments;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserInitializerTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;

    private static final String TEST_USERNAME = "admin";
    private static final String TEST_PASSWORD  = "secret123";

    private AdminUserInitializer initializer;

    @BeforeEach
    void setUp() {
        initializer = new AdminUserInitializer(userRepository, passwordHasher, TEST_USERNAME, TEST_PASSWORD);
    }

    @Test
    void run_whenNoAdminExists_createsAdminUserWithHashedPassword() throws Exception {
        // Arrange
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordHasher.encode(TEST_PASSWORD)).thenReturn("hashed");

        // Act
        initializer.run(mock(ApplicationArguments.class));

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getRole()).isEqualTo(Role.ADMIN);
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getUsername()).isEqualTo(TEST_USERNAME);
        assertThat(saved.getPasswordHash()).isEqualTo("hashed");
    }

    @Test
    void run_whenAdminPasswordIsBlank_throwsIllegalStateException() throws Exception{
        // Arrange
        AdminUserInitializer blankInitializer = new AdminUserInitializer(userRepository, passwordHasher, TEST_USERNAME, "");
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> blankInitializer.run(mock(ApplicationArguments.class))).isInstanceOf(IllegalStateException.class).hasMessage("INITIAL_ADMIN_PASSWORD env var is required when no ADMIN user exists");
        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).encode(any());

    }

    @Test
    void run_whenAdminAlreadyExists_skipsCreation() throws Exception {
        // Arrange
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(true);

        // Act
        initializer.run(mock(ApplicationArguments.class));

        // Assert
        verify(userRepository, never()).save(any());
        verify(passwordHasher, never()).encode(any());
    }

    @Test
    void run_savesUserWithNullId_guaranteesInsert() throws Exception {
        // Arrange
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordHasher.encode(TEST_PASSWORD)).thenReturn("hashed");

        // Act
        initializer.run(mock(ApplicationArguments.class));

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isNull();
    }

    @Test
    void run_usesUsernameFromConstructor() throws Exception {
        // Arrange
        when(userRepository.existsByRole(Role.ADMIN)).thenReturn(false);
        when(passwordHasher.encode(TEST_PASSWORD)).thenReturn("hashed");

        // Act
        initializer.run(mock(ApplicationArguments.class));

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getUsername()).isEqualTo(TEST_USERNAME);
    }

    @Test
    void constructor_withNullAdminPassword_throwsNullPointerException(){
        // Arrange / Act / Assert
        assertThatThrownBy(() -> new AdminUserInitializer(userRepository, passwordHasher, TEST_USERNAME, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("adminPassword must not be null");
    }

    @Test
    void constructor_withNullUserRepository_throwsNullPointerException() {
        // Act + Assert
        assertThatThrownBy(() -> new AdminUserInitializer(null, passwordHasher, TEST_USERNAME, TEST_PASSWORD))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("userRepository must not be null");
    }

    @Test
    void constructor_withNullPasswordHasher_throwsNullPointerException() {
        // Act + Assert
        assertThatThrownBy(() -> new AdminUserInitializer(userRepository, null, TEST_USERNAME, TEST_PASSWORD))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("passwordHasher must not be null");
    }
}
