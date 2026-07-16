package com.fleetpulse.api.application.service;

import com.fleetpulse.api.application.port.out.PasswordHasher;
import com.fleetpulse.api.application.port.out.UserRepository;
import com.fleetpulse.api.application.service.command.CreateUserCommand;
import com.fleetpulse.api.application.service.command.UpdateUserCommand;
import com.fleetpulse.api.domain.exception.UserNotFoundException;
import com.fleetpulse.api.domain.exception.UsernameAlreadyExistsException;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;

    @InjectMocks UserManagementService service;

    @Test
    void createUser_whenUsernameIsNew_encodesPasswordAndSavesUser() {
        // Arrange
        CreateUserCommand command = new CreateUserCommand("new-user", "rawPassword", Role.USER);
        when(userRepository.existsByUsername("new-user")).thenReturn(false);
        when(passwordHasher.encode("rawPassword")).thenReturn("hashedPassword");

        // Act
        service.createUser(command);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getId()).isNull();
        assertThat(saved.getUsername()).isEqualTo("new-user");
        assertThat(saved.getPasswordHash()).isEqualTo("hashedPassword");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isActive()).isTrue();
    }

    @Test
    void createUser_whenUsernameAlreadyExists_throwsAndNeverSaves() {
        // Arrange
        CreateUserCommand command = new CreateUserCommand("existing-user", "pass", Role.USER);
        when(userRepository.existsByUsername("existing-user")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.createUser(command))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_whenUserFoundAndPasswordProvided_encodesAndSaves() {
        // Arrange
        User existing = new User(1L, "user", "oldHash", Role.USER, true);
        UpdateUserCommand command = new UpdateUserCommand(1L, "user", "newPassword", Role.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordHasher.encode("newPassword")).thenReturn("newHash");

        // Act
        service.updateUser(command);

        // Assert
        verify(passwordHasher).encode("newPassword");
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("newHash");
    }

    @Test
    void updateUser_whenPasswordIsNull_keepsExistingHashWithoutEncoding() {
        // Arrange
        User existing = new User(1L, "user", "existingHash", Role.USER, true);
        UpdateUserCommand command = new UpdateUserCommand(1L, "user", null, Role.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        service.updateUser(command);

        // Assert
        verify(passwordHasher, never()).encode(any());
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("existingHash");
    }

    @Test
    void updateUser_whenUserNotFound_throwsUserNotFoundException() {
        // Arrange
        UpdateUserCommand command = new UpdateUserCommand(99L, "user", "pass", Role.USER, true);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.updateUser(command))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_whenUsernameChangedToExistingOne_throwsUsernameAlreadyExistsException() {
        // Arrange
        User existing = new User(1L, "originalUser", "hash", Role.USER, true);
        UpdateUserCommand command = new UpdateUserCommand(1L, "takenUser", "pass", Role.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepository.existsByUsername("takenUser")).thenReturn(true);

        // Act + Assert
        assertThatThrownBy(() -> service.updateUser(command))
                .isInstanceOf(UsernameAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_whenUsernameUnchanged_doesNotTriggerDuplicateCheck() {
        // Arrange
        User existing = new User(1L, "sameUser", "hash", Role.USER, true);
        UpdateUserCommand command = new UpdateUserCommand(1L, "sameUser", null, Role.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        service.updateUser(command);

        // Assert
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository).save(any());
    }

    @Test
    void updateRoleAndActive_whenUserFound_preservesUsernameAndPasswordHash() {
        // Arrange
        User existing = new User(1L, "operador1", "existingHash", Role.ADMIN, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(existing));

        // Act
        service.updateRoleAndActive(1L, Role.USER, false);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("operador1");
        assertThat(saved.getPasswordHash()).isEqualTo("existingHash");
        assertThat(saved.getRole()).isEqualTo(Role.USER);
        assertThat(saved.isActive()).isFalse();
        verify(passwordHasher, never()).encode(any());
    }

    @Test
    void updateRoleAndActive_whenUserNotFound_throwsUserNotFoundException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.updateRoleAndActive(99L, Role.USER, false))
                .isInstanceOf(UserNotFoundException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void deactivateUser_delegatesToRepository() {
        // Arrange
        User user = new User(5L, "any-user", "hash", Role.USER, true);
        when(userRepository.findById(5L)).thenReturn(Optional.of(user));

        // Act
        service.deactivateUser(5L);

        // Assert
        verify(userRepository).deactivateById(5L);
    }

    @Test
    void deactivateUser_whenUserNotFound_throwsUserNotFoundException(){
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act
        assertThatThrownBy(() -> service.deactivateUser(99L)).isInstanceOf(UserNotFoundException.class);

        // Assert
        verify(userRepository, never()).deactivateById(anyLong());

    }

    @Test
    void findUser_whenFound_returnsUser() {
        // Arrange
        User user = new User(1L, "user", "hash", Role.USER, true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // Act
        User result = service.findUser(1L);

        // Assert
        assertThat(result).isEqualTo(user);
    }

    @Test
    void findUser_whenNotFound_throwsUserNotFoundException() {
        // Arrange
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        // Act + Assert
        assertThatThrownBy(() -> service.findUser(99L))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void findAllUsers_passesPageAndSizeToRepository() {
        // Arrange
        List<User> users = List.of(new User(1L, "user", "hash", Role.USER, true));
        when(userRepository.findAll(0, 10)).thenReturn(users);

        // Act
        List<User> result = service.findAllUsers(0, 10);

        // Assert
        verify(userRepository).findAll(0, 10);
        assertThat(result).isEqualTo(users);
    }

    @Test
    void countAllUsers_delegatesToRepository() {
        // Arrange
        when(userRepository.countAllUsers()).thenReturn(42L);

        // Act
        long result = service.countAllUsers();

        // Assert
        assertThat(result).isEqualTo(42L);
        verify(userRepository).countAllUsers();
    }
}
