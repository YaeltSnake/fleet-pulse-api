package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.UserManagementUseCase;
import com.fleetpulse.api.application.port.out.TokenBlacklist;
import com.fleetpulse.api.application.port.out.TokenService;
import com.fleetpulse.api.domain.exception.UserNotFoundException;
import com.fleetpulse.api.domain.exception.UsernameAlreadyExistsException;
import com.fleetpulse.api.domain.model.Role;
import com.fleetpulse.api.domain.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenService tokenService;

    @MockitoBean UserManagementUseCase userManagementUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;

    @BeforeEach
    void setUp() {
        when(tokenBlacklist.isBlacklisted(any())).thenReturn(false);
    }

    private User adminUser(Long id) {
        return new User(id, "operador1", "hash", Role.ADMIN, true);
    }

    // ── GET /api/users ───────────────────────────────────────────────────────

    @Test
    void listUsers_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listUsers_withUserRole_returns403() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void listUsers_withAdminRole_returns200WithPagedContent() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.findAllUsers(0, 20)).thenReturn(List.of(adminUser(1L)));
        when(userManagementUseCase.countAllUsers()).thenReturn(1L);

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].username").value("operador1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void listUsers_withOversizedPage_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .param("size", "101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value("https://api.fleetpulse.com/errors/validation-failed"));
    }

    // ── POST /api/users ──────────────────────────────────────────────────────

    @Test
    void createUser_withAdminRole_returns201WithLocation() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.createUser(any())).thenReturn(adminUser(5L));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN"}"""))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.username").value("operador1"));
    }

    @Test
    void createUser_withDuplicateUsername_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.createUser(any())).thenThrow(new UsernameAlreadyExistsException("operador1"));

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN"}"""))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.type").value("https://api.fleetpulse.com/errors/username-exists"));
    }

    @Test
    void createUser_withBlankUsername_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"","rawPassword":"password123","role":"ADMIN"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createUser_withUserRole_returns403() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN"}"""))
                .andExpect(status().isForbidden());
    }

    // ── PUT /api/users/{id} ──────────────────────────────────────────────────

    @Test
    void updateUser_withAdminRole_returns200() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.updateUser(any())).thenReturn(adminUser(5L));

        mockMvc.perform(put("/api/users/5")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN","active":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("operador1"));
    }

    @Test
    void updateUser_withUnknownId_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.updateUser(any())).thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(put("/api/users/999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN","active":true}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.fleetpulse.com/errors/user-not-found"));
    }

    @Test
    void updateUser_withDuplicateUsername_returns409() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.updateUser(any())).thenThrow(new UsernameAlreadyExistsException("operador1"));

        mockMvc.perform(put("/api/users/5")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN","active":true}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void updateUser_withUserRole_returns403() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(put("/api/users/5")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"operador1","rawPassword":"password123","role":"ADMIN","active":true}"""))
                .andExpect(status().isForbidden());
    }

    // ── DELETE /api/users/{id} ───────────────────────────────────────────────

    @Test
    void deactivateUser_withAdminRole_returns204() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(delete("/api/users/5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivateUser_withUnknownId_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        org.mockito.Mockito.doThrow(new UserNotFoundException(999L))
                .when(userManagementUseCase).deactivateUser(999L);

        mockMvc.perform(delete("/api/users/999")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivateUser_withUserRole_returns403() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(delete("/api/users/5")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    // ── PATCH /api/users/{id} (gap 2.7) ──────────────────────────────────────

    @Test
    void updateUserRole_withAdminRole_returns200WithUpdatedRoleAndActive() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        User updated = new User(5L, "operador1", "hash", Role.USER, false);
        when(userManagementUseCase.updateRoleAndActive(5L, Role.USER, false)).thenReturn(updated);

        mockMvc.perform(patch("/api/users/5")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","active":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.rawPassword").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    void updateUserRole_withUnknownId_returns404() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");
        when(userManagementUseCase.updateRoleAndActive(eq(999L), any(), anyBoolean()))
                .thenThrow(new UserNotFoundException(999L));

        mockMvc.perform(patch("/api/users/999")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","active":false}"""))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("https://api.fleetpulse.com/errors/user-not-found"));
    }

    @Test
    void updateUserRole_withUserRole_returns403() throws Exception {
        String token = tokenService.generateAccessToken(2L, "USER");

        mockMvc.perform(patch("/api/users/5")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"USER","active":false}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUserRole_withMissingRole_returns400() throws Exception {
        String token = tokenService.generateAccessToken(1L, "ADMIN");

        mockMvc.perform(patch("/api/users/5")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"active":false}"""))
                .andExpect(status().isBadRequest());
    }
}
