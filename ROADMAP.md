# fleet-pulse-api — Roadmap

> Living document. Updated after every completed milestone.
> Legend: ⬜ Pending · 🔄 In progress · ✅ Done

---

## Release Map

| Phase | Tag | Type | Milestone |
|---|---|---|---|
| 0 | `v0.1.0-phase0` | Pre-release | Project setup |
| 1 | `v0.1.0` | Pre-release | Domain model + ports |
| 2 | `v0.2.0` | Pre-release | Persistence |
| 3 | `v0.3.0` | Pre-release | Security + Auth |
| 4 | `v0.4.0` | Pre-release | SOAP confirmed live |
| 5 | `v1.0.0` | **Full release** | Production replacement |
| 6 | `v1.1.0` | Full release | Traccar GPS live |
| 7 | `v1.2.0` | Full release | API contract stable |
| 8 | `v2.0.0` | Full release | React frontend shipped |

---

## Phase 0 — Project Setup
**Tag:** `v0.1.0-phase0`

| Status | Task |
|--|---|
| ✅ | Spring Boot 3.5.14 + Maven + Java 21 project created |
| ✅ | Package structure set to `com.fleetpulse.api` |
| ✅ | Main application class renamed to `FleetPulseApiApplication` |
| ✅ | All dependencies added to `pom.xml` (JAX-WS, JWT, MapStruct, Flyway, Security) |
| ✅ | Lombok + MapStruct annotation processors configured together |
| ✅ | WSDL copied to `src/main/resources/wsdl/ReceiveGPSInfo.wsdl` |
| ✅ | `README.md` architectural blueprint added to project root |
| ✅ | `target/` excluded from git, unnecessary Spring MVC folders removed |
| ✅ | GitHub repository created and initial commit pushed |

---

## Phase 1 — Domain Model + Ports
**Tag:** `v0.1.0`
**Exit condition:** All tests pass with no Spring context loaded. `GpsReading` rejects invalid coordinates and `0.0, 0.0`. `Unit.isWithinActiveWindow()` is correct for all units including manual-schedule ones.

| Status | Task |
|---|---|
| ✅ | Create full package structure (domain, application, infrastructure) |
| ✅ | `FleetConstants.java` — `FLEET_TIMEZONE = ZoneId.of("America/Mexico_City")` |
| ✅ | `Role.java` — enum: ADMIN, USER |
| ✅ | `Unit.java` — aggregate root with `isWithinActiveWindow(LocalTime now)` |
| ✅ | `GpsReading.java` — immutable value object, validates in constructor |
| ✅ | `ScheduledPulse.java` — value object, assembled at dispatch, never persisted |
| ✅ | `InvalidCoordinateException.java` — thrown by `GpsReading` constructor |
| ✅ | `UnitNotFoundException.java` |
| ✅ | `GpsProviderUnavailableException.java` |
| ✅ | `PulseSendException.java` — thrown by `PulseSender` port |
| ✅ | Driving ports: `SendPulseUseCase`, `TestProviderUseCase`, `ManageUnitUseCase`, `ConfigureScheduleUseCase` |
| ✅ | Driven ports: `GpsCoordinateProvider`, `PulseSender`, `UnitRepository`, `UserRepository` |
| ✅ | ArchUnit dependency added to `pom.xml` (test scope) |
| ✅ | ArchUnit test — verify nothing in `domain/` or `application/` imports from `infrastructure/` |
| ✅ | Unit tests for `GpsReading` constructor validation (no Spring context) |
| ✅ | Unit tests for `Unit.isWithinActiveWindow()` (no Spring context) |

**Notes:**
- `ProviderType.java` added — enum `MANUAL / TRACCAR` required by `GpsReading`, not listed in original roadmap
- `FleetpulseapiApplicationTests` disabled with `@Disabled("Requires database — enable in Phase 2")` — Spring context test deferred until datasource is available
- `jaxws-maven-plugin` moved to `soap-codegen` Maven profile — SOAP stub generation is Phase 4, removed from default build cycle. Path bug in `<wsdlFile>` corrected in the same move.
- `lombok-mapstruct-binding` added to `annotationProcessorPaths` — prevents compilation errors in Phase 2+ when MapStruct mappers reference Lombok-annotated infrastructure classes
---

## Phase 2 — Persistence
**Tag:** `v0.2.0`
**Exit condition:** 5 units seed correctly. Repository adapters return domain objects. DB round-trip validated.

| Status | Task |
|--|---|
| ✅ | `application.properties` — bind all environment variables (`QSOLUTIONS_*`, `JWT_*`, `GPS_*`, `SPRING_DATASOURCE_*`) |
| ✅ | `V1__schema.sql` — units and users tables (no `DEFAULT CURRENT_TIMESTAMP` on `pulse_log`) |
| ✅ | `V2__seed_units.sql` — 5 fleet units seeded (Peugeot, Kangoo, Tr-02, Attitude, Sentra) |
| ✅ | `UnitEntity.java` + `UserEntity.java` — JPA entities, never exposed outside infrastructure |
| ✅ | `UnitJpaAdapter.java` implements `UnitRepository` — returns domain objects only |
| ✅ | `UserJpaAdapter.java` implements `UserRepository` — returns domain objects only |
| ✅ | Validate DB round-trip — save Unit, retrieve Unit, assert domain fields match |

---

## Phase 3 — Security + Auth
**Tag:** `v0.3.0`
**Exit condition:** ADMIN and USER roles enforce the authorization matrix. No endpoint reachable without valid token except `/api/auth/*`. Login returns access token + refresh token. Logout blacklists access token in Redis AND marks refresh token as `revoked = true` in DB. First ADMIN provisioned via `AdminUserInitializer` on startup. All tests pass. All 6 Layer 4 components implemented in order. No component marked as BLOCKED for more than one iteration. `ApplicationConfig` declares all `@Bean`s for application services and security components.

### Layer 1 — Infrastructure dependencies

| Status | Task |
|--|---|
| ✅ | Add Redis service to `docker-compose.yml` — `redis:7-alpine`, port 6379 |
| ✅ | Add `spring-boot-starter-data-redis` to `pom.xml` |
| ✅ | Confirm JJWT is present in `pom.xml` at correct version (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`) |
| ✅ | Add `REDIS_HOST`, `REDIS_PORT` to `application.properties` and `.env` |
| ✅ | `V3__refresh_tokens.sql` — fields: `id`, `token`, `username`, `expires_at`, `revoked`, `created_at`; indexes on `token` and `username` |
| ✅ | `V4__refresh_tokens_userid.sql` — migrates `username` → `user_id BIGINT` FK to `users(id)`; index on `user_id` |

### Layer 2 — Domain additions

| Status | Task |
|--|---|
| ✅ | `User.java` made fully immutable — all fields `final`, setters removed, `UserManagementService` uses command pattern (`CreateUserCommand`, `UpdateUserCommand`) |
| ✅ | `RefreshToken.java` — value object in `domain/model/`: fields `token`, `userId`, `expiresAt`, `revoked` (no `id` — infrastructure concern) |
| ✅ | `RefreshTokenRepository` port in `application/port/out/` — `findByToken`, `save`, `revokeByToken`, `deleteAllExpired` |
| ✅ | `TokenBlacklist` port in `application/port/out/` — `blacklist(String token, Duration remainingTtl)`, `isBlacklisted(String token)` |
| ✅ | `TokenService` port in `application/port/out/` — `generateAccessToken(Long userId, String role)`, `generateRefreshToken(Long userId)`, `extractUserId(String token)`, `isTokenValid(String token, Long userId)`, `remainingTtl(String token)`, `refreshTokenExpiresAt()` (keeps `AuthService` free of JJWT imports) |

### Layer 3 — Application services

| Status | Task |
|---|---|
| ✅ | `AuthService.java` in `application/service/` — login (verify password, issue access + refresh tokens), refresh (validate refresh token, issue new access token), logout (blacklist access token in Redis AND revoke refresh token in DB) |
| ✅ | `UserManagementService.java` in `application/service/` — ADMIN-only: create user, deactivate user, list users |
| ✅ | Both services call only ports — zero Spring, JJWT, or JPA imports |

### Layer 4 — Infrastructure / Security (Implement in this order)

| Order | Component | Files | Depends On | Status |
|---|---|---|---|--|
| 4.1 | Persistencia RefreshToken | `RefreshTokenEntity.java`, `RefreshTokenJpaRepository.java`, `RefreshTokenJpaAdapter.java` | Layer 3 (`AuthService` — consumes `RefreshTokenRepository` port) | ✅ |
| 4.2 | Redis Blacklist | `RedisTokenBlacklistAdapter.java` | Layer 3 (`TokenBlacklist` port), `StringRedisTemplate` from Spring Data Redis (Layer 1) | ✅ |
| 4.3 | JWT Service | `JwtService.java` | Layer 3 (`TokenService` port), `JWT_SECRET` env var, `GeneratedRefreshToken` record | ✅ |
| 4.4 | UserDetailsService | `UserDetailsServiceImpl.java` | Layer 2 (`UserRepository` port) — exists to suppress Spring Boot auto-config only; filter does NOT call it | ✅ |
| 4.5 | JWT Filter | `JwtAuthenticationFilter.java` | 4.3 (`JwtService`), 4.2 (`RedisTokenBlacklistAdapter`) | ✅ |
| 4.6 | Security Config + App Config | `SecurityConfig.java`, `ApplicationConfig.java` | 4.4 (`UserDetailsServiceImpl`), 4.5 (`JwtAuthenticationFilter`), `BCryptPasswordEncoder` `@Bean` | ✅ |

#### Dependency Rules

- Each component MUST be completed and its contracts verified before the next begins.
- A component is "complete" when its port contract is fully implemented and `mvn test` (ArchUnit) passes.
- If a component cannot be started because a dependency is not yet done, declare it explicitly: `BLOCKED by 4.X — reason`.
- `@Transactional` belongs in application services (`AuthService`, `UserManagementService`), NOT in any adapter in this layer.
- `ApplicationConfig` (4.6) is the single source of truth for all `@Bean` declarations: `AuthService`, `UserManagementService`, `BCryptPasswordEncoder`. No `@Service` annotation anywhere in `application/`.

### Layer 5 — Controllers + DTOs + Global Error Handling

> ⛔ Cannot begin until Layer 4.6 (`SecurityConfig`) is COMPLETE. Layer 4.6 is ✅ COMPLETE as of commit `58ce7a0`.

**Exit condition:** All endpoints respond with correct HTTP status, RFC 7807 error bodies, OpenAPI docs accessible at `/v3/api-docs`, and authorization matrix enforced. All components implemented in order. No component marked as BLOCKED. `AdminUserInitializer` seeds first ADMIN on fresh database. `mvn test` passes (unit + integration + ArchUnit).

#### Dependency Rules

- Each component MUST be completed and its contracts verified before the next begins.
- A component is "complete" when `mvn test` passes (unit + integration) and ArchUnit passes.
- If a component cannot be started because a dependency is not yet done, declare it explicitly: `BLOCKED by 5.X — reason`.
- `@ControllerAdvice` handles ALL errors. No Spring whitelabel page.
- DTOs are `record`. Controllers return `ResponseEntity`. Application services are never called directly from tests — inject and mock the use case port.
- Controllers inject **driving port interfaces** (`AuthUseCase`, `UserManagementUseCase`), never service implementations directly.

#### Component Map

| Order | Component | Files | Depends On | Status |
|---|---|---|---|--|
| 5.1 | DTOs + Commands | See 5.1 section | Layer 3 (ports defined) — COMPLETE | ✅ |
| 5.2 | Thin Mappers (optional) | `DtoToCommandMapper.java`, `ResponseFromDomainMapper.java` | 5.1 (DTOs exist) | ✅ |
| 5.3 | Global Exception Handler | `GlobalExceptionHandler.java` | Domain exceptions (Layer 1 — COMPLETE) | ✅ |
| 5.4 | AuthController | `AuthController.java` | 5.1, 5.3, `AuthUseCase` port (Layer 3 — COMPLETE) | ✅ |
| 5.5 | UserController | `UserController.java` | 5.1, 5.3, `UserManagementUseCase` port (Layer 3 — COMPLETE) | ✅ |
| 5.6 | OpenAPI + SpringDoc | `pom.xml` + `@Tag`/`@Operation` on controllers | 5.4, 5.5 (controllers exist) | ✅ |
| 5.7 | AdminUserInitializer | `AdminUserInitializer.java` | `UserRepository`, `PasswordHasher` (Layer 3 — COMPLETE). Parallel with 5.1. | ✅ |

#### 5.1 — DTOs + Commands

Files:
- `infrastructure/adapter/in/web/dto/LoginRequest.java`
- `infrastructure/adapter/in/web/dto/LoginResponse.java`
- `infrastructure/adapter/in/web/dto/RefreshRequest.java`
- `infrastructure/adapter/in/web/dto/RefreshResponse.java`
- `infrastructure/adapter/in/web/dto/CreateUserRequest.java`
- `infrastructure/adapter/in/web/dto/UpdateUserRequest.java`
- `infrastructure/adapter/in/web/dto/UserResponse.java`

> `UnitResponse` and `ScheduleUpdateRequest` belong to `UnitController` (Phase 7). Do not create them here.

Commands (application layer — pure Java records):
- `application/service/command/CreateUserCommand.java`
- `application/service/command/UpdateUserCommand.java`
- `application/service/command/LoginCommand.java`

Rules:
- ALL DTOs are `public record`.
- Request DTOs: Jakarta Validation annotations (`@NotBlank`, `@Size`, `@Pattern`, `@Email`).
- Response DTOs: only safe fields — no `passwordHash`, no internal IDs unless required by API contract.
- `LoginResponse` returns `accessToken`, `refreshToken`, `expiresAt` — intended auth response contract.
- `DTO.toCommand()` converts request DTO to domain command. NEVER pass DTO directly to application service.
- Business validation (exists in DB, role check) goes in application service, not in DTO.

Exit condition: All records compile. All `toCommand()` methods tested with valid and invalid input.

#### 5.2 — Thin Mappers (Optional)

Files (skip entirely if `toCommand()` in the record is sufficient):
- `infrastructure/adapter/in/web/mapper/DtoToCommandMapper.java`
- `infrastructure/adapter/in/web/mapper/ResponseFromDomainMapper.java`

Rules:
- NO MapStruct for DTO→Command. Manual mapping only.
- MapStruct ALLOWED only for Entity↔Domain in persistence adapters (Layer 2 — already done).
- If mapping is trivial (≤ 3 fields): put it in the record as `static` method, skip this class.
- If mapping has conditional logic or nested objects: create thin mapper class.
- Mapper has zero business logic. Only field assignment.

Exit condition: All mappings compile. Tested with edge cases (null fields, empty strings).

#### 5.3 — Global Exception Handler

File: `infrastructure/adapter/in/web/GlobalExceptionHandler.java`

Rules:
- `@ControllerAdvice`
- Returns `application/problem+json` for every error (`type`, `title`, `status`, `detail`, `instance`, `timestamp`)
- Never exposes stack traces to client
- Logs full stack trace with correlation ID (UUID) at `ERROR` level for 5xx errors
- Client receives correlation ID for support; stack trace stays in logs only

Exception mapping:

| Exception | HTTP Status | Type URI |
|---|---|---|
| `MethodArgumentNotValidException` | 400 | `/errors/validation-failed` |
| `InvalidCoordinateException` | 400 | `/errors/invalid-coordinates` |
| `UserNotFoundException` | 404 | `/errors/user-not-found` |
| `UnitNotFoundException` | 404 | `/errors/unit-not-found` |
| `InvalidCredentialsException` | 401 | `/errors/invalid-credentials` |
| `UserNotActiveException` | 401 | `/errors/invalid-credentials` (same URI — do not reveal reason to client) |
| `RefreshTokenNotFoundException` | 401 | `/errors/token-invalid` |
| `RefreshTokenExpiredException` | 401 | `/errors/token-expired` |
| `RefreshTokenRevokedException` | 401 | `/errors/token-revoked` |
| `AccessDeniedException` | 403 | `/errors/forbidden` |
| `UsernameAlreadyExistsException` | 409 | `/errors/username-exists` |
| `ExternalServiceUnavailableException` | 503 | `/errors/service-unavailable` |
| `GpsProviderUnavailableException` | 503 | `/errors/service-unavailable` (register now — active Phase 6) |
| `PulseSendException` | 502 | `/errors/soap-rejected` |
| `Exception` (fallback) | 500 | `/errors/internal-error` (log + correlation ID, never expose stack trace) |

> `OutOfActiveWindowException` does NOT exist in `domain/exception/` yet. Do not add it here. Add when the class is created in Phase 7 (`ConfigureScheduleUseCase` scope).

Exit condition: All exception types mapped. MockMvc tests verify every error response format and status code. No Spring whitelabel error page accessible.

#### 5.4 — AuthController

File: `infrastructure/adapter/in/web/AuthController.java`

```
POST /api/auth/login — permitAll
  Inject:   AuthUseCase (driving port — never AuthService directly)
  Request:  LoginRequest (username, password)
  Response: LoginResponse (accessToken, refreshToken, expiresAt)
  200 OK on success — tokens are not REST resources, no Location header
  401 Unauthorized on invalid credentials

POST /api/auth/refresh — permitAll
  Request:  RefreshRequest (refreshToken)
  Response: RefreshResponse (accessToken, refreshToken, expiresAt)
  200 OK on success
  401 Unauthorized on invalid/expired/revoked refresh token

POST /api/auth/logout — authenticated (ADMIN or USER)
  Request body: LogoutRequest (refreshToken)
  Access token from Authorization header — extracted by JwtAuthenticationFilter, not by controller
  Response: 204 No Content
  Blacklists access token in Redis + revokes refresh token in DB
```

Rules:
- Inject `AuthUseCase` (driving port), never `AuthService` directly.
- `JwtAuthenticationFilter` handles token extraction. Controller receives `Authentication` from `SecurityContextHolder`.

Exit condition: MockMvc tests — valid login (200), invalid credentials (401), expired access token (401), logout then immediate reuse of blacklisted token returns 401. All paths return RFC 7807 on error. OpenAPI annotations present.

#### 5.5 — UserController

File: `infrastructure/adapter/in/web/UserController.java`

```
GET /api/users — ADMIN only
  Inject:   UserManagementUseCase (driving port — never UserManagementService directly)
  Response: Page<UserResponse> (paginated, default size=20, max=100)
  200 OK

POST /api/users — ADMIN only
  Request:  CreateUserRequest
  Response: UserResponse
  201 Created — Location header: /api/users/{id}

PUT /api/users/{id} — ADMIN only
  Request:  UpdateUserRequest
  Response: UserResponse
  200 OK

DELETE /api/users/{id} — ADMIN only
  Response: 204 No Content
  Soft delete (active = false). Never hard delete.
```

Rules:
- `@PreAuthorize("hasAuthority('ADMIN')")` on class or individual methods.
- `Pageable` parameter for list endpoint. Max page size enforced (≤ 100) — return 400 if `size > 100`.
- Path variable `{id}` is `Long` userId (ADR-003 — accepted until Phase 6).

Exit condition: MockMvc tests — ADMIN creates user (201), USER token rejected (403), duplicate username (409 RFC 7807), pagination tested with `page`, `size`, `sort`. OpenAPI annotations present.

#### 5.6 — OpenAPI + SpringDoc

Add to `pom.xml`:
```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.5.0</version>
</dependency>
```

Rules:
- `@Tag(name, description)` on each controller class.
- `@Operation(summary, description)` on each endpoint method.
- `@ApiResponse` for every status code the method can return, including error responses with `ProblemDetail` schema.
- Annotations are infrastructure-only. Never on domain or application classes.

Access: `/swagger-ui.html` (interactive) · `/v3/api-docs` (OpenAPI JSON spec)

Exit condition: All endpoints documented. Error responses visible in Swagger UI.

#### 5.7 — AdminUserInitializer

File: `infrastructure/init/AdminUserInitializer.java`

> See also: **Layer 6** — same component tracked there for release marking. Mark both when complete.

Depends on: `UserRepository` port + `PasswordHasher` port — **COMPLETE since Layer 3/4**. Does NOT depend on 5.4 or 5.5. Implement in parallel with 5.1.

Rules:
- `ApplicationRunner` — runs on startup.
- Checks if any ADMIN exists via `UserRepository`.
- If no ADMIN: reads `INITIAL_ADMIN_PASSWORD` from environment.
  - If absent: `throw new IllegalStateException("INITIAL_ADMIN_PASSWORD env var is required when no ADMIN user exists")`.
  - If present: BCrypt-hash via `PasswordHasher`, insert via `UserRepository`.
- On second run: no-op. No duplicate. No error.
- Log output: `"ADMIN_SEEDED"` or `"ADMIN_ALREADY_EXISTS"` at `INFO` level.

Exit condition: Test — ADMIN created when none exists. Test — no duplicate on second run. Test — `IllegalStateException` when `INITIAL_ADMIN_PASSWORD` not set.

#### Test Matrix

| Component | Test Type | Tools | Scope |
|---|---|---|---|
| 5.1 DTOs | Unit | JUnit 5, AssertJ | Validation annotations, `toCommand()`, edge cases |
| 5.2 Mappers | Unit | JUnit 5, AssertJ | Field mapping, null handling |
| 5.3 Handler | Integration | `@WebMvcTest` (preferred) or `@SpringBootTest` + MockMvc | Every exception → correct `ProblemDetail` format and status code |
| 5.4 AuthController | Integration | `@SpringBootTest` + MockMvc + Testcontainers (MySQL + Redis) | Login (200), refresh, logout, blacklisted token reuse (401) |
| 5.5 UserController | Integration | `@SpringBootTest` + MockMvc + Testcontainers (MySQL) | CRUD, pagination, auth rejection (403) |
| 5.6 OpenAPI | Validation | swagger-cli | Spec compliance, schema correctness |
| 5.7 AdminUserInitializer | Integration | `@SpringBootTest` + Testcontainers (MySQL) | Startup seeding, idempotency, fail-fast on missing env var |

#### Blocked States

```
If 5.1 is not done → 5.2 BLOCKED (no DTOs to map)
If 5.3 is not done → 5.4 BLOCKED (no error handling for auth controller)
If 5.3 is not done → 5.5 BLOCKED (no error handling for user controller)
If 5.4 is not done → 5.6 BLOCKED (no controllers to document)
5.7 AdminUserInitializer → NOT BLOCKED by any 5.x — depends on Layer 3 only (COMPLETE)
```

### Layer 6 — First ADMIN

> **NOTE:** `AdminUserInitializer` is implemented and tested as component **5.7 in Layer 5**. Mark this layer complete when Layer 5 exit condition is fully met. No separate implementation needed here.

> ⛔ Cannot begin until Layer 4.6 (`SecurityConfig`) is COMPLETE.

| Status | Task |
|-------|---|
| ✅     | `AdminUserInitializer.java` — `ApplicationRunner`; checks if any ADMIN exists via `UserRepository`; if not, reads `INITIAL_ADMIN_PASSWORD` from env — fail fast with `IllegalStateException` if absent; BCrypt-hashes and inserts via `UserRepository` |

### Layer 7 — Tests

#### Philosophy

Layer 7 is not "testing after the fact." It is the **proof** that Layers 1–6 are correct. Every test answers one question: *"Does the system behave as specified when X happens?"*

A test that passes by accident is worse than no test. We write tests that **FAIL when the code is wrong**, not tests that PASS when the code is right.

```
        /\
       /  \      Controller Integration (few) — full HTTP stack, mocked use cases
      /    \     AuthControllerTest
     /------\
    /        \   Security + JPA + Redis Integration (some) — real adapters, real deps
   /          \  JwtAuthenticationFilterTest, RefreshTokenJpaAdapterTest
  /------------\
 /              \ Unit (many) — logic in isolation, no Spring, no I/O
/                \ AuthServiceTest, UserManagementServiceTest, AdminUserInitializerTest,
------------------  JwtServiceTest
```

**Rule:** If a test can be unit, it MUST be unit. If it needs a database, use `@DataJpaTest` + H2. If it needs the full filter chain, use `@SpringBootTest`. Never use `@SpringBootTest` for pure logic.

**Exit condition:** `mvn test` passes with zero failures. All 8 components complete (4 new classes, additions to 2 existing, 1 new filter test, 1 ArchUnit expansion). JaCoCo line coverage ≥ 80% on infrastructure adapters, 100% interaction coverage on application service methods. ArchUnit passes with zero regressions. All previously completed tests continue green without modification.

#### Already Complete

| Test | Type | Status |
|---|---|---|
| `GpsReadingTest` | Domain unit | ✅ |
| `UnitTest` | Domain unit | ✅ |
| `JwtServiceTest` | Infrastructure unit | ✅ — additions required in 7.4 |
| `RedisTokenBlacklistAdapterTest` | Redis integration | ✅ |
| `UserJpaAdapterTest` | JPA integration | ✅ |
| `HexagonalArchitectureTest` | ArchUnit | ✅ — expansion required in 7.8 |

#### Prerequisites — `application-test.properties` additions

Before implementing 7.6 and 7.7 (`@SpringBootTest` tests), add these entries to `src/test/resources/application-test.properties`. Both tests load the full Spring context, which requires JWT config and admin initializer config to resolve `@Value` bindings:

```properties
# JWT — use fixed test secret (same value as JwtServiceTest.TEST_SECRET)
jwt.secret=dGVzdC1zZWNyZXQta2V5LXRoYXQtaXMtbG9uZy1lbm91Z2gtZm9yLUhTMjU2
jwt.access-expiry-seconds=900
jwt.refresh-expiry-seconds=604800

# AdminUserInitializer — required by ApplicationConfig @Value binding
app.initial-admin-username=testadmin
app.initial-admin-password=testpassword123
```

> Without these, `@SpringBootTest` fails at context startup with `Could not resolve placeholder`. Add them before writing 7.6 or 7.7.

#### Dependency Rules

- Each test MUST be implemented, executed, and green before marking it complete. Compiles ≠ passes.
- NEVER mock the implementation class directly (`AuthService`, `UserManagementService`). Always mock the **driving port interface** (`AuthUseCase`, `UserManagementUseCase`). This enforces hexagonal boundaries even in tests.
- AAA pattern mandatory in every test method: `// Arrange`, `// Act`, `// Assert` inline comments required — they serve as documentation for future readers.
- Each test is independent and self-contained. No `@TestMethodOrder` unless testing state that legitimately persists across methods (e.g., Redis TTL expiry).
- AssertJ (`assertThat`) is the assertion library for all new tests. Do not add new JUnit 5 `assertEquals` calls.
- Test method names follow `whatIsBeingTested_condition_expectedOutcome`.
- `ArgumentCaptor` must be used whenever verifying the exact object passed to a mocked method.
- `verify(..., never())` must be used to assert side effects did NOT happen.

#### Component Map

| Order | Component | File | Test Type | Tools | Depends On | Status |
|---|---|---|---|---|---|---|
| 7.1 | AuthServiceTest | `AuthServiceTest.java` | Unit | Mockito — no Spring | Layer 3 — AuthService ✅ | ⬜ |
| 7.2 | UserManagementServiceTest | `UserManagementServiceTest.java` | Unit | Mockito — no Spring | Layer 3 — UserManagementService ✅ | ⬜ |
| 7.3 | AdminUserInitializerTest | `AdminUserInitializerTest.java` | Unit | Mockito — no Spring | Layer 5.7 — AdminUserInitializer ✅ | ⬜ |
| 7.4 | JwtServiceTest (additions) | `JwtServiceTest.java` | Unit | JUnit 5, JJWT — no Spring | Layer 4.3 — JwtService ✅ | ⬜ additions |
| 7.5 | RefreshTokenJpaAdapterTest | `RefreshTokenJpaAdapterTest.java` | JPA Integration | `@DataJpaTest` + H2 | Layer 4.1 — RefreshTokenJpaAdapter ✅ | ⬜ |
| 7.6 | JwtAuthenticationFilterTest | `JwtAuthenticationFilterTest.java` | Security Integration | `@SpringBootTest` + MockMvc + `@MockBean` | Layer 4.5 — JwtAuthenticationFilter ✅ | ⬜ |
| 7.7 | AuthControllerTest | `AuthControllerTest.java` | Controller Integration | `@SpringBootTest` + MockMvc + `@MockBean` | Layer 5 — AuthController ✅, GlobalExceptionHandler ✅ | ⬜ |
| 7.8 | ArchUnit (expansion) | `HexagonalArchitectureTest.java` | Architecture | ArchUnit — no Spring | All layers ✅ | ⬜ additions |

**Recommended implementation order: 7.1 → 7.2 → 7.3 → 7.4 → 7.5 → 7.6 → 7.7 → 7.8**
(unit tests first — fastest feedback; `@SpringBootTest` tests last — slowest startup)

#### 7.1 — AuthServiceTest

File: `src/test/java/com/fleetpulse/api/application/service/AuthServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;
    @Mock TokenService tokenService;
    @Mock TokenBlacklist tokenBlacklist;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService service;
}
```

Test cases:

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `login_withValidCredentials_returnsAuthResultWithTokens` | User found, active, password matches | `AuthResult` with `accessToken` + `refreshToken`; `refreshTokenRepository.save()` called | Happy path — full token issuance |
| 2 | `login_withNonExistentUser_throwsInvalidCredentialsException` | `findByUsername` → `Optional.empty()` | `InvalidCredentialsException` thrown — **NOT** `UserNotFoundException` | User enumeration prevention (ASVS V2.7.1) |
| 3 | `login_withWrongPassword_throwsInvalidCredentialsException` | User found, `passwordHasher.matches()` → false | `InvalidCredentialsException` thrown; `tokenService.generateAccessToken` NEVER called | Invalid password guard |
| 4 | `login_withInactiveUser_throwsUserNotActiveException` | User found, `active=false` | `UserNotActiveException` thrown; token generation NEVER called | Inactive user guard |
| 5 | `login_savesRefreshTokenWithCorrectFields` | Happy path | `ArgumentCaptor<RefreshToken>` captures: `userId` matches, `revoked=false`, `expiresAt` in future | Refresh token persistence |
| 6 | `refresh_withValidToken_revokesOldAndIssuesNew` | Token found, not expired, not revoked | `revokeByToken` called on old token; `save` called with new token; returns new `AuthResult` | Token rotation |
| 7 | `refresh_withExpiredToken_throwsRefreshTokenExpiredException` | Token found, `expiresAt` in past | `RefreshTokenExpiredException` thrown | Expiry guard |
| 8 | `refresh_withRevokedToken_throwsRefreshTokenRevokedException` | Token found, `revoked=true` | `RefreshTokenRevokedException` thrown | Revocation guard |
| 9 | `refresh_withNonExistentToken_throwsRefreshTokenNotFoundException` | `findByToken` → `Optional.empty()` | `RefreshTokenNotFoundException` thrown | Not-found guard |
| 10 | `logout_blacklistsAccessTokenWithRemainingTtl` | `remainingTtl` returns `Duration.ofMinutes(10)` | `tokenBlacklist.blacklist(accessToken, Duration.ofMinutes(10))` called | Access token blacklist with exact TTL |
| 11 | `logout_revokesRefreshTokenInDb` | Happy path | `refreshTokenRepository.revokeByToken(refreshToken)` called | Refresh token revocation |
| 12 | `logout_withAlreadyExpiredAccessToken_doesNotBlacklist` | `remainingTtl` returns negative `Duration` | `tokenBlacklist.blacklist(...)` NEVER called (expired token has no TTL to set) | No-op blacklist for already-expired tokens |

Rules:
- Test #2 is the user enumeration test. This verifies the fix in `AuthService.login()` — `findByUsername().orElseThrow(() -> new InvalidCredentialsException(...))`. If this test fails, the service is leaking username existence via 404.
- Test #5: use `ArgumentCaptor<RefreshToken>` on `refreshTokenRepository.save(...)` — assert `revoked=false`, `userId == user.getId()`, `expiresAt.isAfter(Instant.now())`.
- Test #12: `Duration.isNegative()` or `Duration.isZero()` → skip blacklist call. Verify with `verify(tokenBlacklist, never()).blacklist(any(), any())`.
- No Spring context. `@InjectMocks` wires the constructor injection.

Exit condition: 12 tests green. Test #2 specifically asserts `InvalidCredentialsException` — a `UserNotFoundException` would be a security regression.

#### 7.2 — UserManagementServiceTest

File: `src/test/java/com/fleetpulse/api/application/service/UserManagementServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordHasher passwordHasher;

    @InjectMocks UserManagementService service;
}
```

Test cases:

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `createUser_whenUsernameIsNew_encodesPasswordAndSavesUser` | `existsByUsername` → false | `encode(rawPassword)` called; `save(user)` called with hashed password, `role`, `active=true` | Happy path + encoding |
| 2 | `createUser_whenUsernameAlreadyExists_throwsAndNeverSaves` | `existsByUsername` → true | `UsernameAlreadyExistsException` thrown; `save` NEVER called | Duplicate guard |
| 3 | `updateUser_whenUserFoundAndPasswordProvided_encodesAndSaves` | `findById` returns user; new password not null | `encode(newPassword)` called; `save` called with new hash | Full update path |
| 4 | `updateUser_whenPasswordIsNull_keepsExistingHashWithoutEncoding` | `findById` returns user; `rawPassword=null` | `encode` NEVER called; `save` called with existing `passwordHash` | Password preservation on partial update |
| 5 | `updateUser_whenUserNotFound_throwsUserNotFoundException` | `findById` → `Optional.empty()` | `UserNotFoundException` thrown; `save` NEVER called | Not-found guard |
| 6 | `updateUser_whenUsernameChangedToExistingOne_throwsUsernameAlreadyExistsException` | Username changed + `existsByUsername` → true | `UsernameAlreadyExistsException` thrown; `save` NEVER called | Username uniqueness on update |
| 7 | `updateUser_whenUsernameUnchanged_doesNotTriggerDuplicateCheck` | Same username | `save` called successfully | No false positive on own username |
| 8 | `deactivateUser_delegatesToRepository` | Any `id` | `userRepository.deactivateById(id)` called once | Delegation |
| 9 | `findUser_whenFound_returnsUser` | `findById` returns `Optional.of(user)` | Same user returned | Delegation |
| 10 | `findUser_whenNotFound_throwsUserNotFoundException` | `findById` → `Optional.empty()` | `UserNotFoundException` thrown | Not-found guard |
| 11 | `findAllUsers_passesPageAndSizeToRepository` | `page=0`, `size=10` | `userRepository.findAll(0, 10)` called with exact args | Pagination delegation |
| 12 | `countAllUsers_delegatesToRepository` | `countAllUsers()` returns `42L` | Returns `42L` | Delegation |

Rules:
- Tests #1 and #3: `ArgumentCaptor<User>` on `save()` — assert `id=null` on create, `passwordHash` is the encoded value, `role` matches command.
- Tests #2, #5, #6: `verify(userRepository, never()).save(any())`.
- Test #4: `verify(passwordHasher, never()).encode(any())`.

Exit condition: 12 tests green. `ArgumentCaptor` confirms exact `User` object passed to `save()`.

#### 7.3 — AdminUserInitializerTest

File: `src/test/java/com/fleetpulse/api/infrastructure/init/AdminUserInitializerTest.java`

```java
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
}
```

> `@InjectMocks` cannot be used — the constructor has non-mock `String` parameters. Construct manually in `@BeforeEach`.

Test cases:

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `run_whenNoAdminExists_createsAdminUserWithHashedPassword` | `existsByRole(ADMIN)` → false; `encode(TEST_PASSWORD)` → `"hashed"` | `save(user)` called with `role=ADMIN`, `active=true`, `username=TEST_USERNAME`, `passwordHash="hashed"` | Full seeding logic |
| 2 | `run_whenAdminAlreadyExists_skipsCreation` | `existsByRole(ADMIN)` → true | `save` NEVER called; `encode` NEVER called | Idempotency — no duplicate on second run |
| 3 | `run_savesUserWithNullId_guaranteesInsert` | `existsByRole(ADMIN)` → false | Captured user has `id == null` | `id=null` enforces INSERT, not MERGE |
| 4 | `run_usesUsernameFromConstructor` | `existsByRole(ADMIN)` → false | Captured user has `username == TEST_USERNAME` | Constructor injection, not hardcoded string |
| 5 | `constructor_withNullUserRepository_throwsNullPointerException` | `new AdminUserInitializer(null, ...)` | `NullPointerException`, message `"userRepository must not be null"` | Constructor null guard |
| 6 | `constructor_withNullPasswordHasher_throwsNullPointerException` | `new AdminUserInitializer(..., null, ...)` | `NullPointerException`, message `"passwordHasher must not be null"` | Constructor null guard |

Rules:
- `run()` parameter: pass `mock(ApplicationArguments.class)` — it is never used in the implementation.
- Tests #1, #3, #4: single `ArgumentCaptor<User>` captures `save(...)` argument; assert all fields in one capture.
- Tests #5, #6: `assertThatThrownBy(() -> new AdminUserInitializer(...)).isInstanceOf(NullPointerException.class).hasMessage("...")`.

Exit condition: 6 tests green. No Spring context.

#### 7.4 — JwtServiceTest (additions to existing)

File: `src/test/java/com/fleetpulse/api/infrastructure/security/JwtServiceTest.java` — **add to existing class**

Add these test methods to the existing `JwtServiceTest`:

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| + | `generateRefreshToken_shouldContainOnlySubClaim` | Call `generateRefreshToken(1L)` | Claims have `sub="1"`; claims do NOT have `role` key | Refresh token carries no authorization |
| + | `generateRefreshToken_shouldExpireInSevenDays` | Call `generateRefreshToken(1L)` | `exp - iat == 604800 seconds` | Refresh token TTL contract |
| + | `isTokenValid_withExpiredToken_returnsFalse` | Build expired token manually (exp in past via JJWT builder) | `isTokenValid()` → false | Expiry enforced by validation |
| + | `extractRole_shouldReturnCorrectRole` | `generateAccessToken(1L, "ADMIN")` | `extractRole(token)` → `"ADMIN"` | Role claim extraction |

> For the expired token test: build a token directly with JJWT using `Jwts.builder().expiration(Date.from(Instant.now().minusSeconds(60)))` and sign with `signingKey` already available in the test class.

Exit condition: 4 new methods added and green. Existing 5 methods unchanged and green. Total: 9 methods in `JwtServiceTest`.

#### 7.5 — RefreshTokenJpaAdapterTest

File: `src/test/java/com/fleetpulse/api/infrastructure/adapter/out/persistence/RefreshTokenJpaAdapterTest.java`

```java
@DataJpaTest
@ActiveProfiles("test")   // H2 + ddl-auto=create-drop + Flyway disabled
@Import(RefreshTokenJpaAdapter.class)
class RefreshTokenJpaAdapterTest {

    @Autowired RefreshTokenJpaAdapter adapter;
}
```

> H2 in-memory (`MODE=MySQL`). Consistent with `UserJpaAdapterTest`. Testcontainers MySQL reserved for `@SpringBootTest`.

Test cases:

| # | Method name | Input | Expected | What it proves |
|---|---|---|---|---|
| 1 | `save_thenFindByToken_returnsAllFields` | `RefreshToken("tok", 1L, now+7days, false)` | All fields match on retrieval | Full round-trip mapping |
| 2 | `save_withNullId_generatesIdOnInsert` | Domain `RefreshToken` (no `id` field) | Found after save; no `DataIntegrityViolationException` | `id=null` enforces INSERT |
| 3 | `findByToken_whenNotFound_returnsEmpty` | Unknown token | `Optional.empty()` | Not-found contract |
| 4 | `revokeByToken_setsRevokedTrue` | Save then revoke | `findByToken` returns `revoked=true` | `@Modifying` UPDATE |
| 5 | `revokeByToken_whenTokenNotFound_doesNotThrow` | Non-existent token | No exception | Passive adapter contract |
| 6 | `deleteAllExpired_removesExpiredPreservesValid` | One expired + one valid | Expired gone; valid remains | Bulk delete correctness |
| 7 | `deleteAllExpired_whenAllTokensValid_deletesNothing` | Two valid tokens | Both remain | Bulk delete no-op path |

Rules:
- Use `"tok-" + UUID.randomUUID()` per test for isolation.
- `Instant.now().plusSeconds(604800)` for valid tokens; `Instant.now().minusSeconds(3600)` for expired.
- Do NOT assert `createdAt` — `@PrePersist` concern, not domain.
- Test #5: `assertThatNoException().isThrownBy(...)`.

Exit condition: 7 tests green. No Flyway. No MySQL.

#### 7.6 — JwtAuthenticationFilterTest

File: `src/test/java/com/fleetpulse/api/infrastructure/security/JwtAuthenticationFilterTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")   // loads application-test.properties with JWT config
class JwtAuthenticationFilterTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenService tokenService;    // real JwtService — generates real tokens for test
    @MockBean  TokenBlacklist tokenBlacklist; // control blacklist behavior per test
    @MockBean  AuthUseCase authUseCase;       // prevent real auth calls reaching the DB
    @MockBean  UserManagementUseCase userManagementUseCase;
}
```

> This test validates the **filter chain** in isolation from controller logic. `TokenService` is a real bean (uses `jwt.secret` from `application-test.properties`). `TokenBlacklist` is mocked to control Redis behavior without a real Redis container. The filter behavior under test: token extraction, signature validation, blacklist check, SecurityContext population, fail-closed on Redis down.

Test cases:

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 1 | `requestWithoutAuthorizationHeader_passesChainAsAnonymous` | No header; target is `POST /api/auth/login` (permitAll) | 200 (login endpoint reachable anonymously) | Missing header → anonymous, not 401 |
| 2 | `requestWithValidToken_populatesSecurityContextAndContinues` | Real JWT via `tokenService.generateAccessToken(1L, "USER")`; `isBlacklisted` → false; target `POST /api/auth/logout` | Filter continues; `authUseCase.logout(...)` eventually reached (mock returns normally) | Valid token → SecurityContext set |
| 3 | `requestWithExpiredToken_returns401AndClearsContext` | Expired JWT (built manually with JJWT, exp in past) | 401; SecurityContextHolder cleared | Expired token rejected |
| 4 | `requestWithInvalidSignature_returns401AndClearsContext` | JWT signed with different secret | 401 | Wrong signature rejected |
| 5 | `requestWithBlacklistedToken_returns401AndClearsContext` | Valid JWT; `isBlacklisted` → true | 401 | Blacklisted token rejected |
| 6 | `requestWithMalformedToken_returns401AndClearsContext` | `Authorization: Bearer not-a-jwt` | 401 | Malformed JWT rejected |
| 7 | `requestWhenRedisDown_returns503AndClearsContext` | Valid JWT; `isBlacklisted` throws `ExternalServiceUnavailableException` | 503 (fail-closed — never passes when Redis is unreachable) | Fail-closed contract enforced |

Rules:
- Test #1 targets a `permitAll` endpoint (`/api/auth/login`) — if the filter returns 401 for missing header on a public endpoint, that is a misconfiguration bug.
- Test #7 is the most important security test in this class: Redis down MUST return 503, never allow the request through. If this test fails, the fail-closed guarantee is broken.
- For tests #3 and #4: build tokens directly with JJWT builder using the same `signingKey` derivation as `JwtServiceTest`. Do not call `tokenService` for these — the point is to test what the filter does with bad tokens.
- `@MockBean TokenBlacklist` — configure per test with `when(tokenBlacklist.isBlacklisted(any())).thenReturn(false/true)` or `.thenThrow(ExternalServiceUnavailableException.class)`.
- Do NOT assert on `SecurityContextHolder` state directly in MockMvc tests — infer filter behavior from HTTP response codes and whether the downstream mock was called.

Exit condition: 7 tests green. Test #7 (fail-closed Redis) is non-negotiable — if it does not exist and pass, Layer 7 is not complete.

#### 7.7 — AuthControllerTest

File: `src/test/java/com/fleetpulse/api/infrastructure/adapter/in/web/AuthControllerTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  AuthUseCase authUseCase;   // driving port — never AuthService directly
    @MockBean  TokenBlacklist tokenBlacklist; // prevent real Redis calls during context startup
}
```

Test cases (AAA pattern on every method):

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 1 | `login_withValidCredentials_returns200WithTokens` | `authUseCase.login(any())` returns `AuthResult` | 200, `$.accessToken` and `$.refreshToken` present | Happy path — response contract |
| 2 | `login_withInvalidCredentials_returns401ProblemDetail` | `authUseCase.login(any())` throws `InvalidCredentialsException` | 401, `Content-Type: application/problem+json`, `$.type` ends `/errors/invalid-credentials` | GlobalExceptionHandler mapping |
| 3 | `login_withInactiveUser_returns401WithSameTypeAsInvalidCredentials` | `authUseCase.login(any())` throws `UserNotActiveException` | 401, `$.type` ends `/errors/invalid-credentials` — same URI, reason hidden | Security — reason not revealed |
| 4 | `login_withBlankUsername_returns400ValidationFailed` | `{"username":"","password":"x"}` | 400, `$.type` ends `/errors/validation-failed`, `$.errors.username` present | Jakarta Validation before use case |
| 5 | `login_withMissingBody_returns400` | No body | 400 | Malformed request |
| 6 | `refresh_withValidToken_returns200WithNewPair` | `authUseCase.refresh(any())` returns new `AuthResult` | 200, new token pair in body | Rotation contract |
| 7 | `refresh_withExpiredToken_returns401ProblemDetail` | throws `RefreshTokenExpiredException` | 401, `$.type` ends `/errors/token-expired` | Expiry mapping |
| 8 | `refresh_withRevokedToken_returns401ProblemDetail` | throws `RefreshTokenRevokedException` | 401, `$.type` ends `/errors/token-invalid` | Revocation mapping |
| 9 | `logout_withoutAuthorizationHeader_returns401` | No `Authorization` header, no `@WithMockUser` | 401 (SecurityConfig blocks before controller) | Security filter enforced |
| 10 | `logout_withValidBearerToken_returns204` | `@WithMockUser(authorities = "USER")` + `Authorization: Bearer sometoken` | 204; `authUseCase.logout(eq("sometoken"), ...)` called once | Happy path + token extraction |
| 11 | `logout_withMalformedBearerPrefix_returns401` | `Authorization: Token abc` | 401 (controller defense-in-depth) | Header format validation |

Rules:
- Every error test: `.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))` — mandatory.
- Test #10: `verify(authUseCase).logout(eq("sometoken"), eq(refreshTokenValue))` — confirm `substring(7)` strips `"Bearer "` correctly.
- `@MockBean TokenBlacklist` prevents `JwtAuthenticationFilter` from attempting a real Redis call on startup or during tests where a JWT header is not present. Required for context stability.

Exit condition: 11 tests green. Every error test verifies status code AND content type AND `$.type` URI.

#### 7.8 — ArchUnit Expansion

File: `src/test/java/com/fleetpulse/api/architecture/HexagonalArchitectureTest.java` — **add to existing class**

The existing 4 rules verify import boundaries. Add rules that enforce the 14 prohibitions from `02-hexagonal-rules.md`:

| # | Rule name | What it enforces |
|---|---|---|
| + | `noAutowiredOnFields` | `@Autowired` must not appear on any field — constructor injection only |
| + | `noServiceAnnotationInApplicationLayer` | `@Service` must not appear in `application/` packages |
| + | `noTransactionalInAdapters` | `@Transactional` must not appear in `infrastructure/adapter/` packages |
| + | `dtosMustBeRecords` | All classes in `infrastructure/adapter/in/web/dto/` must be records |
| + | `noImplSuffix` | No class name may end with `Impl` |

Rules:
- Add each as a `static final ArchRule` field with `@ArchTest` annotation — consistent with the existing pattern in `HexagonalArchitectureTest`.
- These rules run at build time. A violation means the build fails. No exceptions.

Exit condition: 5 new rules added and passing. Existing 4 rules unchanged and passing. Total: 9 rules in `HexagonalArchitectureTest`.

#### Blocked States

```
7.1 AuthServiceTest              → UNBLOCKED — Layer 3 complete ✅
7.2 UserManagementServiceTest    → UNBLOCKED — Layer 3 complete ✅
7.3 AdminUserInitializerTest     → UNBLOCKED — Layer 5.7 complete ✅
7.4 JwtServiceTest additions     → UNBLOCKED — JwtService complete ✅
7.5 RefreshTokenJpaAdapterTest   → UNBLOCKED — Layer 4.1 complete ✅
7.6 JwtAuthenticationFilterTest  → BLOCKED by Prerequisites — add application-test.properties entries first
7.7 AuthControllerTest           → BLOCKED by Prerequisites — add application-test.properties entries first
7.8 ArchUnit expansion           → UNBLOCKED — all code complete ✅

7.1 through 7.5 and 7.8 can be implemented in parallel.
7.6 and 7.7 unblock immediately after application-test.properties is updated.
```

#### Test Matrix

| Test Class | Context | DB | Redis | JWT | Mockito | Order |
|---|---|---|---|---|---|---|
| `AuthServiceTest` | None | None | None | None | `@InjectMocks` | 1st |
| `UserManagementServiceTest` | None | None | None | None | `@InjectMocks` | 2nd |
| `AdminUserInitializerTest` | None | None | None | None | `@Mock` + manual | 3rd |
| `JwtServiceTest` additions | None | None | None | Real `JwtService` | None | 4th |
| `RefreshTokenJpaAdapterTest` | `@DataJpaTest` | H2 | None | None | None | 5th |
| `JwtAuthenticationFilterTest` | `@SpringBootTest` | None | `@MockBean` | Real `TokenService` | `@MockBean` | 6th |
| `AuthControllerTest` | `@SpringBootTest` | None | `@MockBean` | `@MockBean AuthUseCase` | `@MockBean` | 7th |
| `HexagonalArchitectureTest` additions | None | None | None | None | None | 8th |

#### Note — FullAuthFlowTest (deferred)

A full E2E smoke test with zero mocks (real MySQL + real Redis + real JWT + real BCrypt + Flyway migrations) would require a shared Testcontainers base class, JWT config in test properties, and test data isolation strategy. The individual tests in 7.1–7.8 already cover every layer in isolation with sufficient depth to catch integration bugs before Phase 4. The E2E test adds value in a staging environment with real infrastructure — defer to **Phase 5**, where MySQL + Redis are already running and the full dispatch cycle needs validation.

---

## Phase 4 — SOAP Dispatch Engine ⚠️ Highest Risk
**Tag:** `v0.4.0`
**Exit condition:** Force-dispatch endpoint triggers a confirmed live dispatch to QSolutions (`Protocolo.isProcessed() == true`). Automated 15-minute scheduler fires and dispatches all configured units. All skip conditions logged correctly. All component tests pass. Auditable `Protocolo` response pasted in GitHub release notes before tagging.

> ⚠️ **LIVE EXTERNAL SERVICE.** No sandbox. Every broken payload hits production QSolutions. Do not ship until all unit tests are green and the `@Disabled` live integration test is manually verified.

### Layer 1 — Configuration: Properties + WSDL Bootstrap

> **Must be done first.** Every downstream layer depends on the `@Value` bindings and the `ReceiveGPSInfoSoap` @Bean declared here.

#### 1.1 — New `application.properties` entries

Add these properties. Existing QSolutions properties (`username`, `password`, `proveedor`, `tracking-number`) are already present — do not modify them.

```properties
# QSolutions SOAP — runtime endpoint + timeouts (add after existing qsolutions.* block)
qsolutions.endpoint=${QSOLUTIONS_ENDPOINT:https://qintegrator.qs3.com.mx/ReceiveGPSInformation/ReceiveGPSInfo.asmx}
qsolutions.connect-timeout-ms=${QSOLUTIONS_CONNECT_TIMEOUT_MS:10000}
qsolutions.read-timeout-ms=${QSOLUTIONS_READ_TIMEOUT_MS:30000}

# Manual GPS coordinates — per-unit activation map
# A unit present here is ACTIVE for dispatch. Remove or comment out to deactivate without DB changes.
# Format: gps.manual.units.{numUnidad}.lat / .lon
gps.manual.units.Peugeot.lat=${GPS_PEUGEOT_LAT:19.4326}
gps.manual.units.Peugeot.lon=${GPS_PEUGEOT_LON:-99.1332}
gps.manual.units.Kangoo.lat=${GPS_KANGOO_LAT:19.4326}
gps.manual.units.Kangoo.lon=${GPS_KANGOO_LON:-99.1332}
gps.manual.units.Tr-02.lat=${GPS_TR02_LAT:19.4326}
gps.manual.units.Tr-02.lon=${GPS_TR02_LON:-99.1332}
gps.manual.units.Attitude.lat=${GPS_ATTITUDE_LAT:19.4326}
gps.manual.units.Attitude.lon=${GPS_ATTITUDE_LON:-99.1332}
gps.manual.units.Sentra.lat=${GPS_SENTRA_LAT:19.4326}
gps.manual.units.Sentra.lon=${GPS_SENTRA_LON:-99.1332}

# GPS provider selection — manual (Phase 4) | traccar (Phase 6+)
gps.provider=${GPS_PROVIDER:manual}

# Pulse scheduler — 15 minutes = 900 000 ms
scheduler.pulse.interval-ms=${SCHEDULER_PULSE_INTERVAL_MS:900000}
```

Add the same env vars to `.env.example` and `.env`.

#### 1.2 — `ManualCoordinateProperties`

File: `infrastructure/config/ManualCoordinateProperties.java`

```java
@ConfigurationProperties(prefix = "gps.manual")
public class ManualCoordinateProperties {
    private Map<String, UnitCoordinate> units = new LinkedHashMap<>();

    public record UnitCoordinate(BigDecimal lat, BigDecimal lon) {}

    public Map<String, UnitCoordinate> getUnits() { return units; }
    public void setUnits(Map<String, UnitCoordinate> units) { this.units = units; }
}
```

Registered via `@EnableConfigurationProperties(ManualCoordinateProperties.class)` in `GpsProviderConfig` (Layer 4).

#### 1.3 — WSDL classpath fix in `ApplicationConfig`

The generated `ReceiveGPSInfo.java` static initializer hardcodes `file:/C:/Dev/...`. This path is a developer-machine artefact and will fail in every other environment.

**Fix:** instantiate the service with the classpath URL constructor and override the endpoint at runtime via `BindingProvider`. Add to `ApplicationConfig`:

```java
@Bean
public ReceiveGPSInfoSoap receiveGpsInfoSoap(
        @Value("${qsolutions.endpoint}") String endpoint,
        @Value("${qsolutions.connect-timeout-ms}") int connectTimeoutMs,
        @Value("${qsolutions.read-timeout-ms}") int readTimeoutMs) {
    URL wsdlUrl = ReceiveGPSInfo.class.getResource("/wsdl/ReceiveGPSInfo.wsdl"); // classpath, not file://
    ReceiveGPSInfo service = new ReceiveGPSInfo(wsdlUrl);
    ReceiveGPSInfoSoap port = service.getReceiveGPSInfoSoap();

    BindingProvider bp = (BindingProvider) port;
    bp.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, endpoint);
    bp.getRequestContext().put("com.sun.xml.ws.connect.timeout", connectTimeoutMs);
    bp.getRequestContext().put("com.sun.xml.ws.request.timeout", readTimeoutMs);

    return port;
}
```

Rules:
- `ReceiveGPSInfo.class.getResource("/wsdl/ReceiveGPSInfo.wsdl")` — leading `/` means classpath root. WSDL in `src/main/resources/wsdl/` is on the classpath at runtime.
- The `ReceiveGPSInfoSoap` bean is a single instance. JAX-WS port proxies are NOT thread-safe — if concurrent scheduling is introduced in Phase 8+, revisit with `ThreadLocal` or prototype scope.
- Timeouts bind as `int`. `@Value("${qsolutions.connect-timeout-ms}") int` resolves correctly from properties.

#### 1.4 — `UnitRepository` port addition

Add to `application/port/out/UnitRepository.java`:

```java
List<String> findAllActiveNumUnidades();
```

Add to `UnitJpaRepository`:

```java
@Query("SELECT u.numUnidad FROM UnitEntity u WHERE u.active = true")
List<String> findAllActiveNumUnidades();
```

Add to `UnitJpaAdapter` — delegates to the Spring Data method.

| Status | Task |
|---|---|
| ⬜ | Add 4 property blocks to `application.properties` (SOAP timeouts, manual GPS map, provider selector, scheduler interval) |
| ⬜ | Add same env vars to `.env.example` and `.env` |
| ⬜ | Create `ManualCoordinateProperties.java` |
| ⬜ | Add `receiveGpsInfoSoap()` @Bean to `ApplicationConfig` — classpath WSDL + endpoint override + timeouts |
| ⬜ | Add `findAllActiveNumUnidades()` to `UnitRepository` port + `UnitJpaAdapter` + `UnitJpaRepository` |
| ⬜ | `mvn test` — ArchUnit passes, no `@Value` binding errors |

Exit condition: Application context starts without errors. `receiveGpsInfoSoap()` resolves from classpath WSDL (no network call at startup). `ManualCoordinateProperties` binds correctly for all 5 units. `findAllActiveNumUnidades()` method compiles and exists in port interface.

---

### Layer 2 — QSolutionsSoapAdapter

> ⛔ Cannot begin until Layer 1 complete — depends on `ReceiveGPSInfoSoap` @Bean and `@Value` credential properties.

File: `infrastructure/adapter/out/soap/QSolutionsSoapAdapter.java`

Implements: `PulseSender` (`application/port/out/PulseSender.java`)

Declared as `@Bean` in `ApplicationConfig`. No `@Component`.

#### GPSInfo field mapping

| GPSInfo field | Source | Required? | Notes |
|---|---|---|---|
| `Latitud` | `pulse.getGpsReading().getLatitud()` | Yes | `BigDecimal` — already validated by `GpsReading` constructor |
| `Longitud` | `pulse.getGpsReading().getLongitud()` | Yes | `BigDecimal` |
| `NumUnidad` | `pulse.getGpsReading().getNumUnidad()` | Optional | String |
| `Username` | `@Value("${qsolutions.username}")` | Optional | Never log |
| `Password` | `@Value("${qsolutions.password}")` | Optional | Never log — not even at DEBUG |
| `Proveedor` | `@Value("${qsolutions.proveedor}")` | Optional | `"Digi-Haul"` in production |
| `Trackingnumber` | `pulse.getEffectiveTrackingNumber()` | Optional | Resolved upstream by orchestration service |
| `FechaHoraEvento` | `pulse.getGpsReading().getFechaHoraEvento()` | Yes | `ZonedDateTime` → `XMLGregorianCalendar` via `toXmlCalendar()` |
| `FechaRecepcion` | `pulse.getFechaRecepcion()` | Yes | Same conversion |
| `Velocidad`, `Placas`, `Ruta`, `BOL`, `Observaciones` | `null` | — | Not used in this integration |

#### XMLGregorianCalendar conversion — FLEET_TIMEZONE explicit

```java
private static final DatatypeFactory DATATYPE_FACTORY;

static {
    try {
        DATATYPE_FACTORY = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
        throw new ExceptionInInitializerError(e);
    }
}

private XMLGregorianCalendar toXmlCalendar(ZonedDateTime zdt) {
    ZonedDateTime atFleet = zdt.withZoneSameInstant(FLEET_TIMEZONE);
    GregorianCalendar gc = GregorianCalendar.from(atFleet);
    return DATATYPE_FACTORY.newXMLGregorianCalendar(gc);
}
```

`DatatypeFactory` initialized once in a `static` block — avoids per-call instantiation.

#### Exception contract

| Outcome | Action |
|---|---|
| `Protocolo.isProcessed() == true` | Log `INFO "PULSE_SENT"` with `numUnidad`, `effectiveTracking`, `receptionDate` |
| `Protocolo.isProcessed() == false` | Log `WARN "PULSE_REJECTED"` with `getMessage()`, throw `PulseSendException` |
| `WebServiceException` (network / timeout) | Log `ERROR "PULSE_ERROR"` with reason, throw `GpsProviderUnavailableException` |

Catch only the two specific cases above. All other exceptions propagate unchanged — they indicate programming errors, not expected runtime failures.

| Status | Task |
|---|---|
| ⬜ | Create `QSolutionsSoapAdapter.java` implementing `PulseSender` |
| ⬜ | `send(ScheduledPulse)` maps all 8 required `GPSInfo` fields |
| ⬜ | `toXmlCalendar(ZonedDateTime)` uses `FLEET_TIMEZONE` with static `DatatypeFactory` |
| ⬜ | `Processed=false` → `PulseSendException`; `WebServiceException` → `GpsProviderUnavailableException` |
| ⬜ | `password` field never appears in any log output |
| ⬜ | Declare `QSolutionsSoapAdapter` as `@Bean` in `ApplicationConfig` — inject `ReceiveGPSInfoSoap` port + `@Value` credentials |
| ⬜ | `mvn test` — ArchUnit passes (no `@Component`, no Spring imports in adapter) |

Exit condition: Adapter compiles. ArchUnit passes. Credentials never in source code. `PulseSendException` and `GpsProviderUnavailableException` thrown in correct cases (verified in 8.1 unit tests).

---

### Layer 3 — ManualCoordinateAdapter

> ⛔ Cannot begin until Layer 1 complete — depends on `ManualCoordinateProperties`.

File: `infrastructure/adapter/out/gps/ManualCoordinateAdapter.java`

Implements: `GpsCoordinateProvider` (`application/port/out/GpsCoordinateProvider.java`)

Declared as `@Bean` in `GpsProviderConfig` (Layer 4). No `@Component`.

#### Per-unit activation pattern — zero DB queries

```
isAvailable(numUnidad):
  O(1) map lookup — immutable Map<String, UnitCoordinate> built once at startup
  Returns true  if gps.manual.units.{numUnidad} is present in the properties map
  Returns false if absent — no DB, no network, no exceptions

getCoordinates(numUnidad):
  Returns a NEW GpsReading each call (fresh ZonedDateTime.now(FLEET_TIMEZONE) as fechaHoraEvento)
  Throws GpsProviderUnavailableException if numUnidad not in map (defensive — isAvailable should be called first)
```

**Activate a unit:** add `gps.manual.units.{numUnidad}.lat/lon` to properties and restart.
**Deactivate a unit:** remove its entry. `isAvailable` returns false → orchestration service logs `SKIPPED_NO_COORDINATES` and skips — zero DB writes.

#### Implementation

```java
public class ManualCoordinateAdapter implements GpsCoordinateProvider {

    private final Map<String, ManualCoordinateProperties.UnitCoordinate> coordinates;

    public ManualCoordinateAdapter(ManualCoordinateProperties props) {
        Objects.requireNonNull(props, "props must not be null");
        this.coordinates = Collections.unmodifiableMap(props.getUnits());
    }

    @Override
    public boolean isAvailable(String numUnidad) {
        return coordinates.containsKey(numUnidad);
    }

    @Override
    public GpsReading getCoordinates(String numUnidad) throws GpsProviderUnavailableException {
        ManualCoordinateProperties.UnitCoordinate coord = coordinates.get(numUnidad);
        if (coord == null) {
            throw new GpsProviderUnavailableException("No manual coordinates for unit: " + numUnidad);
        }
        return new GpsReading(numUnidad, coord.lat(), coord.lon(),
                              ZonedDateTime.now(FLEET_TIMEZONE), ProviderType.MANUAL);
    }
}
```

`GpsReading` constructor validates coordinates — out-of-range values in properties throw `InvalidCoordinateException` on first call. Acceptable for Phase 4 since values are operator-controlled via env vars.

| Status | Task |
|---|---|
| ⬜ | Create `ManualCoordinateAdapter.java` implementing `GpsCoordinateProvider` |
| ⬜ | Constructor builds `Collections.unmodifiableMap` from `ManualCoordinateProperties` |
| ⬜ | `isAvailable` — O(1) `containsKey`, zero I/O |
| ⬜ | `getCoordinates` — returns fresh `GpsReading` with `ZonedDateTime.now(FLEET_TIMEZONE)` and `ProviderType.MANUAL` |
| ⬜ | `mvn test` — ArchUnit passes |

Exit condition: Adapter compiles. `isAvailable` returns correct boolean for configured and unconfigured units without any external calls (verified in 8.2 unit tests).

---

### Layer 4 — GpsProviderConfig

> ⛔ Cannot begin until Layer 3 complete — registers the `GpsCoordinateProvider` @Bean.

File: `infrastructure/config/GpsProviderConfig.java`

```java
@Configuration
@EnableConfigurationProperties(ManualCoordinateProperties.class)
public class GpsProviderConfig {

    @Bean
    @ConditionalOnProperty(name = "gps.provider", havingValue = "manual", matchIfMissing = true)
    public GpsCoordinateProvider manualCoordinateAdapter(ManualCoordinateProperties props) {
        return new ManualCoordinateAdapter(props);
    }

    // Phase 6 will add:
    // @Bean
    // @ConditionalOnProperty(name = "gps.provider", havingValue = "traccar")
    // public GpsCoordinateProvider traccarCoordinateAdapter(GpsPositionCache cache) { ... }
}
```

`matchIfMissing = true` — `MANUAL` is the default when `gps.provider` is absent. Setting `GPS_PROVIDER=traccar` in Phase 6 swaps the implementation without changing any application code.

| Status | Task |
|---|---|
| ⬜ | Create `GpsProviderConfig.java` with `@ConditionalOnProperty(matchIfMissing = true)` |
| ⬜ | `@EnableConfigurationProperties(ManualCoordinateProperties.class)` registered here |
| ⬜ | Verify exactly one `GpsCoordinateProvider` bean resolves in `@SpringBootTest` context |

Exit condition: Context starts. Exactly one `GpsCoordinateProvider` bean — `ManualCoordinateAdapter`. ArchUnit passes.

---

### Layer 5 — PulseOrchestrationService

> ⛔ Cannot begin until Layer 4 complete — depends on `GpsCoordinateProvider` and `PulseSender` beans.

File: `application/service/PulseOrchestrationService.java`

Implements: `SendPulseUseCase` (`application/port/in/SendPulseUseCase.java`)

Declared as `@Bean` in `ApplicationConfig`. No `@Service`. No `@Transactional` — read-then-delegate path with no DB mutations.

#### Dispatch algorithm

```
sendPulse(String numUnidad):

  1. unit = unitRepository.findByNumUnidad(numUnidad)
             .orElseThrow(() -> new UnitNotFoundException("Unit not found: " + numUnidad))

  2. if (!unit.isActive()):
         log.info("SKIPPED_UNIT_INACTIVE", kv("numUnidad", numUnidad))
         return

  3. if (!unit.isWithinActiveWindow(LocalTime.now(FLEET_TIMEZONE))):
         log.info("SKIPPED_OUT_OF_WINDOW", kv("numUnidad", numUnidad),
                  kv("window", unit.getHoraInicio() + "-" + unit.getHoraFin()))
         return

  4. if (!gpsProvider.isAvailable(numUnidad)):
         log.info("SKIPPED_NO_COORDINATES", kv("numUnidad", numUnidad))
         return

  5. GpsReading reading           = gpsProvider.getCoordinates(numUnidad)
  6. ZonedDateTime fechaRecepcion = ZonedDateTime.now(FLEET_TIMEZONE)
  7. String effectiveTracking     = (unit.getTrackingNumber() != null)
                                        ? unit.getTrackingNumber()
                                        : defaultTrackingNumber   // from @Value qsolutions.tracking-number

  8. pulseSender.send(new ScheduledPulse(unit, reading, fechaRecepcion, effectiveTracking))
     — PulseSendException propagates → GlobalExceptionHandler maps to 502
     — GpsProviderUnavailableException propagates → GlobalExceptionHandler maps to 503
```

#### Constructor

```java
public PulseOrchestrationService(
        UnitRepository unitRepository,
        GpsCoordinateProvider gpsProvider,
        PulseSender pulseSender,
        @Value("${qsolutions.tracking-number}") String defaultTrackingNumber) {
    this.unitRepository        = Objects.requireNonNull(unitRepository);
    this.gpsProvider           = Objects.requireNonNull(gpsProvider);
    this.pulseSender           = Objects.requireNonNull(pulseSender);
    this.defaultTrackingNumber = Objects.requireNonNull(defaultTrackingNumber);
}
```

Declare in `ApplicationConfig`:

```java
@Bean
public PulseOrchestrationService pulseOrchestrationService(
        UnitRepository unitRepository,
        GpsCoordinateProvider gpsProvider,
        PulseSender pulseSender,
        @Value("${qsolutions.tracking-number}") String defaultTrackingNumber) {
    return new PulseOrchestrationService(unitRepository, gpsProvider, pulseSender, defaultTrackingNumber);
}
```

#### Skip condition log keys — stable, searchable in production

| Condition | Log key | Level |
|---|---|---|
| Unit `active = false` in DB | `SKIPPED_UNIT_INACTIVE` | INFO |
| Outside `horaInicio–horaFin` | `SKIPPED_OUT_OF_WINDOW` | INFO |
| Unit absent from `gps.manual.units.*` | `SKIPPED_NO_COORDINATES` | INFO |

| Status | Task |
|---|---|
| ⬜ | Create `PulseOrchestrationService.java` implementing `SendPulseUseCase` |
| ⬜ | All 3 skip conditions logged with stable structured log keys |
| ⬜ | `effectiveTrackingNumber` resolves: `unit.getTrackingNumber() != null` → unit value, else `defaultTrackingNumber` |
| ⬜ | `PulseSendException` and `GpsProviderUnavailableException` propagate unchanged to caller |
| ⬜ | Declare `pulseOrchestrationService()` @Bean in `ApplicationConfig` |
| ⬜ | `mvn test` — ArchUnit passes (no `@Service`, no `@Transactional`, no Spring imports in `application/`) |

Exit condition: Service compiles. ArchUnit passes. All skip conditions and dispatch paths covered by 8.3 unit tests with zero Spring context.

---

### Layer 6 — PulseSchedulerService + SchedulerConfig

> ⛔ Cannot begin until Layer 5 complete — consumes `SendPulseUseCase` port.

**`SchedulerConfig.java`** — `infrastructure/config/SchedulerConfig.java`

```java
@Configuration
@EnableScheduling
public class SchedulerConfig {

    @Bean
    public PulseSchedulerService pulseSchedulerService(
            SendPulseUseCase sendPulseUseCase,
            UnitRepository unitRepository) {
        return new PulseSchedulerService(sendPulseUseCase, unitRepository);
    }
}
```

`@EnableScheduling` activates Spring's `@Scheduled` post-processor. `PulseSchedulerService` is declared as `@Bean` here — no `@Component` needed — consistent with ADR-009.

**`PulseSchedulerService.java`** — `infrastructure/scheduler/PulseSchedulerService.java`

```java
public class PulseSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(PulseSchedulerService.class);

    private final SendPulseUseCase sendPulseUseCase;
    private final UnitRepository unitRepository;

    public PulseSchedulerService(SendPulseUseCase sendPulseUseCase, UnitRepository unitRepository) {
        this.sendPulseUseCase = Objects.requireNonNull(sendPulseUseCase);
        this.unitRepository   = Objects.requireNonNull(unitRepository);
    }

    @Scheduled(fixedRateString = "${scheduler.pulse.interval-ms}")
    public void dispatchAllActiveUnits() {
        List<String> activeUnits = unitRepository.findAllActiveNumUnidades();
        log.info("SCHEDULER_TICK_START", kv("units", activeUnits.size()));

        for (String numUnidad : activeUnits) {
            try {
                sendPulseUseCase.sendPulse(numUnidad);
            } catch (Exception e) {
                log.error("SCHEDULER_UNIT_FAILED",
                    kv("numUnidad", numUnidad), kv("reason", e.getMessage()));
                // Failure of one unit MUST NOT abort the cycle for remaining units
            }
        }

        log.info("SCHEDULER_TICK_END", kv("units", activeUnits.size()));
    }
}
```

Rules:
- `fixedRateString` (not `fixedRate`) — allows `scheduler.pulse.interval-ms=100` in integration tests to avoid 15-minute waits
- Per-unit `catch (Exception e)` is intentional — a SOAP rejection on "Peugeot" must not prevent "Kangoo" from dispatching in the same tick
- `unitRepository.findAllActiveNumUnidades()` — returns only `String` values, no full entity load
- `SCHEDULER_TICK_START` / `SCHEDULER_TICK_END` are stable structured log keys for monitoring and alerting

| Status | Task |
|---|---|
| ⬜ | Create `SchedulerConfig.java` with `@EnableScheduling` and `pulseSchedulerService()` @Bean |
| ⬜ | Create `PulseSchedulerService.java` with `@Scheduled(fixedRateString)` dispatch method |
| ⬜ | Per-unit exception catch — failure of one unit never aborts the cycle |
| ⬜ | `SCHEDULER_TICK_START` and `SCHEDULER_TICK_END` logged with unit count |
| ⬜ | No `@Component` on `PulseSchedulerService` — `mvn test` ArchUnit passes |

Exit condition: Scheduler fires on configured interval. Per-unit failure does not abort the cycle. `SCHEDULER_TICK_END` logged after every tick.

---

### Layer 7 — PulseController

> ⛔ Cannot begin until Layer 5 complete — injects `SendPulseUseCase` driving port.

File: `infrastructure/adapter/in/web/PulseController.java`

```
POST /api/units/{numUnidad}/pulse/force
  Authority: ADMIN or USER
  Path var:  numUnidad (String)
  Body:      none
  204 No Content  — dispatch confirmed by QSolutions
  401             — missing or invalid token
  403             — valid token, insufficient authority
  404             — unit not found (UnitNotFoundException → GlobalExceptionHandler)
  502             — QSolutions rejected (PulseSendException → GlobalExceptionHandler)
  503             — QSolutions unreachable (GpsProviderUnavailableException → GlobalExceptionHandler)
```

**Add to `SecurityConfig` authorization matrix** (before `anyRequest().denyAll()`):

```java
.requestMatchers(HttpMethod.POST, "/api/units/*/pulse/force").hasAnyAuthority("ADMIN", "USER")
```

**Controller:**

```java
@RestController
@RequestMapping("/api/units")
@Tag(name = "Pulse Dispatch", description = "Force and schedule GPS pulse dispatch to QSolutions")
public class PulseController {

    private final SendPulseUseCase sendPulseUseCase;

    public PulseController(SendPulseUseCase sendPulseUseCase) {
        this.sendPulseUseCase = Objects.requireNonNull(sendPulseUseCase);
    }

    @PostMapping("/{numUnidad}/pulse/force")
    @Operation(summary = "Force-dispatch GPS pulse for a single unit")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pulse dispatched and confirmed"),
        @ApiResponse(responseCode = "404", description = "Unit not found"),
        @ApiResponse(responseCode = "502", description = "QSolutions rejected the pulse"),
        @ApiResponse(responseCode = "503", description = "QSolutions unreachable")
    })
    public ResponseEntity<Void> forceDispatch(@PathVariable String numUnidad) {
        sendPulseUseCase.sendPulse(numUnidad);
        return ResponseEntity.noContent().build();
    }
}
```

Rules:
- `GlobalExceptionHandler` already maps `UnitNotFoundException` → 404, `PulseSendException` → 502, `GpsProviderUnavailableException` → 503 (registered in Phase 3 Layer 5). No new handler needed.
- Authorization delegated to `SecurityConfig` matrix — no `@PreAuthorize` on the method. Centralized authorization prevents drift (ADR-008).
- Inject `SendPulseUseCase` driving port, never `PulseOrchestrationService` directly.

| Status | Task |
|---|---|
| ⬜ | Create `PulseController.java` with `POST /{numUnidad}/pulse/force` |
| ⬜ | Add authorization rule to `SecurityConfig` matrix |
| ⬜ | `@Tag` + `@Operation` + `@ApiResponse` for 204, 404, 502, 503 |
| ⬜ | `mvn test` — ArchUnit passes |

Exit condition: Endpoint returns 204 with valid JWT. Returns 401 without token. Returns 404/502/503 on respective errors. OpenAPI spec includes this endpoint.

---

### Layer 8 — Tests

**Exit condition for Phase 4:** `mvn test` passes with zero failures. All 5 component tests green. `@Disabled` live integration test run manually with `Protocolo.isProcessed() == true` evidence in release notes.

#### Dependency Rules

- All unit tests (8.1, 8.2, 8.3): no Spring context, no I/O
- Controller test (8.4): `@SpringBootTest` + MockMvc + `@MockitoBean SendPulseUseCase`
- AAA pattern (`// Arrange`, `// Act`, `// Assert`) mandatory in every test method
- AssertJ (`assertThat`) for all assertions
- `ArgumentCaptor` to verify the exact `GPSInfo` passed to the SOAP port
- `verify(..., never())` to assert skip conditions did NOT invoke `pulseSender.send()`

#### Component Map

| Order | Component | File | Test Type | Depends On | Status |
|---|---|---|---|---|---|
| 8.1 | QSolutionsSoapAdapterTest | `QSolutionsSoapAdapterTest.java` | Unit + @Disabled live | Layer 2 | ⬜ |
| 8.2 | ManualCoordinateAdapterTest | `ManualCoordinateAdapterTest.java` | Unit | Layer 3 | ⬜ |
| 8.3 | PulseOrchestrationServiceTest | `PulseOrchestrationServiceTest.java` | Unit (Mockito) | Layer 5 | ⬜ |
| 8.4 | PulseControllerTest | `PulseControllerTest.java` | Controller integration | Layer 7 | ⬜ |
| 8.5 | ArchUnit additions | `HexagonalArchitectureTest.java` | Architecture | All layers | ⬜ |

#### 8.1 — QSolutionsSoapAdapterTest

```java
@ExtendWith(MockitoExtension.class)
class QSolutionsSoapAdapterTest {

    @Mock ReceiveGPSInfoSoap soapPort;

    private QSolutionsSoapAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new QSolutionsSoapAdapter(soapPort, "test-user", "test-pass", "test-proveedor");
    }
}
```

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `send_whenProcessedTrue_completesWithoutException` | Port returns `Protocolo(processed=true)` | No exception | Happy path |
| 2 | `send_whenProcessedFalse_throwsPulseSendException` | Port returns `Protocolo(processed=false, message="ERR")` | `PulseSendException`; message contains `"ERR"` | Rejection mapped correctly |
| 3 | `send_whenWebServiceException_throwsGpsProviderUnavailableException` | Port throws `WebServiceException` | `GpsProviderUnavailableException` | Network failure mapped correctly |
| 4 | `send_mapsAllRequiredGpsInfoFields` | `ArgumentCaptor<GPSInfo>` | `latitud`, `longitud`, `numUnidad`, `username`, `proveedor`, `trackingnumber`, `fechaHoraEvento`, `fechaRecepcion` all set | Field mapping completeness |
| 5 | `toXmlCalendar_usesFleetTimezone_notUtc` | Convert `ZonedDateTime` at UTC midnight | `XMLGregorianCalendar` hours/minutes match `America/Mexico_City` offset | FLEET_TIMEZONE enforcement |
| 6 | `send_doesNotLogPassword` | Happy path | No captured log record contains the password value | Credential leak prevention |

```java
// Live integration gate — run manually, do not leave enabled
@Test
@Disabled("LIVE — requires QSolutions network access. Run manually before tagging v0.4.0. Paste Protocolo response in release notes.")
void send_liveQSolutionsIntegration_returnsProcessedTrue() {
    // Use real ReceiveGPSInfoSoap from Spring context (requires @SpringBootTest or manual wiring)
    // Build ScheduledPulse with Peugeot + valid Mexico City coordinates
    // Call adapter.send(pulse)
    // Assert protocolo.isProcessed() == true
    // Print full Protocolo (receptionDate, message, processed) to stdout for auditable evidence
}
```

#### 8.2 — ManualCoordinateAdapterTest

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `isAvailable_whenUnitConfigured_returnsTrue` | Props contains `"Peugeot"` | `true` | Configured unit activates |
| 2 | `isAvailable_whenUnitAbsent_returnsFalse` | Props does not contain `"Unknown"` | `false` | Unconfigured unit skipped silently |
| 3 | `getCoordinates_returnsReadingWithCorrectFields` | `"Peugeot"` → lat=19.4326, lon=-99.1332 | `getNumUnidad()=="Peugeot"`, `getLatitud()==19.4326`, `getProviderType()==MANUAL` | Field mapping |
| 4 | `getCoordinates_fechaHoraEventoIsCurrentTime` | Any configured unit | `fechaHoraEvento` within 5 seconds of `ZonedDateTime.now(FLEET_TIMEZONE)` | Timestamp is dispatch time |
| 5 | `getCoordinates_whenUnitAbsent_throwsGpsProviderUnavailableException` | Unconfigured unit | `GpsProviderUnavailableException` | Defensive guard |
| 6 | `isAvailable_forAllFiveFleetUnits_returnsTrue` | Props with all 5 `numUnidad` values | All return `true` | Full fleet configured |

#### 8.3 — PulseOrchestrationServiceTest

```java
@ExtendWith(MockitoExtension.class)
class PulseOrchestrationServiceTest {

    @Mock UnitRepository unitRepository;
    @Mock GpsCoordinateProvider gpsProvider;
    @Mock PulseSender pulseSender;

    private PulseOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new PulseOrchestrationService(unitRepository, gpsProvider, pulseSender, "1");
    }

    // Builds an active unit with a 00:00–23:59 window (always in-window)
    private Unit activeUnit(String numUnidad) {
        return new Unit(numUnidad, false, LocalTime.MIN, LocalTime.MAX, null, true);
    }
}
```

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `sendPulse_happyPath_callsPulseSenderWithCorrectPulse` | Active unit, in window, coords available | `pulseSender.send(capture)` called; `capture.getUnit().getNumUnidad() == "Peugeot"` | Full dispatch path |
| 2 | `sendPulse_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException`; `pulseSender.send` NEVER called | Not-found guard |
| 3 | `sendPulse_whenUnitInactive_skipsDispatch` | `unit.isActive() == false` | `pulseSender.send` NEVER called | `SKIPPED_UNIT_INACTIVE` path |
| 4 | `sendPulse_whenOutsideActiveWindow_skipsDispatch` | `isWithinActiveWindow` → false | `pulseSender.send` NEVER called | `SKIPPED_OUT_OF_WINDOW` path |
| 5 | `sendPulse_whenCoordinatesUnavailable_skipsDispatch` | `gpsProvider.isAvailable` → false | `pulseSender.send` and `getCoordinates` NEVER called | `SKIPPED_NO_COORDINATES` path |
| 6 | `sendPulse_usesUnitTrackingNumberWhenSet` | `unit.getTrackingNumber() == "custom-99"` | `capture.getEffectiveTrackingNumber() == "custom-99"` | Per-unit tracking override |
| 7 | `sendPulse_usesDefaultTrackingNumberWhenUnitHasNone` | `unit.getTrackingNumber() == null` | `capture.getEffectiveTrackingNumber() == "1"` | Default fallback |
| 8 | `sendPulse_propagatesPulseSendException` | `pulseSender.send()` throws `PulseSendException` | Exception propagates to caller (maps to 502) | Error propagation |
| 9 | `sendPulse_propagatesGpsProviderUnavailableException` | `gpsProvider.getCoordinates()` throws `GpsProviderUnavailableException` | Exception propagates (maps to 503) | Error propagation |

Tests #3, #4, #5: `verify(pulseSender, never()).send(any())` — mandatory assertion.

#### 8.4 — PulseControllerTest

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PulseControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenService tokenService;
    @MockitoBean TokenBlacklist tokenBlacklist;
    @MockitoBean SendPulseUseCase sendPulseUseCase;
}
```

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 1 | `forceDispatch_withAdminToken_returns204` | Admin JWT; use case → void | 204 | ADMIN authorized |
| 2 | `forceDispatch_withUserToken_returns204` | User JWT; use case → void | 204 | USER authorized |
| 3 | `forceDispatch_withoutToken_returns401` | No `Authorization` header | 401 | Anonymous blocked |
| 4 | `forceDispatch_whenUnitNotFound_returns404WithProblemDetail` | throws `UnitNotFoundException` | 404; `application/problem+json`; `$.type` ends `/errors/unit-not-found` | GlobalExceptionHandler 404 mapping |
| 5 | `forceDispatch_whenQSolutionsRejects_returns502WithProblemDetail` | throws `PulseSendException` | 502; `application/problem+json`; `$.type` ends `/errors/soap-rejected` | GlobalExceptionHandler 502 mapping |
| 6 | `forceDispatch_whenQSolutionsUnreachable_returns503WithProblemDetail` | throws `GpsProviderUnavailableException` | 503; `application/problem+json`; `$.type` ends `/errors/service-unavailable` | GlobalExceptionHandler 503 mapping |

Tests #4, #5, #6: `.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))` mandatory.

#### 8.5 — ArchUnit additions

File: `src/test/java/com/fleetpulse/api/architecture/HexagonalArchitectureTest.java` — add to existing class.

| Rule | What it enforces |
|---|---|
| `noClassInSchedulerPackageAnnotatedWithComponent` | `infrastructure/scheduler/` classes must not use `@Component` — declared as `@Bean` in `SchedulerConfig` |
| `noSoapAdapterAnnotatedWithComponent` | `infrastructure/adapter/out/soap/` must not use `@Component` |
| `noGpsAdapterAnnotatedWithComponent` | `infrastructure/adapter/out/gps/` must not use `@Component` |

#### Blocked States

```
8.1 QSolutionsSoapAdapterTest     → UNBLOCKED after Layer 2
8.2 ManualCoordinateAdapterTest   → UNBLOCKED after Layer 3
8.3 PulseOrchestrationServiceTest → UNBLOCKED after Layer 5
8.4 PulseControllerTest           → UNBLOCKED after Layer 7
8.5 ArchUnit additions            → UNBLOCKED after all layers complete
```

#### Live Integration Gate (mandatory before v0.4.0 tag)

1. Ensure `.env` has live QSolutions credentials (`QSOLUTIONS_USERNAME`, `QSOLUTIONS_PASSWORD`, `QSOLUTIONS_PROVEEDOR`)
2. Remove `@Disabled` from `QSolutionsSoapAdapterTest.send_liveQSolutionsIntegration_returnsProcessedTrue`
3. Run with Spring context wired to the real `ReceiveGPSInfoSoap` bean
4. Assert `protocolo.isProcessed() == true`
5. Copy full response: `receptionDate`, `message`, `processed` → paste in GitHub release notes for `v0.4.0`
6. Re-add `@Disabled` before committing

**This gate is non-negotiable. Do not tag `v0.4.0` without auditable proof of live dispatch.**

---

## Phase 5 — Production Replacement Milestone 🏁
**Tag:** `v1.0.0`
**Exit condition:** All 5 fleet units receive confirmed pulses on the 15-minute automated cycle in production. Skip conditions fire and log correctly. JavaFX desktop app shut down. fleet-pulse-api is the sole GPS dispatcher.

> Phase 4 delivers the full dispatch engine (adapters, orchestration service, scheduler, force-dispatch endpoint). Phase 5 is the production smoke test and the irreversible handover.

| Status | Task |
|---|---|
| ⬜ | Verify all 5 fleet units are configured in `gps.manual.units.*` with valid real coordinates |
| ⬜ | Deploy Phase 4 build to production environment |
| ⬜ | Use `POST /api/units/{numUnidad}/pulse/force` to confirm each of the 5 units dispatches before trusting the scheduler |
| ⬜ | Monitor first 3 automated scheduler ticks — confirm `PULSE_SENT` logged for all configured units |
| ⬜ | Trigger at least one `SKIPPED_OUT_OF_WINDOW` event — confirm log fires correctly |
| ⬜ | Confirm `Protocolo.isProcessed() == true` for all 5 units in production logs |
| ⬜ | **MILESTONE: Shut down JavaFX desktop app. fleet-pulse-api is the sole dispatcher.** |

---

## Phase 6 — Traccar GPS Integration
**Tag:** `v1.1.0`
**Exit condition:** Traccar OsmAnd push received, stored in cache, dispatched to QSolutions. `SKIPPED_STALE` fires correctly after 300s. Full flow verified with Mockito before real devices.

| Status | Task |
|---|---|
| ⬜ | `GpsPositionCache.java` — `ConcurrentHashMap<String, GpsReading>` |
| ⬜ | `TraccarPositionController.java` — public endpoint, receives OsmAnd HTTP GET |
| ⬜ | `TraccarCoordinateAdapter.java` implements `GpsCoordinateProvider` |
| ⬜ | `GpsProviderConfig.java` extended — add TRACCAR switch via `application.properties` |
| ⬜ | `SKIPPED_STALE` logged when coordinate age > `GPS_MAX_COORDINATE_AGE_SECONDS` (300s default) |
| ⬜ | Full Traccar flow tested with Mockito — no real phones required |
| ⬜ | Operator setup guide — how to configure Traccar Client (Server URL, device ID, interval) |

---

## Phase 7 — REST API + Dry-run
**Tag:** `v1.2.0`
**Exit condition:** All endpoints in authorization matrix respond correctly. Dry-run returns `GpsReading` with zero `PulseSender` invocations verified in tests. API contract stable for React.

| Status | Task |
|---|---|
| ⬜ | `UnitController.java` — CRUD endpoints for units (ADMIN) |
| ⬜ | `UnitManagementService.java` |
| ⬜ | `ProviderTestController.java` — dry-run endpoint |
| ⬜ | `ProviderTestService.java` — calls only `GpsCoordinateProvider`, never `PulseSender` |
| ⬜ | `GET /api/units/{numUnidad}/pulse/test` — returns `GpsReading`, zero SOAP calls |
| ⬜ | `PulseController.java` — expose force dispatch via REST |
| ⬜ | All endpoints validated against authorization matrix |
| ⬜ | API contract documented and stable — React team can begin |

---

## Phase 8 — React Frontend + Pulse Log (Deferred)
**Tag:** `v2.0.0`
**Exit condition:** Frontend deployed on same domain as API. Pulse log visible in dashboard. Auth flow complete.

| Status | Task |
|---|---|
| ⬜ | `V5__pulse_log.sql` Flyway migration |
| ⬜ | Pulse log write on every dispatch result (SENT, SKIPPED, REJECTED, ERROR) |
| ⬜ | React project initialized |
| ⬜ | Authentication flow — login, access token storage, refresh |
| ⬜ | Dashboard — unit status, active window, last pulse timestamp |
| ⬜ | Pulse log table — filterable by unit and status |
| ⬜ | Deployment — backend + frontend on same domain with HTTPS |

---

## Branch Strategy

| Branch | Purpose |
|---|---|
| `main` | Production-ready code only. Tagged on phase completion. |
| `phase/1-domain` | Phase 1 work. Merges to main when exit condition confirmed. |
| `phase/2-persistence` | Phase 2 work. Same pattern. |
| `phase/N-name` | One branch per phase. |

Merge to `main` only when the phase exit condition is fully met.
The merge commit is what gets tagged with the release version.

---

## GitHub Release Notes Template

Use this structure for every release:

```
Phase N — [Name]
Exit condition met: Yes / No
Deviations from README: None / [describe]

What was built
[list]

Exit condition evidence
[paste relevant log output, test results, or server responses]

Next phase
[one sentence preview]
```
