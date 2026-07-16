package com.fleetpulse.api.infrastructure.adapter.in.web;

import com.fleetpulse.api.application.port.in.UserManagementUseCase;
import com.fleetpulse.api.domain.model.User;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.CreateUserRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.PagedResponse;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.UpdateUserRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.UpdateUserRoleRequest;
import com.fleetpulse.api.infrastructure.adapter.in.web.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "User Management", description = "ADMIN-only operations for user lifecycle: create, list, update, deactivate")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserManagementUseCase useCase;
    private static final String ADMIN_AUTHORITY = "hasAuthority('ADMIN')";

    public UserController(UserManagementUseCase useCase) {
        this.useCase = useCase;
    }

    @GetMapping
    @PreAuthorize(ADMIN_AUTHORITY)
    @Operation(
            summary = "List users",
            description = "Returns a paginated list of all users. Supports page number, page size, and sorting.",
            parameters = {
                    @Parameter(name = "page", description = "Page number (0-indexed)", example = "0"),
                    @Parameter(name = "size", description = "Items per page (max 100)", example = "20")
            }
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Paginated user list retrieved",
                    content = @Content(schema = @Schema(implementation = PagedResponse.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PagedResponse<UserResponse>> listUser(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ){

        List<User> rawUsers = useCase.findAllUsers(page, size);
        long totalElements = useCase.countAllUsers();
        int totalPages = (int) Math.ceil((double) totalElements / size);

        PagedResponse<UserResponse> response = new PagedResponse<>(
                rawUsers.stream().map(UserResponse::from).toList(),
                page,
                size,
                totalElements,
                totalPages
        );

        return ResponseEntity.ok(response);

    }

    @Operation(
            summary = "Create user",
            description = "Creates a new user with unique username. Returns the created user and its location."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User created successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed (e.g., blank username, invalid role)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Username already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    @PreAuthorize(ADMIN_AUTHORITY)
    public ResponseEntity<UserResponse> createUser(
            @RequestBody @Valid CreateUserRequest request){

        User user = useCase.createUser(request.toCommand());

        URI location = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(user.getId())
                .toUri();

        return ResponseEntity.created(location).body(UserResponse.from(user));

    }

    @Operation(
            summary = "Update user",
            description = "Replaces all fields of an existing user. The user ID in the path must exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User ID not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping("/{id}")
    @PreAuthorize(ADMIN_AUTHORITY)
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRequest request
            ){

        User user = useCase.updateUser(request.toCommand(id));

        return ResponseEntity.ok(UserResponse.from(user));

    }

    @Operation(
            summary = "Update user role and active status",
            description = "Partial update -- only role and active status. Never accepts a password; "
                    + "use PUT for a full replace including password."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User role/active updated successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User ID not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping("/{id}")
    @PreAuthorize(ADMIN_AUTHORITY)
    public ResponseEntity<UserResponse> updateUserRole(
            @PathVariable Long id,
            @RequestBody @Valid UpdateUserRoleRequest request
    ){
        User user = useCase.updateRoleAndActive(id, request.role(), request.active());

        return ResponseEntity.ok(UserResponse.from(user));
    }

    @Operation(
            summary = "Deactivate user",
            description = "Soft-deletes a user by marking them inactive. The user ID must exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "User deactivated successfully"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid JWT token",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Authenticated user lacks ADMIN authority",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User ID not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping("/{id}")
    @PreAuthorize(ADMIN_AUTHORITY)
    public ResponseEntity<Void> deactivateUser (@PathVariable Long id){
        useCase.deactivateUser(id);

        return ResponseEntity.noContent().build();
    }
}
