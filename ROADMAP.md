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
| 4 | `v0.4.0` | Pre-release | Dispatch engine + API contract freeze |
| 5 | `v1.0.0` | **Full release** | Production replacement |
| 6 | `v1.1.0` | **Full release** | Real-time GPS (Traccar) + technical debt resolved + pulse log |
| 7 | `v1.2.0` | Full release | Auth hardening — httpOnly refresh cookie, single-parse JWT filter, dependency scanning |
| 8 | `v1.3.0` | Full release | Pre-frontend readiness — API contract freeze, full endpoint audit, end-to-end smoke suite |
| 9 | `v2.0.0` | Full release | React frontend shipped, screen by screen |

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
| 7.1 | AuthServiceTest | `AuthServiceTest.java` | Unit | Mockito — no Spring | Layer 3 — AuthService ✅ | ✅ |
| 7.2 | UserManagementServiceTest | `UserManagementServiceTest.java` | Unit | Mockito — no Spring | Layer 3 — UserManagementService ✅ | ✅ |
| 7.3 | AdminUserInitializerTest | `AdminUserInitializerTest.java` | Unit | Mockito — no Spring | Layer 5.7 — AdminUserInitializer ✅ | ✅ |
| 7.4 | JwtServiceTest (additions) | `JwtServiceTest.java` | Unit | JUnit 5, JJWT — no Spring | Layer 4.3 — JwtService ✅ | ✅ |
| 7.5 | RefreshTokenJpaAdapterTest | `RefreshTokenJpaAdapterTest.java` | JPA Integration | `@DataJpaTest` + H2 | Layer 4.1 — RefreshTokenJpaAdapter ✅ | ✅ |
| 7.6 | JwtAuthenticationFilterTest | `JwtAuthenticationFilterTest.java` | Security Integration | `@SpringBootTest` + MockMvc + `@MockBean` | Layer 4.5 — JwtAuthenticationFilter ✅ | ✅ |
| 7.7 | AuthControllerTest | `AuthControllerTest.java` | Controller Integration | `@SpringBootTest` + MockMvc + `@MockBean` | Layer 5 — AuthController ✅, GlobalExceptionHandler ✅ | ✅ |
| 7.8 | ArchUnit (expansion) | `HexagonalArchitectureTest.java` | Architecture | ArchUnit — no Spring | All layers ✅ | ✅ |

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

## Phase 4 — Dispatch Engine + API Contract Freeze ✅ COMPLETE
**Tag:** `v0.4.0` — READY TO TAG
**Exit condition:** (1) SOAP confirmed live: force-dispatch triggers `Protocolo.isProcessed() == true` against live QSolutions endpoint. (2) Global scheduler disabled by default (`scheduler.pulse.global-enabled=false`); per-unit round scheduling starts, ticks, and stops via API — double-start 409, stop-when-not-active 409, `horarioFijo == false` with hours persists to DB before map insert. (3) Full Unit CRUD + schedule management API operational — all 6 `UnitController` endpoints return correct status codes with valid JWT. (4) React contract frozen: `UnitResponse` shape (including `roundActive` boolean) documented and stable. All tests green (ArchUnit passes, no `@Component` violations). Auditable `Protocolo` response pasted in GitHub release notes before tagging.

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

Registered via `@EnableConfigurationProperties(ManualCoordinateProperties.class)` in `GpsProviderConfig` (Layer 3).

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
| ✅ | Add 4 property blocks to `application.properties` (SOAP timeouts, manual GPS map, provider selector, scheduler interval) |
| ✅ | Add same env vars to `.env.example` and `.env` |
| ✅ | Create `ManualCoordinateProperties.java` |
| ✅ | Add `receiveGpsInfoSoap()` @Bean to `ApplicationConfig` — classpath WSDL + endpoint override + timeouts |
| ✅ | Add `findAllActiveNumUnidades()` to `UnitRepository` port + `UnitJpaAdapter` + `UnitJpaRepository` |
| ✅ | `mvn test` — ArchUnit passes, no `@Value` binding errors |

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
| ✅ | Create `QSolutionsSoapAdapter.java` implementing `PulseSender` |
| ✅ | `send(ScheduledPulse)` maps all 8 required `GPSInfo` fields |
| ✅ | `toXmlCalendar(ZonedDateTime)` uses `FLEET_TIMEZONE` with static `DatatypeFactory` |
| ✅ | `Processed=false` → `PulseSendException`; `WebServiceException` → `GpsProviderUnavailableException` |
| ✅ | `password` field never appears in any log output |
| ✅ | Declare `QSolutionsSoapAdapter` as `@Bean` in `ApplicationConfig` — inject `ReceiveGPSInfoSoap` port + `@Value` credentials |
| ✅ | `mvn test` — ArchUnit passes (no `@Component`, no Spring imports in adapter) |

Exit condition: Adapter compiles. ArchUnit passes. Credentials never in source code. `PulseSendException` and `GpsProviderUnavailableException` thrown in correct cases (verified in 8.1 unit tests).

---

### Layer 3 — ManualCoordinateAdapter + GpsProviderConfig

> ⛔ Cannot begin until Layer 1 complete — depends on `ManualCoordinateProperties`.
>
> **Scope note:** `ManualCoordinateAdapter` serves two callers only — the global `PulseSchedulerService`
> (via `PulseOrchestrationService.sendPulse()`) and `ProviderTestService` (dry-run). It is NOT
> called by force-dispatch (`PulseController`) or round-tick (`processTick()`): those receive
> a `GpsReading` pre-assembled by the caller and call `SendPulseUseCase.dispatch()` directly.

File: `infrastructure/adapter/out/gps/ManualCoordinateAdapter.java`

Implements: `GpsCoordinateProvider` (`application/port/out/GpsCoordinateProvider.java`)

Declared as `@Bean` in `GpsProviderConfig` (this layer). No `@Component`.

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

#### `GpsProviderConfig.java` — `infrastructure/config/GpsProviderConfig.java`

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

`matchIfMissing = true` — `MANUAL` is the default when `gps.provider` is absent. Setting `GPS_PROVIDER=traccar` in Phase 6 swaps the implementation without any application code changes.

| Status | Task |
|---|---|
| ✅ | Create `ManualCoordinateAdapter.java` implementing `GpsCoordinateProvider` |
| ✅ | Constructor builds `Collections.unmodifiableMap` from `ManualCoordinateProperties` |
| ✅ | `isAvailable` — O(1) `containsKey`, zero I/O |
| ✅ | `getCoordinates` — returns fresh `GpsReading` with `ZonedDateTime.now(FLEET_TIMEZONE)` and `ProviderType.MANUAL` |
| ✅ | Create `GpsProviderConfig.java` with `@ConditionalOnProperty(matchIfMissing = true)` + `@EnableConfigurationProperties(ManualCoordinateProperties.class)` |
| ✅ | Verify exactly one `GpsCoordinateProvider` bean resolves in `@SpringBootTest` context |
| ✅ | `mvn test` — ArchUnit passes |

Exit condition: Adapter compiles. `isAvailable` returns correct boolean for configured and unconfigured units without any external calls. Exactly one `GpsCoordinateProvider` bean — `ManualCoordinateAdapter`. ArchUnit passes.

---

### Layer 4 — PulseOrchestrationService

> ⛔ Cannot begin until Layer 3 complete — depends on `GpsCoordinateProvider` and `PulseSender` beans.
>
> **This layer also:** adds `dispatch(String numUnidad, GpsReading gpsReading)` to
> `SendPulseUseCase` port; removes stray Spanish comments from the existing file; and adds
> injectable `Clock` to resolve `FIXME-CLOCK`.

File: `application/service/PulseOrchestrationService.java`

Implements: `SendPulseUseCase` (`application/port/in/SendPulseUseCase.java`)

Declared as `@Bean` in `ApplicationConfig`. No `@Service`. No `@Transactional` — read-then-delegate path with no DB mutations.

#### `SendPulseUseCase` port — add `dispatch()` method

**`application/port/in/SendPulseUseCase.java`** — add alongside existing `sendPulse()`:

```java
/**
 * Dispatches a pre-assembled GPS reading to QSolutions.
 * No window check. No provider lookup. Caller supplies the GpsReading.
 * Used by: force dispatch (PulseController), round tick (processTick()).
 */
void dispatch(String numUnidad, GpsReading gpsReading);
```

`sendPulse(String numUnidad)` — unchanged. Used by global `PulseSchedulerService` only.

#### Dispatch algorithms

```
sendPulse(String numUnidad):
  — global scheduler path: window check + provider lookup + send

  1. unit = unitRepository.findByNumUnidad(numUnidad)
             .orElseThrow(() -> new UnitNotFoundException("Unit not found: " + numUnidad))

  2. if (!unit.isActive()):
         log.info("SKIPPED_UNIT_INACTIVE", kv("numUnidad", numUnidad))
         return

  3. if (!unit.isWithinActiveWindow(LocalTime.now(clock))):
         log.info("SKIPPED_OUT_OF_WINDOW", kv("numUnidad", numUnidad),
                  kv("window", unit.getHoraInicio() + "-" + unit.getHoraFin()))
         return

  4. if (!gpsProvider.isAvailable(numUnidad)):
         log.info("SKIPPED_NO_COORDINATES", kv("numUnidad", numUnidad))
         return

  5. GpsReading reading           = gpsProvider.getCoordinates(numUnidad)
  6. ZonedDateTime fechaRecepcion = ZonedDateTime.now(clock.getZone() != null
                                        ? clock : Clock.system(FLEET_TIMEZONE))
  7. dispatch(numUnidad, reading)     // delegates to shared dispatch path


dispatch(String numUnidad, GpsReading gpsReading):
  — force dispatch + round tick path: no window check, no provider lookup

  1. unit = unitRepository.findByNumUnidad(numUnidad)
             .orElseThrow(() -> new UnitNotFoundException("Unit not found: " + numUnidad))

  2. if (!unit.isActive()):
         throw UnitNotActiveException(numUnidad)     // callers handle: 409 (controller) or auto-remove (tick)

  3. String effectiveTracking = (unit.getTrackingNumber() != null)
                                    ? unit.getTrackingNumber()
                                    : defaultTrackingNumber

  4. ZonedDateTime fechaRecepcion = ZonedDateTime.now(clock)

  5. pulseSender.send(new ScheduledPulse(unit, gpsReading, fechaRecepcion, effectiveTracking))
     — PulseSendException propagates unchanged
     — GpsProviderUnavailableException propagates unchanged
```

#### Constructor

```java
public PulseOrchestrationService(
        UnitRepository unitRepository,
        GpsCoordinateProvider gpsProvider,
        PulseSender pulseSender,
        Clock clock,
        String defaultTrackingNumber) {
    this.unitRepository        = Objects.requireNonNull(unitRepository);
    this.gpsProvider           = Objects.requireNonNull(gpsProvider);
    this.pulseSender           = Objects.requireNonNull(pulseSender);
    this.clock                 = Objects.requireNonNull(clock);
    this.defaultTrackingNumber = Objects.requireNonNull(defaultTrackingNumber);
}
```

Replace `private static final ZoneId FLEET_TIMEZONE = ZoneId.of(...)` with `private final Clock clock`. Remove stray Spanish comments (lines 18-26 of existing file).

Declare in `ApplicationConfig`:

```java
@Bean
public Clock clock() {
    return Clock.system(FleetConstants.FLEET_TIMEZONE);
}

@Bean
public PulseOrchestrationService pulseOrchestrationService(
        UnitRepository unitRepository,
        GpsCoordinateProvider gpsProvider,
        PulseSender pulseSender,
        Clock clock,
        @Value("${qsolutions.tracking-number}") String defaultTrackingNumber) {
    return new PulseOrchestrationService(unitRepository, gpsProvider, pulseSender, clock, defaultTrackingNumber);
}
```

The `Clock` `@Bean` declared here resolves `FIXME-CLOCK` from known debt (CLAUDE.md Section 7).

#### Skip condition log keys — stable, searchable in production

| Condition | Log key | Level |
|---|---|---|
| Unit `active = false` in DB | `SKIPPED_UNIT_INACTIVE` | INFO |
| Outside `horaInicio–horaFin` | `SKIPPED_OUT_OF_WINDOW` | INFO |
| Unit absent from `gps.manual.units.*` | `SKIPPED_NO_COORDINATES` | INFO |

| Status | Task |
|---|---|
| ✅ | Add `dispatch(String numUnidad, GpsReading gpsReading)` to `SendPulseUseCase` port |
| ✅ | Update `PulseOrchestrationService.java` — inject `Clock`, remove stray Spanish comments, add `dispatch()` implementation |
| ✅ | `sendPulse()` uses `LocalTime.now(clock)` (not `LocalTime.now(FLEET_TIMEZONE)`) |
| ✅ | `dispatch()` throws `UnitNotActiveException` (not silent skip) |
| ✅ | `effectiveTrackingNumber` resolves: `unit.getTrackingNumber() != null` → unit value, else `defaultTrackingNumber` |
| ✅ | Add `Clock @Bean` + update `pulseOrchestrationService()` @Bean in `ApplicationConfig` to pass `Clock` |
| ✅ | `mvn test` — ArchUnit passes (no `@Service`, no `@Transactional`, no Spring imports in `application/`) |

Exit condition: Service compiles. `FIXME-CLOCK` resolved. Both `sendPulse()` and `dispatch()` paths covered by 9.3 unit tests with `Clock.fixed`. ArchUnit passes.

---

### Layer 5 — PulseSchedulerService + SchedulerConfig

> ⛔ Cannot begin until Layer 4 complete — consumes `SendPulseUseCase.sendPulse()` port method.
>
> **Disabled by default:** `scheduler.pulse.global-enabled=false` in `application.properties`.
> `PulseSchedulerService` uses `@ConditionalOnProperty(name = "scheduler.pulse.global-enabled", havingValue = "true")`
> so the global scheduler bean is not created unless explicitly enabled — no accidental live dispatches
> during development. `ManualCoordinateAdapter` is the GPS source for this scheduler's path
> (via `PulseOrchestrationService.sendPulse()`).

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
| ✅ | Create `SchedulerConfig.java` with `@EnableScheduling` and `@ConditionalOnProperty` `pulseSchedulerService()` @Bean |
| ✅ | Add `scheduler.pulse.global-enabled=false` to `application.properties` |
| ✅ | Create `PulseSchedulerService.java` with `@Scheduled(fixedRateString)` dispatch method |
| ✅ | Per-unit exception catch — failure of one unit never aborts the cycle |
| ✅ | `SCHEDULER_TICK_START` and `SCHEDULER_TICK_END` logged with unit count |
| ✅ | No `@Component` on `PulseSchedulerService` — `mvn test` ArchUnit passes |

Exit condition: Context starts with `scheduler.pulse.global-enabled=false` — `PulseSchedulerService` bean absent. Scheduler fires on configured interval when enabled. Per-unit failure does not abort the cycle. `SCHEDULER_TICK_END` logged after every tick.

---

### Layer 6 — Unit CRUD + Schedule Management

> ⛔ Cannot begin until Layer 4 complete — `UnitNotActiveException` and `UnitRepository` required.
> May be implemented in parallel with Layer 5. Pulled forward from old Phase 7 to freeze the
> React API contract in Phase 4.

This layer delivers the full `UnitController` (5 endpoints) and `ConfigureScheduleUseCase`. The
`roundActive` and `currentCoordinateMode` fields in `UnitResponse` require `ManageRoundUseCase`
(Layer 7) — the controller is wired to accept it via constructor injection. Because Layer 6
precedes Layer 7 in build order, `ManageRoundUseCase` port exists at this point (it is a Java
interface in `application/port/in/`) — only the implementation (`RoundManagementService`) comes
in Layer 7.

#### New domain exceptions (all 409)

| File | HTTP | Type URI |
|---|---|---|
| `domain/exception/UnitNotActiveException.java` | 409 | `/errors/unit-not-active` |
| `domain/exception/ScheduleConflictException.java` | 409 | `/errors/schedule-conflict` |

Add `@ExceptionHandler` entries to `GlobalExceptionHandler` for each.

> Note: `UnitNotActiveException` is shared with Layer 7 (round scheduling) and Layer 4
> (`dispatch()` method). Define it in this layer; later layers reference it.

#### New driving port — `ManageUnitUseCase`

`application/port/in/ManageUnitUseCase.java`:

```java
List<Unit> listAllUnits();
Optional<Unit> findByNumUnidad(String numUnidad);
Unit activateUnit(String numUnidad);
Unit deactivateUnit(String numUnidad);
```

#### `ConfigureScheduleUseCase` — already declared in Phase 1

Verify it exists. If not, create `application/port/in/ConfigureScheduleUseCase.java`:

```java
Unit updateSchedule(String numUnidad, boolean horarioFijo,
                    @Nullable LocalTime horaInicio, @Nullable LocalTime horaFin);
```

`horarioFijo == false` + `horaInicio`/`horaFin` null: reset to `(LocalTime.MIN, LocalTime.MAX)` sentinel (ADR-013).
`horarioFijo == false` + hours supplied: validate `horaInicio < horaFin`, throw `ScheduleConflictException` if not.

#### New application service — `UnitManagementService`

`application/service/UnitManagementService.java`

Implements `ManageUnitUseCase` and `ConfigureScheduleUseCase`. Declared as `@Bean` in `ApplicationConfig`. No `@Service`. No `@Transactional` (unit activate/deactivate: single DB write, no two-phase commit needed).

```
activateUnit(numUnidad):
  1. unitRepository.findByNumUnidad(numUnidad).orElseThrow(UnitNotFoundException)
  2. unitRepository.setActive(numUnidad, true)  // @Modifying @Query — single UPDATE
  3. return re-fetched unit

deactivateUnit(numUnidad):
  1. unitRepository.findByNumUnidad(numUnidad).orElseThrow(UnitNotFoundException)
  2. unitRepository.setActive(numUnidad, false)
  3. return re-fetched unit

updateSchedule(numUnidad, horarioFijo, horaInicio, horaFin):
  1. unit = unitRepository.findByNumUnidad(numUnidad).orElseThrow(UnitNotFoundException)
  2. if horarioFijo == false AND horaInicio != null AND horaFin != null:
       if !horaInicio.isBefore(horaFin): throw ScheduleConflictException
  3. LocalTime resolvedInicio = (horaInicio != null) ? horaInicio : LocalTime.MIN   // ADR-013 sentinel
     LocalTime resolvedFin    = (horaFin != null) ? horaFin : LocalTime.MAX
  4. unitRepository.updateSchedule(numUnidad, resolvedInicio, resolvedFin)
     unitRepository.setHorarioFijo(numUnidad, horarioFijo)                          // @Modifying @Query
  5. return re-fetched unit
```

#### `UnitRepository` port additions

Add to `application/port/out/UnitRepository.java`:

```java
void setActive(String numUnidad, boolean active);
void setHorarioFijo(String numUnidad, boolean horarioFijo);
// updateSchedule already declared in Layer 7 — ensure it exists here
void updateSchedule(String numUnidad, LocalTime horaInicio, LocalTime horaFin);
```

Add corresponding `@Modifying @Query` methods to `UnitJpaRepository` and delegation in `UnitJpaAdapter`.

#### New DTO — `UnitResponse`

`infrastructure/adapter/in/web/dto/UnitResponse.java`:

```java
public record UnitResponse(
    String numUnidad,
    boolean horarioFijo,
    LocalTime horaInicio,
    LocalTime horaFin,
    boolean active,
    boolean roundActive,
    CoordinateMode currentCoordinateMode  // null when roundActive == false
) {
    public static UnitResponse from(Unit unit, boolean roundActive, CoordinateMode currentMode) {
        return new UnitResponse(
            unit.getNumUnidad(), unit.isHorarioFijo(),
            unit.getHoraInicio(), unit.getHoraFin(),
            unit.isActive(), roundActive,
            currentMode  // null when no active round
        );
    }
}
```

`roundActive` is O(1) from `ConcurrentHashMap.containsKey()` in `ManageRoundUseCase.isRoundActive()`.
`currentCoordinateMode` is O(1) from `ManageRoundUseCase.getRoundCoordinateMode()` — `null` when
no active round. `CoordinateMode` is a domain enum in `domain/model/` — valid import from
`infrastructure/adapter/in/web/dto/`.

#### New DTO — `ScheduleUpdateRequest`

```java
public record ScheduleUpdateRequest(
    @NotNull Boolean horarioFijo,
    @Nullable @JsonFormat(pattern = "HH:mm") LocalTime horaInicio,
    @Nullable @JsonFormat(pattern = "HH:mm") LocalTime horaFin
) {}
```

#### `UnitController` — 5 endpoints

`infrastructure/adapter/in/web/UnitController.java`:

```
GET  /api/units                        → 200 List<UnitResponse>  ADMIN, USER
GET  /api/units/{numUnidad}            → 200 UnitResponse        ADMIN, USER
PUT  /api/units/{numUnidad}/activate   → 200 UnitResponse        ADMIN
PUT  /api/units/{numUnidad}/deactivate → 200 UnitResponse        ADMIN
PUT  /api/units/{numUnidad}/schedule   → 200 UnitResponse        ADMIN, USER (conditional — see note)
```

> POST/DELETE units not included — fleet is a fixed 5-unit set. Unit provisioning is via
> Flyway seed migration only. No API for adding or removing units in Phase 4.

Constructor injects `ManageUnitUseCase`, `ConfigureScheduleUseCase`, and `ManageRoundUseCase`.
`ManageRoundUseCase.isRoundActive(numUnidad)` and `ManageRoundUseCase.getRoundCoordinateMode(numUnidad)`
called at response assembly time to populate `roundActive` and `currentCoordinateMode`.

**Authorization note for `PUT /schedule`:** The SecurityConfig matrix opens this endpoint to
both `ADMIN` and `USER`. Fine-grained authorization based on `unit.horarioFijo` is enforced in
the controller (infrastructure layer) before calling the use case — not in the use case itself.
This keeps `ConfigureScheduleUseCase` free of security concerns (clean hexagonal boundary). The
controller checks: if the caller has authority `"USER"` and `unit.isHorarioFijo() == true`,
throws `AccessDeniedException` → 403. ADMIN always proceeds. `AccessDeniedException` is a
Spring Security type and is valid in the controller (infrastructure); it must never appear in
`application/service/`.

**`PUT /schedule` controller snippet:**

```java
@PutMapping("/{numUnidad}/schedule")
public ResponseEntity<UnitResponse> updateSchedule(
        @PathVariable String numUnidad,
        @Valid @RequestBody ScheduleUpdateRequest request,
        Authentication authentication) {

    Unit unit = manageUnitUseCase.findByNumUnidad(numUnidad)
        .orElseThrow(() -> new UnitNotFoundException(numUnidad));

    boolean isUser = authentication.getAuthorities().stream()
        .anyMatch(a -> a.getAuthority().equals("USER"));

    if (isUser && unit.isHorarioFijo()) {
        throw new AccessDeniedException("USER role cannot modify a fixed schedule");
    }

    Unit updated = configureScheduleUseCase.updateSchedule(
        numUnidad, request.horarioFijo(), request.horaInicio(), request.horaFin());

    boolean roundActive = manageRoundUseCase.isRoundActive(numUnidad);
    CoordinateMode currentMode = manageRoundUseCase.getRoundCoordinateMode(numUnidad);
    return ResponseEntity.ok(UnitResponse.from(updated, roundActive, currentMode));
}
```

**Add to `SecurityConfig` authorization matrix:**

```java
.requestMatchers(HttpMethod.GET, "/api/units").hasAnyAuthority("ADMIN", "USER")
.requestMatchers(HttpMethod.GET, "/api/units/*").hasAnyAuthority("ADMIN", "USER")
.requestMatchers(HttpMethod.PUT, "/api/units/*/activate").hasAuthority("ADMIN")
.requestMatchers(HttpMethod.PUT, "/api/units/*/deactivate").hasAuthority("ADMIN")
.requestMatchers(HttpMethod.PUT, "/api/units/*/schedule").hasAnyAuthority("ADMIN", "USER")
```

**`ApplicationConfig` additions:**

```java
@Bean
public UnitManagementService unitManagementService(UnitRepository unitRepository) {
    return new UnitManagementService(unitRepository);
}
```

**GlobalExceptionHandler additions:**

```java
@ExceptionHandler(UnitNotActiveException.class)
public ResponseEntity<ProblemDetail> handleUnitNotActive(UnitNotActiveException ex) { ... }  // 409

@ExceptionHandler(ScheduleConflictException.class)
public ResponseEntity<ProblemDetail> handleScheduleConflict(ScheduleConflictException ex) { ... }  // 409
```

| Status | Task |
|---|---|
| ✅ | Create `UnitNotActiveException` and `ScheduleConflictException` domain exceptions |
| ✅ | Add 2 `@ExceptionHandler` entries to `GlobalExceptionHandler` |
| ✅ | Create `ManageUnitUseCase` port (`application/port/in/`) |
| ✅ | Verify or create `ConfigureScheduleUseCase` port with `updateSchedule()` signature |
| ✅ | Add `setActive()`, `setHorarioFijo()`, `updateSchedule()` to `UnitRepository` port + `@Modifying @Query` in `UnitJpaRepository` + delegation in `UnitJpaAdapter` |
| ✅ | Create `UnitManagementService` implementing both use cases — ADR-013 sentinel for null hours |
| ✅ | Add `unitManagementService()` @Bean to `ApplicationConfig` |
| ✅ | Create `UnitResponse` DTO record with `roundActive` and `currentCoordinateMode` fields and `from(Unit, boolean, CoordinateMode)` factory |
| ✅ | Create `ScheduleUpdateRequest` DTO record |
| ✅ | Create `UnitController` with 5 endpoints (GET list, GET single, PUT activate, PUT deactivate, PUT schedule) |
| ✅ | Add 5 `requestMatchers` entries to `SecurityConfig` matrix |
| ✅ | `@Tag`, `@Operation`, `@ApiResponse` on all `UnitController` methods |
| ✅ | `mvn test` — ArchUnit passes, `UnitResponse` is a record |

Exit condition: All 5 `UnitController` endpoints return correct HTTP status codes with valid JWT. `roundActive` field in `UnitResponse` reflects current in-memory round state. `PUT /schedule` with `horaInicio >= horaFin` returns 409. ADR-013 sentinel verified: `horarioFijo=false` with null hours writes `(LocalTime.MIN, LocalTime.MAX)` to DB. ArchUnit passes. `mvn test` green.

---

### Layer 7 — Per-Unit Dynamic Round Scheduling

> ⛔ Cannot begin until Layer 6 complete — depends on `SendPulseUseCase` (Layer 4),
> `UnitRepository` (Layer 1), `ManageUnitUseCase` / `UnitNotActiveException` (Layer 6),
> and `SchedulerConfig` (Layer 5).

**Design — single-tick pattern:**
A `ConcurrentHashMap<String, RoundState>` in `RoundManagementService` tracks active rounds
in memory. `RoundTickService` fires a `@Scheduled` tick at `scheduler.round.tick-ms` intervals;
the tick iterates the map and dispatches any unit whose time window is open and whose cooldown
(`scheduler.round.interval-ms`) has elapsed since `ultimoEnvio`. No per-unit `ScheduledFuture`.
No separate thread pool.

**Confirmed decisions:**
- Schedule is always read from DB at `startRound()` time — no schedule write occurs in this path. Use `PUT /api/units/{numUnidad}/schedule` to configure the dispatch window **before** calling `startRound()`.
- If the DB stores sentinel values `(LocalTime.MIN, LocalTime.MAX)` for a unit with `horarioFijo=false`, `startRound()` throws `ScheduleNotConfiguredException`. This is a defensive guard — sentinel is only present when no explicit window has been set via PUT /schedule.
- `horarioFijo` is never modified by `startRound()` or `stopRound()`.
- `RoundState` captures `horaInicio`/`horaFin` at `startRound()` time. DB schedule changes made after round start are NOT reflected until the round is stopped and restarted.
- Active-round state is in-memory only — not persisted across restarts.
- Global `PulseSchedulerService` is independent: disabled by default (`scheduler.pulse.global-enabled=false`). `RoundTickService` has its own `@Scheduled` and is unaffected by the global scheduler being on or off.

#### `RoundState` — `application/service/RoundState.java`

```java
public record RoundState(
    LocalTime horaInicio,
    LocalTime horaFin,
    CoordinateMode coordinateMode,
    BigDecimal manualLat,   // null when coordinateMode == AUTOMATIC
    BigDecimal manualLon,   // null when coordinateMode == AUTOMATIC
    Instant ultimoEnvio     // null if unit has not yet dispatched in this round
) {}
```

Co-located with `RoundManagementService` in `application/service/`. Pure Java record — `java.time.*`, `java.math.BigDecimal`, and domain enum `CoordinateMode`. Stays in `application/service/` because infrastructure importing application is valid (infra → app), but `application/` importing from `infrastructure/` is a violation. `CoordinateMode` is a domain enum in `domain/model/` — valid import from application layer.

#### Domain additions — 4 new exceptions, all 409

| File | HTTP | Type URI |
|---|---|---|
| `domain/exception/RoundAlreadyActiveException.java` | 409 | `/errors/round-already-active` |
| `domain/exception/RoundAlreadyActiveException.java` | 409 | `/errors/round-already-active` |
| `domain/exception/RoundNotActiveException.java` | 409 | `/errors/round-not-active` |
| `domain/exception/ScheduleNotConfiguredException.java` | 409 | `/errors/schedule-not-configured` |
| `domain/exception/UnitNotActiveException.java` | 409 | `/errors/unit-not-active` | ← declared in Layer 6, referenced here |

Add 3 new `@ExceptionHandler` entries to `GlobalExceptionHandler` (for the 3 new exceptions). `UnitNotActiveException` handler already exists from Layer 6 — verify it is present. Each returns `HttpStatus.CONFLICT` with `application/problem+json` and the type URI above.

#### Port additions

**`UnitRepository.updateSchedule()` — verify from Layer 6:**

`updateSchedule(String numUnidad, LocalTime horaInicio, LocalTime horaFin)` is declared in Layer 6 for `ConfigureScheduleUseCase`. Layer 7's `startRound()` reads the DB but **never writes the schedule**. Verify the method and its `@Modifying @Query` in `UnitJpaRepository` exist before implementing `RoundManagementService`. No new declaration needed here.

**`getRoundCoordinateMode()` implementation in `RoundManagementService`:**

```java
@Override
public CoordinateMode getRoundCoordinateMode(String numUnidad) {
    RoundState state = activeRounds.get(numUnidad);
    return state != null ? state.coordinateMode() : null;
}
```

O(1) `ConcurrentHashMap.get()`. Returns `null` when no active round. Called by `UnitController` at response assembly time to populate `currentCoordinateMode` in `UnitResponse`.

**`ManageRoundUseCase`** — `application/port/in/ManageRoundUseCase.java`:

```java
void startRound(String numUnidad, CoordinateMode coordinateMode,
                @Nullable BigDecimal manualLat, @Nullable BigDecimal manualLon);
void stopRound(String numUnidad);
boolean isRoundActive(String numUnidad);
@Nullable CoordinateMode getRoundCoordinateMode(String numUnidad);
```

`coordinateMode` is mandatory. `manualLat`/`manualLon` required when `coordinateMode == MANUAL`; `null` is valid when `coordinateMode == AUTOMATIC`. Schedule is always read from the DB at round start — hours are NOT accepted in this method; configure the window via `PUT /api/units/{numUnidad}/schedule` before calling `startRound()`. `getRoundCoordinateMode()` returns `null` when no round is active for the given unit.

#### Application service — `RoundManagementService`

File: `application/service/RoundManagementService.java`

Implements `ManageRoundUseCase`. Declared as `@Bean` in `ApplicationConfig` with **return type `RoundManagementService`** (not `ManageRoundUseCase`). Spring satisfies `ManageRoundUseCase` injection points in the controller automatically via interface matching. `SchedulerConfig` injects it by class for the tick service. No `@Service`. No `@Transactional`. Constructor injects `GpsCoordinateProvider gpsProvider` alongside `unitRepository`, `sendPulseUseCase`, `clock`, and `roundIntervalMs`.

**`startRound()` validation order:**

```
1. unit ← unitRepository.findByNumUnidad(numUnidad)
             .orElseThrow(UnitNotFoundException)

2. if !unit.isActive()
     → throw UnitNotActiveException                     // precondition before DB or map work

3. if activeRounds.containsKey(numUnidad)
     → throw RoundAlreadyActiveException                // fast reject — no DB side effects

4. Validate coordinate mode:
     a. coordinateMode == AUTOMATIC
          → throw InvalidCoordinateException("AUTOMATIC mode not supported until Phase 6")
            // FIXME-PHASE6: remove this guard when TraccarCoordinateAdapter is wired
     b. coordinateMode == MANUAL AND (manualLat == null OR manualLon == null)
          → throw InvalidCoordinateException("MANUAL mode requires lat and lon")
     c. coordinateMode == MANUAL AND coords supplied
          → GpsReading constructor validates range — throws InvalidCoordinateException if OOB

5. Resolve schedule (always reads from DB — no DB write in this path):
     a. horaInicio = unit.getHoraInicio()
        horaFin    = unit.getHoraFin()
     b. if horaInicio == LocalTime.MIN AND horaFin == LocalTime.MAX:
          → throw ScheduleNotConfiguredException        // sentinel guard — configure via PUT /schedule first

6. RoundState existing = activeRounds.putIfAbsent(numUnidad,
                             new RoundState(horaInicio, horaFin,
                                 coordinateMode, manualLat, manualLon, null))
   if existing != null
     → throw RoundAlreadyActiveException                // concurrent race guard

7. log.info("ROUND_STARTED", ...)
```

**`stopRound()` validation order:**

```
1. RoundState removed = activeRounds.remove(numUnidad)
   if removed == null
     → throw RoundNotActiveException

2. log.info("ROUND_STOPPED", ...)
```

**`processTick()` — internal method, called by `RoundTickService`:**

```
LocalTime now    = LocalTime.now(clock)
Instant  instant = Instant.now(clock)

for each (numUnidad, state) in activeRounds.entrySet():     // weakly consistent — safe
  if now >= state.horaInicio() AND now <= state.horaFin():
    if state.ultimoEnvio() == null
       OR Duration.between(state.ultimoEnvio(), instant).toMillis() >= roundIntervalMs:
      try:
        GpsReading reading
        if state.coordinateMode() == MANUAL:
          reading = new GpsReading(numUnidad, state.manualLat(), state.manualLon(),
                                   ZonedDateTime.now(clock), ProviderType.MANUAL)
        else: // AUTOMATIC — Phase 6 path
          reading = gpsProvider.getCoordinates(numUnidad)  // may throw GpsProviderUnavailableException

        sendPulseUseCase.dispatch(numUnidad, reading)

        activeRounds.computeIfPresent(numUnidad,            // atomic — no-op if round stopped
            (k, v) -> new RoundState(v.horaInicio(), v.horaFin(),
                                     v.coordinateMode(), v.manualLat(), v.manualLon(), instant))
        log.info("ROUND_PULSE_SENT", kv("numUnidad", numUnidad))

      catch GpsProviderUnavailableException e:
        activeRounds.remove(numUnidad)                      // auto-remove: provider lost
        log.warn("ROUND_REMOVED_GPS_UNAVAILABLE", kv("numUnidad", numUnidad))

      catch UnitNotActiveException e:
        activeRounds.remove(numUnidad)                      // auto-remove: unit deactivated mid-round
        log.warn("ROUND_REMOVED_UNIT_INACTIVE", kv("numUnidad", numUnidad))

      catch Exception e:
        log.error("ROUND_TICK_UNIT_FAILED",
            kv("numUnidad", numUnidad), kv("reason", e.getMessage()))
        // Failure of one unit MUST NOT abort the tick for remaining units
```

Thread-safety: `computeIfPresent` for `ultimoEnvio` update is the critical guard — if an HTTP thread calls `remove()` between `dispatch()` and the update, `computeIfPresent` is a no-op, preventing a stopped round from being silently re-inserted. `putIfAbsent` in step 6 of `startRound()` is the concurrent race guard.

#### Infrastructure — `RoundTickService`

File: `infrastructure/scheduler/RoundTickService.java`

```java
public class RoundTickService {

    private static final Logger log = LoggerFactory.getLogger(RoundTickService.class);
    private final RoundManagementService roundManagementService;

    public RoundTickService(RoundManagementService roundManagementService) {
        this.roundManagementService = Objects.requireNonNull(roundManagementService);
    }

    @Scheduled(fixedRateString = "${scheduler.round.tick-ms:30000}")
    public void tick() {
        log.debug("ROUND_TICK_START");
        roundManagementService.processTick();
    }
}
```

Injects `RoundManagementService` by class — `processTick()` is not on the `ManageRoundUseCase` port (that port is user-facing; the tick is an infrastructure scheduling concern). Infrastructure importing an application service class is valid: `infrastructure/* → application.*` is permitted. Declared as `@Bean` in `SchedulerConfig`. No `@Component`.

#### Config additions

**New properties — add to `application.properties` and `.env.example`:**

```properties
# Round dispatch — how often conditions are evaluated (tick interval)
scheduler.round.tick-ms=${SCHEDULER_ROUND_TICK_MS:30000}
# Round dispatch — minimum time between dispatches per unit in an active round
scheduler.round.interval-ms=${SCHEDULER_ROUND_INTERVAL_MS:900000}
```

**`SchedulerConfig` addition:**

```java
@Bean
public RoundTickService roundTickService(RoundManagementService roundManagementService) {
    return new RoundTickService(roundManagementService);
}
```

No new `ThreadPoolTaskScheduler`. `@EnableScheduling` default scheduler is sufficient for ≤ 5 units at 30-second ticks.

**`ApplicationConfig` additions:**

```java
@Bean
public Clock clock() {
    return Clock.system(FleetConstants.FLEET_TIMEZONE);
}

@Bean
public RoundManagementService roundManagementService(
        UnitRepository unitRepository,
        SendPulseUseCase sendPulseUseCase,
        GpsCoordinateProvider gpsProvider,
        Clock clock,
        @Value("${scheduler.round.interval-ms}") long roundIntervalMs) {
    return new RoundManagementService(unitRepository, sendPulseUseCase, gpsProvider, clock, roundIntervalMs);
}
```

Return type of `roundManagementService()` is `RoundManagementService` — not `ManageRoundUseCase`. The `Clock` `@Bean` resolves `FIXME-CLOCK` from known debt (CLAUDE.md).

#### Controller

File: `infrastructure/adapter/in/web/RoundController.java`

Separate from `PulseController` — lifecycle management (start/stop) is semantically distinct from force dispatch.

```
POST /api/units/{numUnidad}/round/start
  Authority:  ADMIN or USER
  Body:       RoundStartRequest (required)
              { "coordinateMode": "MANUAL", "lat": 19.4326, "lon": -99.1332 }
              coordinateMode required. lat/lon required for MANUAL; omit for AUTOMATIC.
              Schedule window is read from DB — configure via PUT /schedule first.
  204 No Content  — round active, tick will dispatch on next qualifying interval
  400             — coordinateMode AUTOMATIC (FIXME-PHASE6 guard) or missing lat/lon for MANUAL
  404             — unit not found
  409             — round already active / unit inactive / schedule not configured (sentinel)

POST /api/units/{numUnidad}/round/stop
  Authority:  ADMIN or USER
  Body:       none
  204 No Content  — unit removed from activeRounds, no further dispatches
  409             — round not active
```

**New DTO:** `RoundStartRequest` in `infrastructure/adapter/in/web/dto/`:

```java
public record RoundStartRequest(
    @NotNull CoordinateMode coordinateMode,
    @Nullable BigDecimal lat,    // required when coordinateMode == MANUAL
    @Nullable BigDecimal lon     // required when coordinateMode == MANUAL
) {}
```

> Schedule window is NOT part of this request — configure it first via `PUT /api/units/{numUnidad}/schedule`.

**Add to `SecurityConfig` authorization matrix** (before `anyRequest().denyAll()`):

```java
.requestMatchers(HttpMethod.POST, "/api/units/*/round/start").hasAnyAuthority("ADMIN", "USER")
.requestMatchers(HttpMethod.POST, "/api/units/*/round/stop").hasAnyAuthority("ADMIN", "USER")
```

| Status | Task |
|---|---|
| ✅ | Add `scheduler.round.tick-ms` and `scheduler.round.interval-ms` to `application.properties` and `.env.example` |
| ✅ | Verify `updateSchedule(String, LocalTime, LocalTime)` exists in `UnitRepository` port (declared in Layer 6) — no new declaration needed here |
| ✅ | Add 3 new domain exceptions: `RoundAlreadyActiveException`, `RoundNotActiveException`, `ScheduleNotConfiguredException` (`UnitNotActiveException` declared in Layer 6) |
| ✅ | Add 3 `@ExceptionHandler` entries to `GlobalExceptionHandler` (409 each) + verify `UnitNotActiveException` handler exists from Layer 6 |
| ✅ | Create `ManageRoundUseCase` port (`application/port/in/`) — `startRound(numUnidad, coordinateMode, manualLat, manualLon)`, `stopRound`, `isRoundActive`, `getRoundCoordinateMode` |
| ✅ | Add `getRoundCoordinateMode(String numUnidad)` to `ManageRoundUseCase` port — returns `@Nullable CoordinateMode` |
| ✅ | Create `RoundState` record (`application/service/RoundState.java`) — 6 fields including `coordinateMode`, `manualLat`, `manualLon` |
| ✅ | Create `RoundManagementService` (`application/service/`) — constructor injects `gpsProvider`; `startRound()` reads schedule from DB, throws `ScheduleNotConfiguredException` on sentinel; `processTick()` branches on MANUAL/AUTOMATIC; `getRoundCoordinateMode()` is O(1) map lookup; auto-removes on `GpsProviderUnavailableException` and `UnitNotActiveException` |
| ✅ | Implement `getRoundCoordinateMode()` in `RoundManagementService` — `activeRounds.get(numUnidad)`, returns `state.coordinateMode()` or `null` |
| ✅ | Add `Clock` `@Bean` + `roundManagementService()` `@Bean` to `ApplicationConfig` — include `GpsCoordinateProvider gpsProvider` param; return type `RoundManagementService` |
| ✅ | Create `RoundTickService` (`infrastructure/scheduler/`) — `@Scheduled(fixedRateString)`, delegates to `processTick()` |
| ✅ | Add `roundTickService()` `@Bean` to `SchedulerConfig` |
| ✅ | Create `RoundStartRequest` DTO record (`infrastructure/adapter/in/web/dto/`) — 3 fields: `coordinateMode` (@NotNull), `lat`, `lon` (@Nullable BigDecimal) — no hours fields |
| ✅ | Create `RoundController` with start + stop endpoints + OpenAPI `@Tag`/`@Operation`/`@ApiResponse` |
| ✅ | Add 2 `requestMatchers` entries to `SecurityConfig` matrix |
| ✅ | `mvn test` — ArchUnit passes (no `@Component` on `RoundTickService`, no Spring imports in `RoundManagementService`) |

Exit condition: `POST /api/units/{numUnidad}/round/start` with MANUAL coords returns 204. Missing lat/lon for MANUAL returns 400. AUTOMATIC mode returns 400 (FIXME-PHASE6 guard). `processTick()` dispatches the unit on the next qualifying tick using stored coords (verified via unit test with `Clock.fixed`). `processTick()` auto-removes round on `GpsProviderUnavailableException` (AUTOMATIC path) and `UnitNotActiveException`. `POST /api/units/{numUnidad}/round/stop` returns 204 and the unit no longer dispatches. Double-start returns 409. Stop when not active returns 409. `startRound()` with sentinel schedule throws `ScheduleNotConfiguredException` — caller must configure via `PUT /schedule` first (verified in `RoundManagementServiceTest`). `Clock @Bean` resolves `FIXME-CLOCK`. ArchUnit passes. `mvn test` green.

---

### Layer 8 — Force Dispatch + Provider Dry-run

> ⛔ Cannot begin until Layer 4 complete (`SendPulseUseCase.dispatch()` method) and Layer 3 complete
> (`GpsCoordinateProvider` bean). May be implemented in parallel with Layers 6 and 7.

#### 8A — Force Dispatch (`PulseController`)

File: `infrastructure/adapter/in/web/PulseController.java`

**New `ForcePulseRequest` DTO** — `infrastructure/adapter/in/web/dto/ForcePulseRequest.java`:

```java
public record ForcePulseRequest(
    @NotNull CoordinateMode coordinateMode,
    @Nullable BigDecimal lat,    // required when coordinateMode == MANUAL
    @Nullable BigDecimal lon     // required when coordinateMode == MANUAL
) {}
```

**Endpoint contract:**

```
POST /api/units/{numUnidad}/pulse/force
  Authority: ADMIN or USER
  Body:      ForcePulseRequest
  204 No Content  — dispatch confirmed by QSolutions
  400             — MANUAL mode with missing or out-of-range coordinates
  401             — missing or invalid token
  403             — valid token, insufficient authority
  404             — unit not found
  409             — unit inactive (UnitNotActiveException from dispatch())
  502             — QSolutions rejected (PulseSendException)
  503             — QSolutions unreachable (GpsProviderUnavailableException)

  Note: coordinateMode=AUTOMATIC accepted in DTO but returns 400 (FIXME-PHASE6 guard).
        API contract shape is stable — Phase 6 removes the guard to enable AUTOMATIC mode.
```

**Controller:**

```java
@RestController
@RequestMapping("/api/units")
@Tag(name = "Pulse Dispatch", description = "Force and test GPS pulse dispatch to QSolutions")
public class PulseController {

    private final SendPulseUseCase sendPulseUseCase;

    public PulseController(SendPulseUseCase sendPulseUseCase) {
        this.sendPulseUseCase = Objects.requireNonNull(sendPulseUseCase);
    }

    @PostMapping("/{numUnidad}/pulse/force")
    @Operation(summary = "Force-dispatch GPS pulse for a single unit with operator-supplied coordinates")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Pulse dispatched and confirmed"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid coordinates"),
        @ApiResponse(responseCode = "404", description = "Unit not found"),
        @ApiResponse(responseCode = "409", description = "Unit is inactive"),
        @ApiResponse(responseCode = "502", description = "QSolutions rejected the pulse"),
        @ApiResponse(responseCode = "503", description = "QSolutions unreachable")
    })
    public ResponseEntity<Void> forceDispatch(
            @PathVariable String numUnidad,
            @Valid @RequestBody ForcePulseRequest request) {

        // FIXME-PHASE6: remove this guard when TraccarCoordinateAdapter is wired (Phase 6)
        if (request.coordinateMode() == CoordinateMode.AUTOMATIC) {
            throw new InvalidCoordinateException("AUTOMATIC mode not supported until Phase 6");
        }

        if (request.lat() == null || request.lon() == null) {
            throw new InvalidCoordinateException("MANUAL mode requires lat and lon");
        }

        GpsReading reading = new GpsReading(
            numUnidad, request.lat(), request.lon(),
            ZonedDateTime.now(FleetConstants.FLEET_TIMEZONE), ProviderType.MANUAL
        );

        sendPulseUseCase.dispatch(numUnidad, reading);
        return ResponseEntity.noContent().build();
    }
}
```

Rules:
- `GlobalExceptionHandler` maps `UnitNotFoundException` → 404, `PulseSendException` → 502, `GpsProviderUnavailableException` → 503, `UnitNotActiveException` → 409, `InvalidCoordinateException` → 400.
- `GpsReading` constructor validates coordinate range — out-of-range or `0.0, 0.0` throws `InvalidCoordinateException` which propagates to GlobalExceptionHandler.
- Authorization delegated to `SecurityConfig` matrix — no `@PreAuthorize` on the method (ADR-008).
- Inject `SendPulseUseCase` driving port, never `PulseOrchestrationService` directly.

**Add to `SecurityConfig` authorization matrix** (before `anyRequest().denyAll()`):

```java
.requestMatchers(HttpMethod.POST, "/api/units/*/pulse/force").hasAnyAuthority("ADMIN", "USER")
```

#### 8B — Provider Dry-run (`ProviderTestController`)

Pulled forward from old ROADMAP Phase 7. Tests the configured `GpsCoordinateProvider` — never calls `PulseSender`.

**New port:** `TestProviderUseCase` — `application/port/in/TestProviderUseCase.java`:

```java
GpsReading testProvider(String numUnidad);
GpsReading testProviderWithOverride(String numUnidad, BigDecimal lat, BigDecimal lon);
```

**New service:** `ProviderTestService` — `application/service/ProviderTestService.java`

Implements `TestProviderUseCase`. Declared as `@Bean` in `ApplicationConfig`. No `@Service`. No `@Transactional`. Receives `Clock` via constructor (needed for `testProviderWithOverride` timestamp). `PulseSender` is NOT injected — absence enforced by ArchUnit rule `providerTestServiceDoesNotDeclareFieldOfTypePulseSender`.

```
testProvider(String numUnidad):
  1. unit = unitRepository.findByNumUnidad(numUnidad)
               .orElseThrow(UnitNotFoundException)      // 404

  2. if !gpsProvider.isAvailable(numUnidad):
       throw GpsProviderUnavailableException(numUnidad) // 503

  3. return gpsProvider.getCoordinates(numUnidad)       // returns GpsReading, NEVER calls PulseSender

testProviderWithOverride(String numUnidad, BigDecimal lat, BigDecimal lon):
  1. unit = unitRepository.findByNumUnidad(numUnidad)
               .orElseThrow(UnitNotFoundException)      // 404

  2. return new GpsReading(numUnidad, lat, lon,
                 ZonedDateTime.now(clock), ProviderType.MANUAL)
     // GpsReading constructor validates range — throws InvalidCoordinateException if OOB
     // NEVER calls gpsProvider — bypasses configured provider
     // NEVER calls PulseSender
```

**New request DTO:** `ManualCoordinateTestRequest` — `infrastructure/adapter/in/web/dto/ManualCoordinateTestRequest.java`:

```java
public record ManualCoordinateTestRequest(
    @NotNull BigDecimal lat,
    @NotNull BigDecimal lon
) {}
```

Used by `POST /{numUnidad}/pulse/test-manual` to supply override coordinates. Validates non-null only at DTO level; range validation is in `GpsReading` constructor (domain layer).

**New response DTO:** `ProviderTestResponse` — `infrastructure/adapter/in/web/dto/ProviderTestResponse.java`:

```java
public record ProviderTestResponse(
    String numUnidad,
    BigDecimal lat,
    BigDecimal lon,
    ZonedDateTime timestamp,
    ProviderType providerType
) {
    public static ProviderTestResponse from(GpsReading reading) {
        return new ProviderTestResponse(
            reading.getNumUnidad(), reading.getLatitud(), reading.getLongitud(),
            reading.getFechaHoraEvento(), reading.getProviderType()
        );
    }
}
```

**New controller:** `ProviderTestController` — `infrastructure/adapter/in/web/ProviderTestController.java`:

```
GET /api/units/{numUnidad}/pulse/test
  Authority: ADMIN or USER
  Body:      none
  200: ProviderTestResponse  — coordinates from configured provider
  404: unit not found
  503: provider not available for this unit

POST /api/units/{numUnidad}/pulse/test-manual
  Authority: ADMIN or USER
  Body:      ManualCoordinateTestRequest (required)
             { "lat": 19.4326, "lon": -99.1332 }
  200: ProviderTestResponse  — coordinates reflect the supplied override values
  400: lat or lon null / out of range (InvalidCoordinateException from GpsReading constructor)
  404: unit not found
  Note: does NOT call the configured GpsCoordinateProvider or PulseSender.
        Useful for verifying the SOAP dispatch flow with known safe coordinates.
```

**`ApplicationConfig` additions:**

```java
@Bean
public TestProviderUseCase providerTestService(
        UnitRepository unitRepository,
        GpsCoordinateProvider gpsProvider,
        Clock clock) {
    return new ProviderTestService(unitRepository, gpsProvider, clock);
}
```

**Add to `SecurityConfig` authorization matrix:**

```java
.requestMatchers(HttpMethod.GET, "/api/units/*/pulse/test").hasAnyAuthority("ADMIN", "USER")
.requestMatchers(HttpMethod.POST, "/api/units/*/pulse/test-manual").hasAnyAuthority("ADMIN", "USER")
```

**Task list for Layer 8:**

| Status | Task |
|---|---|
| ✅ | Create `ForcePulseRequest` DTO record |
| ✅ | Update `PulseController` — validate `coordinateMode`, assemble `GpsReading`, call `sendPulseUseCase.dispatch()` |
| ✅ | Add `FIXME-PHASE6` comment in `PulseController` for AUTOMATIC mode guard |
| ✅ | Add `POST /api/units/*/pulse/force` to `SecurityConfig` matrix |
| ✅ | Create `TestProviderUseCase` port (`application/port/in/`) — declare both `testProvider()` and `testProviderWithOverride()` |
| ✅ | Create `ProviderTestService` (`application/service/`) — inject `Clock`; no `PulseSender` injection |
| ✅ | Implement `testProvider()` — provider availability check + `getCoordinates()`; never calls `PulseSender` |
| ✅ | Implement `testProviderWithOverride()` — builds `GpsReading` with caller-supplied coords + `ZonedDateTime.now(clock)`; never calls `gpsProvider` or `PulseSender` |
| ✅ | Add `providerTestService()` @Bean to `ApplicationConfig` — pass `Clock` parameter |
| ✅ | Create `ManualCoordinateTestRequest` DTO record (`@NotNull lat`, `@NotNull lon`) |
| ✅ | Create `ProviderTestResponse` DTO record |
| ✅ | Create `ProviderTestController` with `GET /{numUnidad}/pulse/test` and `POST /{numUnidad}/pulse/test-manual` |
| ✅ | Add `POST /api/units/*/pulse/test-manual` to `SecurityConfig` matrix |
| ✅ | Add `GET /api/units/*/pulse/test` to `SecurityConfig` matrix |
| ✅ | `mvn test` — ArchUnit passes (no `PulseSender` field in `ProviderTestService`) |

Exit condition: `POST /pulse/force` with MANUAL coords returns 204. Missing coords returns 400. `GET /pulse/test` returns `ProviderTestResponse` with zero SOAP calls — verified by `verify(sendPulseUseCase, never()).dispatch(any(), any())` in 9.10. ArchUnit passes.

---

### Layer 9 — Tests

**Exit condition for Phase 4:** `mvn test` passes with zero failures. All 11 component tests green. `@Disabled` live integration test run manually with `Protocolo.isProcessed() == true` evidence in release notes.

**Result (2026-06-23):** ✅ 235 passing, 2 skipped (@Disabled live gate). All spec cases implemented including previously missing: timezone conversion assertion (9.1.6), password-not-logged assertion (9.1.7), timestamp freshness (9.2.7), all-5-units coverage (9.2.8), `sendPulse` exception propagation (9.3.16–17), `dispatch_neverCallsGpsProvider` ADR-016 enforcement (9.3.18).

**Live gate PASSED (2026-06-23 17:01 CST):** `PULSE_SENT numUnidad=Peugeot tracking=test-tracking receptionDate=2026-06-23T17:01:45.703765300-06:00[America/Mexico_City]` — `Protocolo.isProcessed() == true` confirmed against live QSolutions endpoint. All 4 Phase 4 exit conditions met. ✅

#### Dependency Rules

- All unit tests (9.1–9.6): no Spring context, no I/O
- Controller tests (9.7–9.10): `@SpringBootTest` + MockMvc
- AAA pattern (`// Arrange`, `// Act`, `// Assert`) mandatory in every test method
- AssertJ (`assertThat`) for all assertions
- `ArgumentCaptor` to verify the exact `GPSInfo` passed to the SOAP port
- `verify(..., never())` to assert skip conditions did NOT invoke `pulseSender.send()`

#### Component Map

| Order | Component | File | Test Type | Depends On | Status |
|---|---|---|---|---|---|
| 9.1 | QSolutionsSoapAdapterTest | `QSolutionsSoapAdapterTest.java` | Unit (7 cases) + @Disabled live gate | Layer 2 | ✅ |
| 9.2 | ManualCoordinateAdapterTest | `ManualCoordinateAdapterTest.java` | Unit — 8 cases (+2 extra) | Layer 3 | ✅ |
| 9.3 | PulseOrchestrationServiceTest | `PulseOrchestrationServiceTest.java` | Unit (Mockito) — 18 cases (+4 extra) | Layer 4 | ✅ |
| 9.4 | RoundManagementServiceTest | `RoundManagementServiceTest.java` | Unit (Mockito + Clock) — 25 cases (+2 extra) | Layer 7 | ✅ |
| 9.5 | UnitManagementServiceTest | `UnitManagementServiceTest.java` | Unit (Mockito) — 11 cases (+1 extra) | Layer 6 | ✅ |
| 9.6 | ProviderTestServiceTest | `ProviderTestServiceTest.java` | Unit (Mockito + Clock) — 8 cases | Layer 8 | ✅ |
| 9.7 | PulseControllerTest | `PulseControllerTest.java` | Controller integration — 9 cases | Layer 8 | ✅ |
| 9.8 | RoundControllerTest | `RoundControllerTest.java` | Controller integration — 13 cases | Layer 7 | ✅ |
| 9.9 | UnitControllerTest | `UnitControllerTest.java` | Controller integration — 11 cases (+1 extra) | Layer 6 | ✅ |
| 9.10 | ProviderTestControllerTest | `ProviderTestControllerTest.java` | Controller integration — 9 cases | Layer 8 | ✅ |
| 9.11 | ArchUnit additions | `HexagonalArchitectureTest.java` | Architecture — 2 new Phase 4 rules | All layers | ✅ |

#### Discovered Additional Cases (SUT analysis surfaced during implementation)

| # | Test Class | Extra Case | Rationale |
|---|---|---|---|
| +1 | `PulseOrchestrationServiceTest` | `dispatch_withNullGpsReading_throwsNullPointerException` | `dispatch()` null-guard via `Objects.requireNonNull` — not in original spec but enforced by constructor of `ScheduledPulse` |
| +2 | `RoundManagementServiceTest` | `startRound_withZeroZeroCoordinates_throwsInvalidCoordinateException` | `GpsReading` constructor rejects `(0.0, 0.0)` as "GPS not locked" — happens BEFORE sentinel schedule check |
| +3 | `RoundManagementServiceTest` | `processTick_whenUltimoEnvioIsNull_dispatches` | First-tick case: `ultimoEnvio == null` is an explicit branch in `processTick()` that skips the cooldown check entirely |
| +4 | `UnitManagementServiceTest` | `updateSchedule_withUnknownUnit_throwsUnitNotFoundException` | Unit lookup must be the first DB call in `updateSchedule()` — validates "not-found before validation" ordering |
| +5 | `UnitControllerTest` | `activateUnit_withUserRole_returns403` | SecurityConfig's `hasAuthority("ADMIN")` on `PUT /api/units/*/activate` must reject USER at the filter/security level — not just at service level |

#### 9.1 — QSolutionsSoapAdapterTest

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

#### 9.2 — ManualCoordinateAdapterTest

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `isAvailable_whenUnitConfigured_returnsTrue` | Props contains `"Peugeot"` | `true` | Configured unit activates |
| 2 | `isAvailable_whenUnitAbsent_returnsFalse` | Props does not contain `"Unknown"` | `false` | Unconfigured unit skipped silently |
| 3 | `getCoordinates_returnsReadingWithCorrectFields` | `"Peugeot"` → lat=19.4326, lon=-99.1332 | `getNumUnidad()=="Peugeot"`, `getLatitud()==19.4326`, `getProviderType()==MANUAL` | Field mapping |
| 4 | `getCoordinates_fechaHoraEventoIsCurrentTime` | Any configured unit | `fechaHoraEvento` within 5 seconds of `ZonedDateTime.now(FLEET_TIMEZONE)` | Timestamp is dispatch time |
| 5 | `getCoordinates_whenUnitAbsent_throwsGpsProviderUnavailableException` | Unconfigured unit | `GpsProviderUnavailableException` | Defensive guard |
| 6 | `isAvailable_forAllFiveFleetUnits_returnsTrue` | Props with all 5 `numUnidad` values | All return `true` | Full fleet configured |

#### 9.3 — PulseOrchestrationServiceTest (14 cases)

```java
@ExtendWith(MockitoExtension.class)
class PulseOrchestrationServiceTest {

    @Mock UnitRepository unitRepository;
    @Mock GpsCoordinateProvider gpsProvider;
    @Mock PulseSender pulseSender;

    private static final Clock CLOCK = Clock.fixed(
        Instant.parse("2026-06-18T20:00:00Z"), ZoneId.of("America/Mexico_City"));
    private PulseOrchestrationService service;

    @BeforeEach
    void setUp() {
        service = new PulseOrchestrationService(unitRepository, gpsProvider, pulseSender, CLOCK, "1");
    }

    // Builds an active unit with a 00:00–23:59 window (always in-window)
    private Unit activeUnit(String numUnidad) {
        return new Unit(numUnidad, false, LocalTime.MIN, LocalTime.MAX, null, true);
    }
}
```

**`sendPulse()` test cases (window-checked, provider-backed):**

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

**`dispatch()` test cases (no window, no provider):**

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 10 | `dispatch_happyPath_callsPulseSenderWithProvidedReading` | Active unit; provided `GpsReading` | `pulseSender.send(capture)` called; captured reading == provided reading | dispatch() uses caller-supplied reading |
| 11 | `dispatch_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException`; `pulseSender.send` NEVER called | Not-found guard |
| 12 | `dispatch_whenUnitInactive_throwsUnitNotActiveException` | `unit.isActive() == false` | `UnitNotActiveException` thrown (not silent skip) | Throws, does not skip |
| 13 | `dispatch_doesNotCheckWindow` | Active unit; window set to `(23:58, 23:59)` — would be "out of window" | `pulseSender.send` called regardless | No window check in dispatch() |
| 14 | `dispatch_doesNotCallGpsProvider` | Any active unit | `verify(gpsProvider, never()).getCoordinates(any())` and `verify(gpsProvider, never()).isAvailable(any())` | No provider call |

#### 9.4 — RoundManagementServiceTest (23 cases)

File: `src/test/java/com/fleetpulse/api/application/service/RoundManagementServiceTest.java`

```java
@ExtendWith(MockitoExtension.class)
class RoundManagementServiceTest {

    @Mock UnitRepository unitRepository;
    @Mock SendPulseUseCase sendPulseUseCase;
    @Mock GpsCoordinateProvider gpsProvider;

    // Fixed clock: 2026-06-18T20:00:00Z = 14:00 America/Mexico_City
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-18T20:00:00Z");
    private static final ZoneId FLEET_ZONE = ZoneId.of("America/Mexico_City");
    private Clock clock;
    private RoundManagementService service;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(FIXED_INSTANT, FLEET_ZONE);
        service = new RoundManagementService(unitRepository, sendPulseUseCase, gpsProvider, clock, 900_000L);
    }

    private Unit activeUnit(boolean horarioFijo, LocalTime inicio, LocalTime fin) {
        return new Unit("Peugeot", horarioFijo, inicio, fin, null, true);
    }
}
```

> `@InjectMocks` cannot be used — constructor has non-mock `Clock` and `long` parameters.
> Construct manually in `@BeforeEach`. No Spring context. No I/O.

**`startRound()` test cases:**

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `startRound_horarioFijo_usesDbValues` | `horarioFijo=true` | Map contains unit with `unit.getHoraInicio()`/`getHoraFin()`; `updateSchedule` NEVER called | DB values always read |
| 2 | `startRound_horarioFijoFalse_storedValues_usesStoredValues` | `horarioFijo=false`; non-sentinel stored times | Map contains stored hours; `updateSchedule` NEVER called | Reads from DB without writing |
| 3 | `startRound_horarioFijoFalse_sentinelSchedule_throwsScheduleNotConfigured` | `horarioFijo=false`; `horaInicio==LocalTime.MIN AND horaFin==LocalTime.MAX` (sentinel) | `ScheduleNotConfiguredException`; map empty; `updateSchedule` NEVER called | Sentinel guard — configure via PUT /schedule first |
| 4 | `startRound_whenAlreadyActive_throwsRoundAlreadyActiveException` | Same unit started twice | `RoundAlreadyActiveException` on second call; `updateSchedule` NEVER called on either call | Fast rejection — no DB write |
| 5 | `startRound_whenUnitNotFound_throws` | `findByNumUnidad` → empty | `UnitNotFoundException`; map empty | Not-found guard |
| 6 | `startRound_whenUnitInactive_throws` | `unit.isActive() == false` | `UnitNotActiveException`; map empty | Inactive guard before map check |

**`stopRound()` test cases:**

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 7 | `stopRound_whenActive_removesFromMap` | Round started then stopped | Map empty after stop; `isRoundActive` → false | Stop removes entry |
| 8 | `stopRound_whenNotActive_throws` | No round started | `RoundNotActiveException` | Atomic remove contract |

**`processTick()` test cases — all use `Clock.fixed`:**

| # | Method name | Clock / state setup | Expected | What it proves |
|---|---|---|---|---|
| 9 | `processTick_unitInWindow_firstSend_dispatches` | now=14:00; window 08:00–18:00; `ultimoEnvio=null` | `dispatch("Peugeot", reading)` called; `isRoundActive` stays true; `ultimoEnvio` updated | First dispatch in round |
| 10 | `processTick_unitBeforeWindow_notDispatched` | now=07:00; window 08:00–18:00 | `dispatch` NEVER called | Before-window guard |
| 11 | `processTick_unitAfterWindow_notDispatched` | now=19:00; window 08:00–18:00 | `dispatch` NEVER called | After-window guard |
| 12 | `processTick_cooldownNotElapsed_notDispatched` | now=14:00; `ultimoEnvio`=5 min ago; interval=15 min | `dispatch` NEVER called | Cooldown guard |
| 13 | `processTick_cooldownElapsed_dispatches` | now=14:00; `ultimoEnvio`=16 min ago; interval=15 min | `dispatch` called; `ultimoEnvio` updated to `FIXED_INSTANT` | Cooldown elapsed |
| 14 | `processTick_multipleUnits_onlyQualifyingDispatched` | 3 units: one in window + elapsed, one outside window, one cooldown not elapsed | Exactly 1 dispatch | Partial qualification |
| 15 | `processTick_sendPulseThrows_tickContinuesOtherUnits` | Unit A throws; Unit B qualifies | Both `dispatch()` attempted; Unit B dispatched despite A's exception | Per-unit error isolation |
| 16 | `processTick_emptyMap_noDispatches` | No rounds started | `dispatch` NEVER called | Empty map safe |

**`isRoundActive()` test cases:**

| # | Method name | Condition | Expected |
|---|---|---|---|
| 17 | `isRoundActive_whenActive_returnsTrue` | Round started | `true` |
| 18 | `isRoundActive_whenNotActive_returnsFalse` | No round | `false` |

**`coordinateMode` test cases:**

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 19 | `startRound_manualMode_storesCoordinatesInRoundState` | `coordinateMode=MANUAL`, lat=19.4326, lon=-99.1332 | `RoundState.manualLat()==19.4326`, `manualLon()==-99.1332`, `coordinateMode==MANUAL` | Coords stored in state |
| 20 | `startRound_manualMode_nullLat_throwsInvalidCoordinateException` | `coordinateMode=MANUAL`, lat=null | `InvalidCoordinateException`; map empty | MANUAL requires coords |
| 21 | `processTick_manualMode_callsDispatchNotSendPulse` | MANUAL round in window, cooldown elapsed | `verify(sendPulseUseCase).dispatch(eq("Peugeot"), any(GpsReading.class))`; `verify(sendPulseUseCase, never()).sendPulse(any())` | `dispatch()` called, not `sendPulse()` |
| 22 | `processTick_automaticMode_callsProviderThenDispatch` | AUTOMATIC round in window, cooldown elapsed; provider returns reading | `verify(gpsProvider).getCoordinates("Peugeot")`; `verify(sendPulseUseCase).dispatch(eq("Peugeot"), any())` | AUTOMATIC flows through provider |
| 23 | `processTick_automaticMode_providerUnavailable_removesRound` | AUTOMATIC round; `gpsProvider.getCoordinates()` throws `GpsProviderUnavailableException` | `isRoundActive("Peugeot")` → false after tick; `sendPulseUseCase.dispatch` NEVER called | Auto-remove on provider failure |

Rules:
- ALL `startRound()` tests: `verify(unitRepository, never()).updateSchedule(any(), any(), any())` — mandatory assertion in every test. Schedule is always read, never written, in `startRound()`.
- Tests #3, #5, #6, #20: `assertThat(service.isRoundActive("Peugeot")).isFalse()` after exception — map must be empty.
- Tests #10, #11, #12: `verify(sendPulseUseCase, never()).sendPulse(any())` and `verify(sendPulseUseCase, never()).dispatch(any(), any())` — mandatory.
- Test #15: two units added to map; mock throws on first, returns normally on second; assert both `dispatch()` calls attempted; Unit B dispatched.
- Tests #11, #15: verify `ultimoEnvio` was updated by advancing clock past cooldown on a second `processTick()` call — confirm second call dispatches again.
- Test #25: verify `activeRounds` no longer contains unit after `GpsProviderUnavailableException` — `isRoundActive` returns false.

Exit condition: 23 tests green. ALL `startRound()` tests assert `verify(unitRepository, never()).updateSchedule(...)` — schedule is read-only in `startRound()`. Test #21 verifies `dispatch()` is called instead of `sendPulse()` for MANUAL rounds — this distinction is architecturally critical (ADR-016).

#### 9.5 — UnitManagementServiceTest (10 cases)

```java
@ExtendWith(MockitoExtension.class)
class UnitManagementServiceTest {

    @Mock UnitRepository unitRepository;
    private UnitManagementService service;

    @BeforeEach
    void setUp() {
        service = new UnitManagementService(unitRepository);
    }
}
```

> No createUnit tests — 5-unit fleet is fixed; units are seeded by Flyway migration, not created via API.
> No callerRole tests — role authorization belongs in the controller layer (`@PreAuthorize`), not the service.

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `updateSchedule_withHours_persistsSuppliedHours` | Hours supplied | `unitRepository.updateSchedule("Peugeot", horaInicio, horaFin)` called with exact values | Schedule persisted to DB |
| 2 | `updateSchedule_noHours_resetsToSentinel` | `horaInicio=null`; `horaFin=null` | `unitRepository.updateSchedule("Peugeot", LocalTime.MIN, LocalTime.MAX)` called | ADR-013 sentinel reset |
| 3 | `updateSchedule_invalidWindow_throwsScheduleConflictException` | `horaInicio` after `horaFin` | `ScheduleConflictException`; `updateSchedule` NEVER called | Window validation before DB |
| 4 | `updateSchedule_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException`; `updateSchedule` NEVER called | Not-found guard |
| 5 | `setHorarioFijo_true_callsRepository` | `horarioFijo=true` | `unitRepository.setHorarioFijo("Peugeot", true)` called | Repository delegation |
| 6 | `setHorarioFijo_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException`; `setHorarioFijo` NEVER called | Not-found guard |
| 7 | `setActive_deactivate_callsRepository` | `active=false` | `unitRepository.setActive("Peugeot", false)` called | Soft deactivation |
| 8 | `setActive_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException`; `setActive` NEVER called | Not-found guard |
| 9 | `listAll_returnsAllUnits` | 5 units in repo | Returns list of 5 | Repository pass-through |
| 10 | `findByNumUnidad_whenNotFound_returnsEmptyOptional` | Unit absent | `Optional.isEmpty()` | Pass-through |

Rules:
- Test #2: `verify(unitRepository).updateSchedule(eq("Peugeot"), eq(LocalTime.MIN), eq(LocalTime.MAX))` — sentinel values mandatory, proves ADR-013.
- Tests #3, #4, #6, #8: verify NEVER call on mutating repository method — proves no DB side effect on exception path.
- Role-based access (ADMIN-only for `setHorarioFijo`) is enforced by `@PreAuthorize` in `UnitController`, not verified here.

---

#### 9.6 — ProviderTestServiceTest (8 cases)

```java
@ExtendWith(MockitoExtension.class)
class ProviderTestServiceTest {

    @Mock UnitRepository unitRepository;
    @Mock GpsCoordinateProvider gpsProvider;
    private static final Instant FIXED_INSTANT = Instant.parse("2026-06-18T20:00:00Z");
    private static final ZoneId FLEET_ZONE = ZoneId.of("America/Mexico_City");
    private ProviderTestService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(FIXED_INSTANT, FLEET_ZONE);
        service = new ProviderTestService(unitRepository, gpsProvider, clock);
    }
}
```

**`testProvider()` test cases:**

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 1 | `testProvider_happyPath_returnsGpsReading` | Unit found; provider available; returns reading | `GpsReading` returned; no `PulseSender` call (not injected) | Dry-run: zero SOAP |
| 2 | `testProvider_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException` | Not-found guard |
| 3 | `testProvider_whenProviderUnavailable_throwsGpsProviderUnavailableException` | `isAvailable` → false | `GpsProviderUnavailableException` | Provider check |
| 4 | `testProvider_returnsReadingFromProvider` | `getCoordinates` returns specific reading | Returned reading equals provider result | Pass-through |
| 5 | `testProvider_doesNotCallGetCoordinatesWhenNotAvailable` | `isAvailable` → false | `verify(gpsProvider, never()).getCoordinates(any())` | Avoids unnecessary call |

**`testProviderWithOverride()` test cases:**

| # | Method name | Condition | Expected | What it proves |
|---|---|---|---|---|
| 6 | `testProviderWithOverride_happyPath_returnsReadingWithSuppliedCoords` | Unit found; lat=19.4326; lon=-99.1332 | Returned `GpsReading` has `lat==19.4326`, `lon==-99.1332`, `providerType==MANUAL`, timestamp from `Clock.fixed` | Override coords used; clock injected |
| 7 | `testProviderWithOverride_neverCallsGpsProvider` | Any unit found | `verify(gpsProvider, never()).getCoordinates(any())`; `verify(gpsProvider, never()).isAvailable(any())` | Bypasses configured provider |
| 8 | `testProviderWithOverride_whenUnitNotFound_throwsUnitNotFoundException` | `findByNumUnidad` → empty | `UnitNotFoundException` | Not-found guard |

---

#### 9.7 — PulseControllerTest (9 cases)

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
| 1 | `forceDispatch_withAdminToken_returns204` | Admin JWT; body `{"coordinateMode":"MANUAL","lat":19.43,"lon":-99.13}`; use case → void | 204 | ADMIN authorized |
| 2 | `forceDispatch_withUserToken_returns204` | User JWT; valid MANUAL body | 204 | USER authorized |
| 3 | `forceDispatch_withoutToken_returns401` | No `Authorization` header | 401 | Anonymous blocked |
| 4 | `forceDispatch_whenUnitNotFound_returns404WithProblemDetail` | throws `UnitNotFoundException` | 404; `application/problem+json`; `$.type` ends `/errors/unit-not-found` | GlobalExceptionHandler 404 mapping |
| 5 | `forceDispatch_whenQSolutionsRejects_returns502WithProblemDetail` | throws `PulseSendException` | 502; `application/problem+json`; `$.type` ends `/errors/soap-rejected` | GlobalExceptionHandler 502 mapping |
| 6 | `forceDispatch_whenQSolutionsUnreachable_returns503WithProblemDetail` | throws `GpsProviderUnavailableException` | 503; `application/problem+json`; `$.type` ends `/errors/service-unavailable` | GlobalExceptionHandler 503 mapping |
| 7 | `forceDispatch_withMissingLat_returns400ProblemDetail` | Body `{"coordinateMode":"MANUAL","lon":-99.13}` | 400; `$.type` ends `/errors/invalid-coordinates` | MANUAL requires both coords |
| 8 | `forceDispatch_withInvalidCoordinates_returns400ProblemDetail` | Body with `lat=91.0` | 400; `$.type` ends `/errors/invalid-coordinates` | `GpsReading` constructor validation |
| 9 | `forceDispatch_withAutomaticMode_returns400ProblemDetail` | Body `{"coordinateMode":"AUTOMATIC"}` | 400 (`FIXME-PHASE6` guard fires) | AUTOMATIC not yet wired |

Tests #4, #5, #6: `.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))` mandatory.

#### 9.8 — RoundControllerTest (13 cases)

File: `src/test/java/com/fleetpulse/api/infrastructure/adapter/in/web/RoundControllerTest.java`

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RoundControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenService tokenService;
    @MockitoBean ManageRoundUseCase manageRoundUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;
    @MockitoBean SendPulseUseCase sendPulseUseCase;  // prevent real dispatches during context
}
```

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 1 | `startRound_withAdminToken_returns204` | Admin JWT; `coordinateMode=MANUAL`, valid coords; use case → void | 204 | ADMIN authorized |
| 2 | `startRound_withUserToken_returns204` | User JWT; valid body | 204 | USER authorized |
| 3 | `startRound_withoutToken_returns401` | No `Authorization` header | 401 | Anonymous blocked |
| 4 | `startRound_manualMode_withCoords_passesLatLonToUseCase` | Body with `lat=19.4326`, `lon=-99.1332`; Admin JWT | 204; use case called with `lat=19.4326`, `lon=-99.1332` | Coord body parsed and forwarded |
| 5 | `startRound_manualMode_missingCoords_returns400` | Body `{"coordinateMode":"MANUAL"}`; Admin JWT | 400; `$.type` ends `/errors/invalid-coordinates` | MANUAL requires lat + lon |
| 6 | `startRound_automaticMode_returns204` | Body `{"coordinateMode":"AUTOMATIC"}`; Admin JWT; use case → void | 204 | AUTOMATIC accepted by controller |
| 7 | `startRound_whenRoundAlreadyActive_returns409ProblemDetail` | throws `RoundAlreadyActiveException` | 409; `application/problem+json`; `$.type` ends `/errors/round-already-active` | Handler mapping |
| 8 | `startRound_whenUnitNotFound_returns404ProblemDetail` | throws `UnitNotFoundException` | 404; `$.type` ends `/errors/unit-not-found` | Existing handler re-used |
| 9 | `startRound_whenScheduleNotConfigured_returns409ProblemDetail` | throws `ScheduleNotConfiguredException` | 409; `$.type` ends `/errors/schedule-not-configured` | New handler |
| 10 | `startRound_whenUnitInactive_returns409ProblemDetail` | throws `UnitNotActiveException` | 409; `$.type` ends `/errors/unit-not-active` | New handler |
| 11 | `stopRound_withAdminToken_returns204` | Admin JWT; use case → void | 204 | Stop happy path |
| 12 | `stopRound_whenRoundNotActive_returns409ProblemDetail` | throws `RoundNotActiveException` | 409; `$.type` ends `/errors/round-not-active` | Stop guard mapping |
| 13 | `startRound_withInvalidCoordinates_returns400ProblemDetail` | Body with `lat=91.0` | 400; `$.type` ends `/errors/invalid-coordinates` | Coord range validation |

Rules:
- Tests #7, #9, #10, #12: `.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))` — mandatory on every 4xx.
- Test #4: use `ArgumentCaptor<BigDecimal>` — verify exact lat/lon values forwarded to use case.
- `@MockitoBean SendPulseUseCase` prevents `RoundManagementService.processTick()` from attempting real dispatches if the context starts the tick scheduler.
- Schedule window (horaInicio/horaFin) is NOT part of the `RoundStartRequest` DTO — no test for parsing those fields.

Exit condition: 13 tests green. Every 4xx test verifies content type AND `$.type` URI. Test #4 verifies coord body forwarding to use case.

#### 9.9 — UnitControllerTest (10 cases)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UnitControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenService tokenService;
    @MockitoBean ManageUnitUseCase manageUnitUseCase;
    @MockitoBean ConfigureScheduleUseCase configureScheduleUseCase;
    @MockitoBean ManageRoundUseCase manageRoundUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;
}
```

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 1 | `listUnits_withAdminToken_returns200WithRoundActiveField` | Admin JWT; use case returns 2 units; `isRoundActive` → false | 200; array length 2; `$.content[0].roundActive == false` | List + `roundActive` field |
| 2 | `listUnits_withUserToken_returns200` | User JWT | 200 | USER can list |
| 3 | `listUnits_withoutToken_returns401` | No token | 401 | Anonymous blocked |
| 4 | `createUnit_withAdminToken_returns201WithLocation` | Admin JWT; valid body; use case → unit | 201; `Location` header contains `/api/units/Peugeot` | 201 + Location contract |
| 5 | `createUnit_withUserToken_returns403` | User JWT | 403 | ADMIN-only |
| 6 | `createUnit_whenDuplicate_returns409ProblemDetail` | throws `UnitAlreadyExistsException` | 409; `application/problem+json`; `$.type` ends `/errors/unit-already-exists` | Conflict mapping |
| 7 | `deactivateUnit_withAdminToken_returns204` | Admin JWT | 204 | DELETE (soft) happy path |
| 8 | `updateSchedule_withAdminToken_returns200` | Admin JWT; valid body | 200; `UnitResponse` includes updated hours | Schedule update |
| 9 | `updateSchedule_withUserToken_horarioFijoUnit_returns403` | User JWT; service throws `ForbiddenScheduleModificationException` | 403 | USER restriction enforced |
| 10 | `updateSchedule_invalidWindow_returns400` | throws `InvalidScheduleException` | 400; `$.type` ends `/errors/invalid-schedule` | Window validation mapping |

Rules:
- Test #1: `verify(manageRoundUseCase, times(2)).isRoundActive(any())` — once per unit in the list.
- Tests #6, #9, #10: `.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))` mandatory.

---

#### 9.10 — ProviderTestControllerTest (9 cases)

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProviderTestControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired TokenService tokenService;
    @MockitoBean TestProviderUseCase testProviderUseCase;
    @MockitoBean TokenBlacklist tokenBlacklist;
    @MockitoBean SendPulseUseCase sendPulseUseCase;  // assert never called
}
```

**`GET /{numUnidad}/pulse/test` test cases:**

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 1 | `testProvider_withAdminToken_returns200WithReading` | Admin JWT; service returns `GpsReading` | 200; body contains `numUnidad`, `lat`, `lon`, `providerType` | Happy path |
| 2 | `testProvider_withUserToken_returns200` | User JWT | 200 | USER can dry-run |
| 3 | `testProvider_withoutToken_returns401` | No token | 401 | Anonymous blocked |
| 4 | `testProvider_whenUnitNotFound_returns404` | throws `UnitNotFoundException` | 404; `application/problem+json` | Not-found mapping |
| 5 | `testProvider_whenProviderUnavailable_returns503` | throws `GpsProviderUnavailableException` | 503; `$.type` ends `/errors/service-unavailable` | Provider unavailable |
| 6 | `testProvider_neverCallsSendPulseUseCase` | Any successful call | `verify(sendPulseUseCase, never()).sendPulse(any())` and `verify(sendPulseUseCase, never()).dispatch(any(), any())` | Dry-run: zero SOAP |

**`POST /{numUnidad}/pulse/test-manual` test cases:**

| # | Method name | Setup | Expected | What it proves |
|---|---|---|---|---|
| 7 | `testManual_withAdminToken_returns200WithSuppliedCoords` | Admin JWT; body `{"lat":19.4326,"lon":-99.1332}`; service returns `GpsReading` | 200; `$.lat == 19.4326`; `$.lon == -99.1332`; `$.providerType == "MANUAL"` | Override coords reflected in response |
| 8 | `testManual_withInvalidCoordinates_returns400` | Body `{"lat":91.0,"lon":0.0}`; service throws `InvalidCoordinateException` | 400; `$.type` ends `/errors/invalid-coordinates` | Domain validation surfaced |
| 9 | `testManual_neverCallsSendPulseUseCase` | Any successful call | `verify(sendPulseUseCase, never()).sendPulse(any())` and `verify(sendPulseUseCase, never()).dispatch(any(), any())` | Dry-run: zero SOAP even with manual coords |

---

#### 9.11 — ArchUnit additions (✅ IMPLEMENTED)

File: `src/test/java/com/fleetpulse/api/architecture/HexagonalArchitectureTest.java` — added to existing class.

Two Phase 4 rules added (total: 11 rules now in class):

| Rule | ADR | What it enforces |
|---|---|---|
| `roundStateResidesInApplicationServicePackage` | ADR-015 | `RoundState` must reside in `application/service/` — moving to `infrastructure/` would require application to import infrastructure (ArchUnit violation) |
| `providerTestServiceDoesNotDependOnPulseSender` | ADR-016 | `ProviderTestService` must not import `PulseSender` — dry-run path intentionally has zero SOAP access |

Note: existing `applicationDoesNotImportInfrastructure` and `applicationDoesNotImportSpring` rules already cover `RoundManagementService`, `RoundState`, `UnitManagementService`. The two rules above add Phase 4-specific named enforcement.

#### Blocked States

```
9.1  QSolutionsSoapAdapterTest     → UNBLOCKED after Layer 2
9.2  ManualCoordinateAdapterTest   → UNBLOCKED after Layer 3
9.3  PulseOrchestrationServiceTest → UNBLOCKED after Layer 4
9.4  RoundManagementServiceTest    → UNBLOCKED after Layer 7
9.5  UnitManagementServiceTest     → UNBLOCKED after Layer 6
9.6  ProviderTestServiceTest       → UNBLOCKED after Layer 8
9.7  PulseControllerTest           → UNBLOCKED after Layer 8
9.8  RoundControllerTest           → UNBLOCKED after Layer 7
9.9  UnitControllerTest            → UNBLOCKED after Layer 6
9.10 ProviderTestControllerTest    → UNBLOCKED after Layer 8
9.11 ArchUnit additions            → UNBLOCKED after all layers complete

9.1–9.6 can be implemented in parallel (unit tests only, no Spring context).
9.7–9.10 require @SpringBootTest — implement after all production layers are complete.
9.11 runs last.
```

#### Live Integration Gate — ✅ PASSED 2026-06-23

**Evidence:** `POST /api/units/Peugeot/pulse/force` → `204 No Content`
**Log:** `PULSE_SENT numUnidad=Peugeot tracking=test-tracking receptionDate=2026-06-23T17:01:45.703765300-06:00[America/Mexico_City]`
**Confirmed:** `Protocolo.isProcessed() == true` against live QSolutions endpoint.

Tag `v0.4.0` and proceed to Phase 5.

---

## Phase 5 — Production Replacement Milestone 🏁
**Tag:** `v1.0.0`
**Exit condition:** (1) Token lifecycle verified: login → refresh single-use → logout → access token blacklisted. (2) Authorization matrix enforced: USER blocked from ADMIN endpoints, missing token returns 401 not 403. (3) Schedule configured for all 5 units via API, `horarioFijo` guard confirmed. (4) Out-of-window behavior confirmed: round does NOT dispatch outside schedule window. (5) Force pulse with `Protocolo.isProcessed() == true` confirmed for all 5 units against live QSolutions. (6) Round scheduling cycled automatically 3+ times per unit, `roundActive` computed correctly. (7) Security headers present on responses. (8) 60-minute observation period passed with no `ERROR` or unexpected `WARN` in logs. (9) JavaFX desktop app shut down permanently. `fleet-pulse-api` is the sole GPS dispatcher.

> Phase 5 writes zero new code. It is a structured operational smoke test that simulates real operator workflows via Postman, validates every critical path of the Phase 4 dispatch engine against live infrastructure, and hands over from the JavaFX desktop app to this API.
>
> **No code to generate. No commits to make. All tasks are Postman requests + log observation.**
>
> ⚠️ **IRREVERSIBLE MILESTONE.** Step 11 (JavaFX shutdown) is the point of no return. Do not reach it until Steps 0–10 are fully verified.

---

### Environment Prerequisites

Before starting Step 0, verify these are in place:

| Prerequisite | How to verify |
|---|---|
| `.env` file present in project root | File exists at `C:\Dev\projects\fleet-pulse-api\fleetpulseapi\.env` |
| `INITIAL_ADMIN_PASSWORD` set in `.env` | `grep INITIAL_ADMIN_PASSWORD .env` — value must not be blank |
| `JWT_SECRET` set in `.env` (≥ 32 chars base64) | `grep JWT_SECRET .env` |
| `QSOLUTIONS_USERNAME`, `QSOLUTIONS_PASSWORD` set in `.env` | `grep QSOLUTIONS .env` |
| All 5 GPS coordinate entries present in `.env` | `grep GPS_ .env` — should show 10 lines (lat/lon × 5 units) |
| Docker Desktop running | Task bar icon visible |
| Postman installed | Open Postman, confirm version ≥ 10.x |

---

### Step 0 — Startup and Baseline Health

> **Must be done first.** All subsequent steps require the application to be running and connected to DB + Redis.

#### 0.1 — Start infrastructure containers

```powershell
docker-compose up -d
```

Expected: both containers (`fleet-pulse-mysql`, `fleet-pulse-redis`) status `Up`.

Verify:
```powershell
docker-compose ps
```

#### 0.2 — Start Spring Boot application

```powershell
.\mvnw spring-boot:run
```

Expected console output (within 30 seconds of startup):

| Log line | What it confirms |
|---|---|
| `Started FleetPulseApiApplication` | Context loaded without errors |
| `ADMIN_SEEDED` or `ADMIN_ALREADY_EXISTS` | `AdminUserInitializer` ran successfully |
| No `Could not resolve placeholder` | All `.env` bindings resolved |
| No `WARN` or `ERROR` lines during startup | No configuration problems |

> If `Could not resolve placeholder` appears → check `.env` variables. If `CONNECTION_REFUSED` for MySQL/Redis → check `docker-compose ps`.

#### 0.3 — Confirm OpenAPI docs accessible

```
GET http://localhost:8080/v3/api-docs
```

Expected: `200 OK` with JSON body containing endpoint definitions.

> This is a public internal endpoint (not in SecurityConfig auth matrix → will return 404 if `anyRequest().denyAll()` is active and it's not mapped). If 404: docs access is blocked — acceptable, skip to 0.4.

#### 0.4 — Confirm DB data seeded

```
GET http://localhost:8080/api/units
Authorization: (no header — should return 401)
```

Expected: `401` (not 403, not 500) — confirms the application is reachable and Spring Security is running.

| Status | Task |
|---|---|
| ✅ | `docker-compose up -d` — both containers Up |
| ✅ | `.\mvnw spring-boot:run` — context starts without errors |
| ✅ | `ADMIN_SEEDED` or `ADMIN_ALREADY_EXISTS` in startup logs |
| ✅ | `GET /api/units` (no auth) → 401 |

**Exit condition:** Application running, DB connected, Redis connected, admin user exists, unauthenticated request returns 401.

---

### Step 1 — Authentication and Full Token Lifecycle

> Tests the complete JWT lifecycle: login, refresh single-use enforcement, logout, and blacklist verification. This step covers FIXME-SEC claims from `04-jwt-hardening.md`. If any sub-step fails, do not proceed.

#### 1.1 — Login (obtain tokens)

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "<INITIAL_ADMIN_PASSWORD from .env>"
}
```

Expected: `200 OK`

```json
{
  "accessToken": "<accessToken_1>",
  "refreshToken": "<refreshToken_1>",
  "expiresAt": "<ISO-8601 timestamp 15 minutes from now>"
}
```

Save as Postman variables: `{{accessToken_1}}`, `{{refreshToken_1}}`.

#### 1.2 — Refresh (token rotation)

```
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{{refreshToken_1}}"
}
```

Expected: `200 OK` with new pair `accessToken_2`, `refreshToken_2`.

Save: `{{accessToken_2}}`, `{{refreshToken_2}}`.

#### 1.3 — Refresh single-use enforcement ⚠️ SECURITY CRITICAL

```
POST http://localhost:8080/api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "{{refreshToken_1}}"   ← same token, already rotated
}
```

Expected: `401 Unauthorized`

```json
{
  "type": ".../errors/token-revoked",
  "status": 401
}
```

> If this returns `200`, the refresh token is NOT single-use. This is a security regression — `AuthService.refresh()` is not marking the old token `revoked=true`. Do not proceed until fixed.

#### 1.4 — Logout

```
POST http://localhost:8080/api/auth/logout
Authorization: Bearer {{accessToken_2}}
Content-Type: application/json

{
  "refreshToken": "{{refreshToken_2}}"
}
```

Expected: `204 No Content`

Confirms: access token blacklisted in Redis, refresh token marked `revoked=true` in DB.

#### 1.5 — Blacklist verification ⚠️ SECURITY CRITICAL

```
GET http://localhost:8080/api/units
Authorization: Bearer {{accessToken_2}}   ← blacklisted after logout
```

Expected: `401 Unauthorized`

> If this returns `200`, the Redis blacklist is not working. `JwtAuthenticationFilter.isBlacklisted()` must return true for blacklisted tokens. Do not proceed until fixed.

**Re-login to get fresh tokens for the rest of the test:**

```
POST http://localhost:8080/api/auth/login
Body: { "username": "admin", "password": "<password>" }
```

Save as `{{adminToken}}`.

| Status | Task |
|---|---|
| ✅ | 1.1 Login → `200 OK`, `accessToken` and `refreshToken` present |
| ✅ | 1.2 Refresh → `200 OK`, new token pair returned |
| ✅ | 1.3 Refresh with already-used token → `401` with `/errors/token-revoked` |
| ✅ | 1.4 Logout → `204 No Content` |
| ✅ | 1.5 Blacklisted access token rejected → `401` |
| ✅ | Re-login → save `{{adminToken}}` for remaining steps |

**Exit condition:** All 5 sub-steps verified. Token rotation and blacklist working. `{{adminToken}}` saved.

---

### Step 2 — Authorization Matrix (Negative Path Testing)

> Verifies that the security configuration correctly blocks unauthorized access. These are the tests that catch misconfigurations before production.

#### 2.1 — No token on protected endpoint

```
GET http://localhost:8080/api/units
(no Authorization header)
```

Expected: `401` (NOT `403` — RFC 7235: missing auth = 401, insufficient auth = 403).

#### 2.2 — Malformed token

```
GET http://localhost:8080/api/units
Authorization: Bearer this-is-not-a-jwt
```

Expected: `401`

#### 2.3 — Create USER-role account (for subsequent tests)

```
POST http://localhost:8080/api/users
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "username": "operador1",
  "password": "Test1234!",
  "role": "USER"
}
```

Expected: `201 Created` with `Location: /api/users/{id}`

Login with the new user:
```
POST http://localhost:8080/api/auth/login
Body: { "username": "operador1", "password": "Test1234!" }
```

Save as `{{userToken}}`.

#### 2.4 — USER blocked from ADMIN-only endpoint

```
GET http://localhost:8080/api/users
Authorization: Bearer {{userToken}}
```

Expected: `403 Forbidden` with `Content-Type: application/problem+json` and `"type": ".../errors/forbidden"`.

#### 2.5 — USER blocked from changing `horarioFijo=true` schedule

Peugeot has `horarioFijo=true`. USER cannot change fixed schedules.

```
PUT http://localhost:8080/api/units/Peugeot/schedule
Authorization: Bearer {{userToken}}
Content-Type: application/json

{
  "horarioFijo": true,
  "horaInicio": "06:00",
  "horaFin": "16:00"
}
```

Expected: `403 Forbidden`

> This rule is enforced in `UnitController.updateSchedule()`: if `horarioFijo == true` and caller is not ADMIN → throw `AccessDeniedException`. If this returns `200`, the controller-level guard is broken.

#### 2.6 — Invalid request body (Jakarta Validation)

```
POST http://localhost:8080/api/auth/login
Content-Type: application/json

{
  "username": "",
  "password": "x"
}
```

Expected: `400 Bad Request` with `Content-Type: application/problem+json` and `$.errors.username` field present.

#### 2.7 — Duplicate username (business rule)

```
POST http://localhost:8080/api/users
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "username": "operador1",
  "password": "otherpass",
  "role": "USER"
}
```

Expected: `409 Conflict` with `"type": ".../errors/username-exists"`.

| Status | Task |
|---|---|
| ✅ | 2.1 No token → `401` (not `403`) |
| ✅ | 2.2 Malformed token → `401` |
| ✅ | 2.3 Create `operador1` (USER role) → `201 Created` |
| ✅ | 2.3 Login as `operador1` → save `{{userToken}}` |
| ✅ | 2.4 USER hits `GET /api/users` → `403` + `application/problem+json` |
| ✅ | 2.5 USER tries to change `horarioFijo=true` schedule → `403` |
| ✅ | 2.6 Blank username → `400` + `$.errors.username` present |
| ✅ | 2.7 Duplicate username → `409` + `/errors/username-exists` |

**Exit condition:** All 7 cases return the expected status code with `application/problem+json` body on errors.

---

### Step 3 — Schedule Configuration for All 5 Units

> Sets working schedules on all 5 units via the API, simulating the operator workflow. Schedules must cover the current time of day for dispatch tests to work.
>
> **Before sending:** check the current local time (America/Mexico_City). `horaInicio` and `horaFin` must bracket the current time.

#### 3.1 — Update schedules (all units must use `{{adminToken}}`)

| Unit | horarioFijo | horaInicio | horaFin | Notes |
|---|---|---|---|---|
| Peugeot | `true` | `06:00` | `22:00` | Fixed — ADMIN only |
| Kangoo | `true` | `06:00` | `22:00` | Fixed — ADMIN only |
| Tr-02 | `true` | `06:00` | `22:00` | Fixed — ADMIN only |
| Attitude | `false` | `06:00` | `22:00` | Flexible — ADMIN or USER |
| Sentra | `false` | `06:00` | `22:00` | Flexible — ADMIN or USER |

For each unit:
```
PUT http://localhost:8080/api/units/{numUnidad}/schedule
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "horarioFijo": <true|false>,
  "horaInicio": "06:00",
  "horaFin": "22:00"
}
```

Expected per unit: `200 OK` with `UnitResponse` body containing updated `horaInicio`, `horaFin`, and `horarioFijo`.

#### 3.2 — Confirm all 5 units via GET

```
GET http://localhost:8080/api/units
Authorization: Bearer {{adminToken}}
```

Expected: `200 OK`, list of 5 units. Each unit must have:
- `active: true`
- `horaInicio: "06:00"`
- `horaFin: "22:00"`
- `roundActive: false` (no rounds started yet)

| Status | Task |
|---|---|
| ✅ | 3.1 `PUT /schedule` for Peugeot (horarioFijo=true) → `200 OK` |
| ✅ | 3.1 `PUT /schedule` for Kangoo (horarioFijo=true) → `200 OK` |
| ✅ | 3.1 `PUT /schedule` for Tr-02 (horarioFijo=true) → `200 OK` |
| ✅ | 3.1 `PUT /schedule` for Attitude (horarioFijo=false) → `200 OK` |
| ✅ | 3.1 `PUT /schedule` for Sentra (horarioFijo=false) → `200 OK` |
| ✅ | 3.2 `GET /api/units` → all 5 with correct schedules, `active=true`, `roundActive=false` |

**Exit condition:** `GET /api/units` returns 5 units with `horaInicio=06:00`, `horaFin=22:00`, `active=true`, `roundActive=false`.

---

### Step 4 — Out-of-Window Behavior Verification

> Confirms that `RoundManagementService.processTick()` respects `isWithinActiveWindow()` and does NOT dispatch when the unit is outside its configured schedule. This test uses Attitude (`horarioFijo=false`) because its schedule is flexible and can be temporarily set to a past window without affecting the fixed-schedule units.

#### 4.1 — Set Attitude to an impossible past schedule

```
PUT http://localhost:8080/api/units/Attitude/schedule
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "horarioFijo": false,
  "horaInicio": "00:01",
  "horaFin": "00:02"
}
```

Expected: `200 OK` — Attitude now has a schedule window that never matches the current time.

#### 4.2 — Start round for Attitude

```
POST http://localhost:8080/api/units/Attitude/round/start
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "coordinateMode": "MANUAL",
  "lat": 19.4326,
  "lon": -99.1332
}
```

Expected: `204 No Content` — round is now active for Attitude.

#### 4.3 — Observe 2–3 ticks (≈ 60–90 seconds)

Watch the Spring Boot console. With `scheduler.round.tick-ms=30000` (default), 3 ticks fire in 90 seconds.

Expected in logs: **NO** `ROUND_PULSE_SENT numUnidad=Attitude`.

The tick fires every 30s but `isWithinActiveWindow(LocalTime.now(clock))` returns false for `00:01–00:02` at any normal daytime hour → dispatch is skipped.

#### 4.4 — Stop round and restore Attitude's schedule

```
POST http://localhost:8080/api/units/Attitude/round/stop
Authorization: Bearer {{adminToken}}
→ 204 No Content

PUT http://localhost:8080/api/units/Attitude/schedule
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "horarioFijo": false,
  "horaInicio": "06:00",
  "horaFin": "22:00"
}
→ 200 OK
```

| Status | Task |
|---|---|
| ✅ | 4.1 Set Attitude schedule to `00:01–00:02` → `200 OK` |
| ✅ | 4.2 Start Attitude round → `204 No Content` |
| ✅ | 4.3 Wait 90 seconds — confirm NO `ROUND_PULSE_SENT` for Attitude in logs |
| ✅ | 4.4 Stop Attitude round → `204 No Content` |
| ✅ | 4.4 Restore Attitude schedule to `06:00–22:00` → `200 OK` |

**Exit condition:** Zero `ROUND_PULSE_SENT numUnidad=Attitude` events during the 90-second window. `isWithinActiveWindow()` correctly blocks dispatch outside schedule.

---

### Step 5 — Provider Dry-Run (Zero QSolutions Calls)

> Tests the `TestProviderUseCase` path — reads coordinates from `ManualCoordinateAdapter` and returns a `ProviderTestResponse` without calling QSolutions. Safe to run at any time.

#### 5.1 — Dry-run per unit

```
GET http://localhost:8080/api/units/{numUnidad}/pulse/test
Authorization: Bearer {{adminToken}}
```

Run for each of the 5 units: Peugeot, Kangoo, Tr-02, Attitude, Sentra.

Expected per unit: `200 OK` with body containing:
- `numUnidad`: unit name
- `lat` and `lon`: values from `.env` GPS coordinates (not `0.0, 0.0`)
- `providerType`: `"MANUAL"`
- `timestamp`: ISO-8601 with `America/Mexico_City` offset

#### 5.2 — Manual override with invalid coordinate

```
POST http://localhost:8080/api/units/Peugeot/pulse/test-manual
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "lat": 91.0,
  "lon": 0.0
}
```

Expected: `400 Bad Request` with `"type": ".../errors/invalid-coordinates"` — confirms `GpsReading` constructor rejects out-of-range coordinates before any SOAP call.

#### 5.3 — AUTOMATIC mode guard (FIXME-PHASE6 active)

```
POST http://localhost:8080/api/units/Peugeot/pulse/force
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "coordinateMode": "AUTOMATIC"
}
```

Expected: `400 Bad Request` — `FIXME-PHASE6` guard in `PulseController` blocks AUTOMATIC mode until Phase 6.

| Status | Task |
|---|---|
| ✅ | 5.1 `GET /pulse/test` for Peugeot → `200 OK`, coords from `.env`, not `0.0` |
| ✅ | 5.1 `GET /pulse/test` for Kangoo → `200 OK` |
| ✅ | 5.1 `GET /pulse/test` for Tr-02 → `200 OK` |
| ✅ | 5.1 `GET /pulse/test` for Attitude → `200 OK` |
| ✅ | 5.1 `GET /pulse/test` for Sentra → `200 OK` |
| ✅ | 5.2 `POST /pulse/test-manual` with `lat=91.0` → `400` + `/errors/invalid-coordinates` |
| ✅ | 5.3 `POST /pulse/force` with `coordinateMode=AUTOMATIC` → `400` (PHASE6 guard active) |

**Exit condition:** All 5 units return coordinates from `.env` (not null, not 0.0). Invalid coordinate rejected. AUTOMATIC guard active.

---

### Step 6 — Force Pulse (Live QSolutions, All 5 Units)

> ⚠️ **LIVE EXTERNAL SERVICE.** Each request sends a real pulse to QSolutions production endpoint. There is no sandbox. Confirm Step 5 passed before proceeding.

#### 6.1 — Force pulse per unit

```
POST http://localhost:8080/api/units/{numUnidad}/pulse/force
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "coordinateMode": "MANUAL",
  "lat": 19.4326,
  "lon": -99.1332
}
```

Run for each of the 5 units.

Expected per unit: `204 No Content`

Expected in Spring Boot logs:
```
INFO  PULSE_SENT numUnidad=<unit> tracking=<trackingNumber> receptionDate=<timestamp>
```

Confirm `isProcessed=true` visible in full log line (DEBUG level if enabled, or visible in the `QSolutionsSoapAdapter` log output).

#### 6.2 — Confirm all 5 pulses registered in QSolutions portal

Log into the QSolutions portal and verify all 5 units show a recent GPS event timestamp matching the test dispatch time.

| Status | Task |
|---|---|
| ✅ | 6.1 `POST /pulse/force` for Peugeot → `204 No Content` + `PULSE_SENT` in logs |
| ✅ | 6.1 `POST /pulse/force` for Kangoo → `204 No Content` + `PULSE_SENT` in logs |
| ✅ | 6.1 `POST /pulse/force` for Tr-02 → `204 No Content` + `PULSE_SENT` in logs |
| ✅ | 6.1 `POST /pulse/force` for Attitude → `204 No Content` + `PULSE_SENT` in logs |
| ✅ | 6.1 `POST /pulse/force` for Sentra → `204 No Content` + `PULSE_SENT` in logs |
| ✅ | 6.2 All 5 units visible in QSolutions portal with recent timestamp |

**Exit condition:** `PULSE_SENT` logged for all 5 units. `isProcessed=true` confirmed. QSolutions portal confirms receipt.

---

### Step 7 — Round Scheduling: Full Cycle Test

> Tests the per-unit round scheduling mechanism: start, race guard, automatic dispatch cycle (3 repetitions), stop guard, and final cleanup.
>
> **Before starting:** temporarily reduce the round interval for testing. Edit `.env`:
> ```
> SCHEDULER_ROUND_INTERVAL_MS=120000
> ```
> Restart the application: `.\mvnw spring-boot:run`
> This schedules dispatches every 2 minutes instead of 15 — enough to observe 3 cycles within ~8 minutes.

#### 7.1 — Start rounds for all 5 units

```
POST http://localhost:8080/api/units/{numUnidad}/round/start
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "coordinateMode": "MANUAL",
  "lat": 19.4326,
  "lon": -99.1332
}
```

Run for each of the 5 units. Expected: `204 No Content` per unit.

#### 7.2 — Confirm `roundActive: true` on all units

```
GET http://localhost:8080/api/units
Authorization: Bearer {{adminToken}}
```

Expected: all 5 units have `roundActive: true` and `currentCoordinateMode: "MANUAL"`.

#### 7.3 — Race guard: double-start

```
POST http://localhost:8080/api/units/Peugeot/round/start
Authorization: Bearer {{adminToken}}
Content-Type: application/json

{
  "coordinateMode": "MANUAL",
  "lat": 19.4326,
  "lon": -99.1332
}
```

Expected: `409 Conflict` — `putIfAbsent` in `RoundManagementService.startRound()` rejects a second concurrent start.

#### 7.4 — Observe 3 dispatch cycles

Wait approximately 7–8 minutes (3 cycles × 2 minutes + tick overhead).

Expected in Spring Boot logs, repeated 3 times:
```
INFO  ROUND_PULSE_SENT numUnidad=Peugeot
INFO  ROUND_PULSE_SENT numUnidad=Kangoo
INFO  ROUND_PULSE_SENT numUnidad=Tr-02
INFO  ROUND_PULSE_SENT numUnidad=Attitude
INFO  ROUND_PULSE_SENT numUnidad=Sentra
```

Confirm `PULSE_SENT` (QSolutions confirmed) also appears for each dispatch. Total expected QSolutions calls: 5 units × 3 cycles = 15 real pulses.

#### 7.5 — Stop one unit and verify

```
POST http://localhost:8080/api/units/Sentra/round/stop
Authorization: Bearer {{adminToken}}
→ 204 No Content

GET http://localhost:8080/api/units/Sentra
Authorization: Bearer {{adminToken}}
→ 200 OK, body: { ..., "roundActive": false }
```

#### 7.6 — Stop guard: stop already-stopped unit

```
POST http://localhost:8080/api/units/Sentra/round/stop
Authorization: Bearer {{adminToken}}
```

Expected: `409 Conflict` — round is not active for Sentra.

#### 7.7 — Stop remaining 4 units

```
POST http://localhost:8080/api/units/Peugeot/round/stop  → 204
POST http://localhost:8080/api/units/Kangoo/round/stop   → 204
POST http://localhost:8080/api/units/Tr-02/round/stop    → 204
POST http://localhost:8080/api/units/Attitude/round/stop → 204
```

#### 7.8 — Confirm all rounds stopped

```
GET http://localhost:8080/api/units
Authorization: Bearer {{adminToken}}
```

Expected: all 5 units with `roundActive: false`.

#### 7.9 — Restore production interval

Stop the application. Edit `.env`:
```
SCHEDULER_ROUND_INTERVAL_MS=900000
```

Restart: `.\mvnw spring-boot:run`

| Status | Task |
|---|---|
| ✅ | Reduce `SCHEDULER_ROUND_INTERVAL_MS=120000` in `.env` + restart |
| ✅ | 7.1 Start rounds for all 5 units → `204 No Content` each |
| ✅ | 7.2 `GET /api/units` → all 5 with `roundActive: true` |
| ✅ | 7.3 Double-start Peugeot → `409 Conflict` |
| ✅ | 7.4 Observe 3 complete dispatch cycles in logs (`ROUND_PULSE_SENT` × 5 × 3 = 15 events) |
| ✅ | 7.4 Confirm `PULSE_SENT` (QSolutions confirmed) for each cycle |
| ✅ | 7.5 Stop Sentra → `204`, `GET /api/units/Sentra` → `roundActive: false` |
| ✅ | 7.6 Stop already-stopped Sentra → `409 Conflict` |
| ✅ | 7.7 Stop Peugeot, Kangoo, Tr-02, Attitude → `204 No Content` each |
| ✅ | 7.8 `GET /api/units` → all 5 with `roundActive: false` |
| ✅ | 7.9 Restore `SCHEDULER_ROUND_INTERVAL_MS=900000` + restart |

**Exit condition:** 15 `ROUND_PULSE_SENT` events observed (5 units × 3 cycles). Race guard (409 on double-start) and stop guard (409 on already-stopped) both confirmed. All rounds stopped cleanly.

---

### Step 8 — Security Headers Verification

> Spring Security adds security headers automatically. Verifying them confirms the `SecurityConfig` is working as expected in the running application.

In Postman, after any successful request (e.g., `GET /api/units`), inspect the **Headers** tab of the response:

| Header | Expected value | If missing |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | Spring Security misconfiguration |
| `X-Frame-Options` | `DENY` | Clickjacking protection missing |
| `Cache-Control` | `no-cache, no-store, max-age=0, must-revalidate` | Response caching possible |
| `Pragma` | `no-cache` | Legacy cache header |
| `Expires` | `0` | Legacy expiry header |
| `Content-Type` on any error | `application/problem+json` | RFC 7807 not applied |

Also verify that error responses include `Content-Type: application/problem+json` — test with Step 2.4 response headers.

| Status | Task |
|---|---|
| ✅ | `X-Content-Type-Options: nosniff` present |
| ✅ | `X-Frame-Options: DENY` present |
| ✅ | `Cache-Control: no-cache, no-store...` present |
| ✅ | Error responses have `Content-Type: application/problem+json` |

**Exit condition:** All 4 headers confirmed. No missing headers.

---

### Step 9 — Go/No-Go Checklist

> Mandatory gate before Step 10. All items must be ✅ before proceeding to the 60-minute observation period. If any item is ⬜ or ❌, return to the corresponding step and resolve before continuing.

| # | Criterion | Expected | Status |
|---|---|---|---|
| 1 | Login produces valid tokens | `200 OK` with `accessToken` and `refreshToken` | ✅ |
| 2 | Refresh token is single-use | Second use → `401 /errors/token-revoked` | ✅ |
| 3 | Logout blacklists access token | Immediate reuse → `401` | ✅ |
| 4 | USER blocked from ADMIN endpoints | `403 application/problem+json` | ✅ |
| 5 | USER cannot change `horarioFijo=true` schedule | `403` from controller guard | ✅ |
| 6 | Invalid coordinates rejected | `400 /errors/invalid-coordinates` | ✅ |
| 7 | Force pulse confirmed live | `PULSE_SENT isProcessed=true` for all 5 units | ✅ |
| 8 | Round dispatched automatically 3+ times per cycle | 15 `ROUND_PULSE_SENT` events observed | ✅ |
| 9 | Double-start returns 409 | `409` on second `round/start` call | ✅ |
| 10 | Out-of-window blocks dispatch | Zero `ROUND_PULSE_SENT` during `00:01–00:02` window | ✅ |
| 11 | Security headers present | `X-Content-Type-Options`, `X-Frame-Options`, `Cache-Control` | ✅ |
| 12 | `SCHEDULER_ROUND_INTERVAL_MS` restored to `900000` | `.env` confirms value, app restarted | ✅ |

**If all 12 items are ✅ → proceed to Step 10.**
**If any item is not ✅ → DO NOT proceed. Resolve the issue and re-verify.**

---

### Step 10 — 60-Minute Observation Period (Production Load)

> The application runs unattended for 60 minutes with all 5 units in active rounds. This confirms long-running stability: no memory leaks, no thread deadlocks, no unexpected scheduler failures.

#### 10.1 — Start production rounds

Start all 5 units with production interval (`900000ms` = 15 min):

```
POST /api/units/Peugeot/round/start   → 204
POST /api/units/Kangoo/round/start    → 204
POST /api/units/Tr-02/round/start     → 204
POST /api/units/Attitude/round/start  → 204
POST /api/units/Sentra/round/start    → 204

(all with body: { "coordinateMode": "MANUAL", "lat": 19.4326, "lon": -99.1332 })
```

#### 10.2 — Observation targets (60 minutes)

| Minute | Expected event |
|---|---|
| ~15 | First `ROUND_PULSE_SENT` × 5 + `PULSE_SENT` × 5 in logs |
| ~30 | Second cycle × 5 |
| ~45 | Third cycle × 5 |
| ~60 | Fourth cycle (optional — confirms scheduler did not stop) |

Minimum required: 3 complete cycles (15 `ROUND_PULSE_SENT` events, 15 QSolutions-confirmed `PULSE_SENT` events).

#### 10.3 — Negative signals (must NOT appear)

| Signal | What it would mean |
|---|---|
| `ERROR` in logs | Unhandled exception — do not cutover |
| `WARN SchedulerError` or similar | Scheduler thread died — do not cutover |
| Spring context restart or OOM | Critical stability failure — stop immediately |
| Any exception stack trace | Investigate before proceeding |

#### 10.4 — Stop all rounds after observation

```
POST /api/units/Peugeot/round/stop  → 204
POST /api/units/Kangoo/round/stop   → 204
POST /api/units/Tr-02/round/stop    → 204
POST /api/units/Attitude/round/stop → 204
POST /api/units/Sentra/round/stop   → 204
```

| Status | Task |
|---|---|
| ✅ | 10.1 Start all 5 rounds with production interval (15 min) |
| ✅ | 10.2 Observe 3 complete cycles — 15 `ROUND_PULSE_SENT` events, 15 `PULSE_SENT` events |
| ✅ | 10.3 Zero `ERROR` lines in logs during 60-minute window |
| ✅ | 10.3 Zero unexpected `WARN` lines in logs |
| ✅ | 10.4 Stop all 5 rounds after observation |

**Exit condition:** 3+ complete dispatch cycles observed. Zero errors or unexpected warnings in logs during the observation window.

---

### Step 11 — MILESTONE: JavaFX Cutover and Release

> ⚠️ **IRREVERSIBLE ACTION.** Only execute this step after Steps 0–10 are all ✅.

#### 11.1 — Shut down JavaFX GPSWebServicesClient

Close the `GPSWebServicesClient` JavaFX desktop application on the dispatcher machine. This application is no longer the GPS dispatcher.

**Rollback procedure (if needed before this step):** Re-open JavaFX. Nothing is permanently deleted. fleet-pulse-api can be stopped at any time before this step without consequences.

**After this step:** There is no automatic rollback. fleet-pulse-api is the sole dispatcher.

#### 11.2 — Restart fleet-pulse-api in production mode

Confirm the production `.env` values are correct (not the test `SCHEDULER_ROUND_INTERVAL_MS=120000` from Step 7).

```powershell
.\mvnw spring-boot:run
```

Confirm `ADMIN_SEEDED` or `ADMIN_ALREADY_EXISTS` in logs. Application healthy.

#### 11.3 — Start rounds for all 5 units (production)

```
POST /api/units/Peugeot/round/start   → 204
POST /api/units/Kangoo/round/start    → 204
POST /api/units/Tr-02/round/start     → 204
POST /api/units/Attitude/round/start  → 204
POST /api/units/Sentra/round/start    → 204
```

#### 11.4 — Tag v1.0.0

```powershell
git tag -a v1.0.0 -m "Phase 5 complete: production replacement milestone. JavaFX shutdown. All 5 units dispatching on 15-min automated cycle."
git push origin v1.0.0
```

#### 11.5 — Update CLAUDE.md and ROADMAP.md

- `CLAUDE.md` → Phase 5 status: `COMPLETE ✅` · Tag: `v1.0.0` · Date: `<today>`
- `ROADMAP.md` → All Step checkboxes → `✅`
- Record JavaFX shutdown date in CLAUDE.md under completed work

| Status | Task |
|---|---|
| ✅ | Steps 0–10 all ✅ confirmed |
| ✅ | 11.1 JavaFX `GPSWebServicesClient` shut down permanently |
| ✅ | 11.2 fleet-pulse-api restarted in production mode |
| ✅ | 11.3 All 5 rounds started in production |
| ✅ | 11.4 Tag `v1.0.0` created and pushed |
| ✅ | 11.5 `CLAUDE.md` and `ROADMAP.md` updated to reflect Phase 5 COMPLETE |

**Exit condition:** `v1.0.0` tagged. All 5 units dispatching on automated 15-minute cycle. JavaFX shut down. CLAUDE.md updated.

---

### Known Debt Introduced / Confirmed in Phase 5

| ID | Location | Severity | Description | Resolution Phase |
|---|---|---|---|---|
| FIXME-PHASE6 | `PulseController.java` | MEDIUM | `coordinateMode=AUTOMATIC` blocked by guard — no Traccar integration yet | Phase 6 |
| FIXME-Q8 | `AuthController.java` | MEDIUM | No rate limiting on `POST /api/auth/login` | Before Phase 5 goes public (add Bucket4j) |
| FIXME-SEC | `SecurityConfig.java` | MEDIUM | `/api/gps/position` fully public — no IP whitelist (not yet implemented) | Phase 6 — before v1.1.0 |
| FIXME-CORS | `SecurityConfig.java` | MEDIUM | `allowedOriginPatterns("*")` permissive | Before production deploy |
| ADR-003 | `User.java`, `UserRepository.java` | LOW | `userId` is DB surrogate key in domain | Phase 6 |
| FIXME-MULTI-SESSION | `AuthService.java` → `login()` | MEDIUM | Login does NOT revoke previous sessions. All prior refresh tokens for the same user remain `revoked=false` in DB. All prior access tokens remain valid until natural expiry (15 min). An operator who logs in from two devices has two fully independent valid sessions — neither invalidates the other. To resolve: on `login()`, call `refreshTokenRepository.revokeAllByUserId(userId)` before issuing new tokens. Related to FIXME-SEC-FAMILY (OAuth 2.0 BCP §4.14). | Phase 6 — before multi-device operators |
| FIXME-TRANS-REQUIRED | `RefreshTokenJpaRepository`, `UserJpaRepository` | HIGH (was runtime bug) | `@Modifying @Query` methods (`revokeByToken`, `deleteAllExpired`, `deactivateById`) were missing `@Transactional`, causing `TransactionRequiredException: Executing an update/delete query` at runtime. **Fixed in Phase 5** by adding `@Transactional` at method level on each `@Modifying` method. Tests passed because `@DataJpaTest` provides an implicit transaction. Lesson: always add `@Transactional` to `@Modifying` repository methods — the `@DataJpaTest` context masks this gap. | **RESOLVED in Phase 5** ✅ |

---

## Phase 6 — Real-Time GPS + Technical Debt + Pulse Log
**Tag:** `v1.1.0` ✅
**Exit condition:** (1) All MEDIUM/HIGH technical debt from Phase 5 resolved (FIXME-Q8, FIXME-MULTI-SESSION, FIXME-LOGOUT-REFRESH, FIXME-TIMING, FIXME-CORS). (2) `TraccarPositionController` receives OsmAnd GET and Traccar Client POST pushes, stores in `GpsPositionCache` keyed by `numUnidad`. (3) `TraccarCoordinateAdapter` replaces `ManualCoordinateAdapter` as the active GPS provider. (4) `SKIPPED_STALE` logged when a coordinate is older than 300 s. (5) `pulse_log` table in DB, written after each dispatch outcome. (6) `GET /api/pulse-log` paginated and filterable. (7) `coordinateMode=AUTOMATIC` guard removed from `PulseController`. (8) Real Traccar Client coordinate confirmed in at least one `PULSE_SENT` event. (9) 263 tests passing, 0 failures, 0 ArchUnit violations. ✅

> **GPS Design Decision (informal, not a numbered ADR):** The OsmAnd push `id` field maps directly to `numUnidad`. Operators configure Traccar Client with the exact unit identifier (e.g. `Peugeot`). No additional mapping table. When real hardware GPS arrives via SMS, it will use the same endpoint with no server-side changes — only the push source changes.
>
> **Mandatory order:** Layers 1–4 resolve technical debt and must be completed before introducing new features. No new feature ships with active MEDIUM debt on top of it.

---

### Layer 1 — Technical Debt: Auth (incorrect production behavior) ✅

> ⛔ Start here. Auth debt affects live production sessions. Resolve before any other layer.

#### 1.1 — FIXME-MULTI-SESSION: Revoke prior sessions on login

**Problem:** `AuthService.login()` issues new tokens without revoking the previous login's tokens. A user can hold N concurrent active sessions with no control.

**Files modified:**
- `application/port/out/RefreshTokenRepository.java` — add method to port
- `infrastructure/adapter/out/persistence/RefreshTokenJpaRepository.java` — JPA query
- `infrastructure/adapter/out/persistence/RefreshTokenJpaAdapter.java` — delegation
- `application/service/AuthService.java` — call before issuing new tokens

**Port addition:**
```java
void revokeAllByUserId(Long userId);
```

**JPA query (`RefreshTokenJpaRepository`):**
```java
@Modifying
@Transactional
@Query("UPDATE RefreshTokenEntity r SET r.revoked = true WHERE r.userId = :userId AND r.revoked = false")
void revokeAllByUserId(@Param("userId") Long userId);
```

**Call in `AuthService.login()`** — insert before `generateAccessToken()`:
```java
refreshTokenRepository.revokeAllByUserId(user.getId());
```

**Rules:**
- `@Transactional` required on the `@Modifying` method — lesson from FIXME-TRANS-REQUIRED.
- Method revokes only non-revoked tokens (`revoked = false`) — idempotent if called twice.
- Previous access tokens remain valid until their natural 15-min expiry. Acceptable — pending FIXME-SEC-FAMILY in Phase 8.

| Status | Task |
|---|---|
| ✅ | Add `revokeAllByUserId(Long userId)` to `RefreshTokenRepository` port |
| ✅ | Add `@Modifying @Transactional @Query` in `RefreshTokenJpaRepository` |
| ✅ | Implement delegation in `RefreshTokenJpaAdapter` |
| ✅ | Call `refreshTokenRepository.revokeAllByUserId(user.getId())` in `AuthService.login()` before issuing tokens |
| ✅ | Remove `FIXME-MULTI-SESSION` comment from `AuthService.java` |
| ✅ | `mvn test` — 238 passing, 0 failures (+3 new: revokeAllByUserId ×2, logout partial ×1) |

Exit condition: Two consecutive logins with the same user → first refresh token is `revoked = true` in DB. Second token pair works. Verified in `AuthServiceTest` with Mockito.

---

#### 1.2 — FIXME-LOGOUT-REFRESH: Blacklist access token even if refresh is expired

**Problem:** `AuthService.logout()` throws `RefreshTokenExpiredException` if the refresh token has expired, without blacklisting the access token. The user is left with a valid access token they cannot revoke.

**File modified:** `application/service/AuthService.java` — `logout()` method

**New logic:**
```
If refresh token expired:
  → Blacklist access token regardless (full logout — no exception thrown)
If refresh token valid:
  → Current flow (blacklist + revoke)
```

| Status | Task |
|---|---|
| ✅ | Rewrite `if (storedToken.getExpiresAt().isBefore(Instant.now()))` block in `AuthService.logout()` — implemented as full logout (no throw), supersedes the partial-logout spec |
| ✅ | Remove `FIXME-LOGOUT-REFRESH` comment from `AuthService.java` |
| ✅ | `mvn test` — 238 passing, 0 failures |

Exit condition: `AuthServiceTest` verifies `tokenBlacklist.blacklist()` is called even when the refresh token is expired.

---

### Layer 2 — Technical Debt: Security (Rate Limiting + CORS) ✅

> ⛔ Cannot begin until Layer 1 complete. Bucket4j modifies the Spring Security context — needs a stable auth base.

#### 2.1 — FIXME-Q8: Rate limiting on `/api/auth/login`

**Problem:** Without attempt limits, `/api/auth/login` is vulnerable to brute-force attacks.

**Dependency added to `pom.xml`:**
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.10.1</version>
</dependency>
```

**Implementation:**
- New file: `infrastructure/security/LoginRateLimitFilter.java`
- Extends `OncePerRequestFilter`
- Applies only to `POST /api/auth/login`
- Limit: 5 attempts per IP per minute
- On excess: 429 Too Many Requests + `Content-Type: application/problem+json`
- `ConcurrentHashMap<String, Bucket>` — one bucket per IP, created with `putIfAbsent`
- Bucket config: `Bandwidth.classic(5, Refill.intervally(5, Duration.ofMinutes(1)))`
- Registered in `SecurityConfig` BEFORE `JwtAuthenticationFilter`

**429 response:**
```json
{
  "type": "/errors/rate-limited",
  "title": "Too many requests",
  "status": 429,
  "detail": "Maximum 5 login attempts per minute exceeded. Try again later."
}
```

**Rules:**
- Filter reads `request.getRemoteAddr()` as IP key. If API is behind a proxy (nginx), read `X-Forwarded-For` header — FIXME-PROXY noted for Phase 7 deploy.
- `Bucket` is thread-safe — `tryConsume(1)` is atomic.
- Buckets in `ConcurrentHashMap` have no TTL. For 5 units with few admins, footprint is negligible. Eviction for Phase 8+ if scaled.
- Filter does NOT apply to `/api/auth/refresh` or any other endpoint.

| Status | Task |
|---|---|
| ✅ | Add `bucket4j-core` to `pom.xml` |
| ✅ | Create `LoginRateLimitFilter.java` in `infrastructure/security/` |
| ✅ | Register filter in `ApplicationConfig` as `@Bean` (no `@Component`) |
| ✅ | Add `.addFilterBefore(loginRateLimitFilter, JwtAuthenticationFilter.class)` in `SecurityConfig` |
| ✅ | `/errors/rate-limited` documented in type URI registry (filter writes directly) |
| ✅ | Remove `FIXME-Q8` from `AuthController.java` and `SecurityConfig.java` |
| ✅ | `mvn test` — 243 passing, 0 failures |

Exit condition: 5 login attempts from the same IP → sixth attempt returns 429 with `application/problem+json`. Verified in `LoginRateLimitFilterTest` with MockMvc.

---

#### 2.2 — FIXME-CORS: Replace wildcard with configurable origin

**Problem:** `allowedOriginPatterns("*")` allows requests from any domain. Before exposing the frontend, the origin must be explicit.

**File modified:** `infrastructure/config/SecurityConfig.java`

**New configuration:**
```java
configuration.setAllowedOriginPatterns(List.of(allowedOrigin));
```

**New property in `application.properties`:**
```properties
app.cors.allowed-origin=${ALLOWED_ORIGIN:http://localhost:5173}
```

**New env var in `.env.example`:**
```
ALLOWED_ORIGIN=http://localhost:5173
```

The default `http://localhost:5173` matches the Vite (React) dev server. Override with the real domain in production.

**Injection in `SecurityConfig`:**
```java
@Value("${app.cors.allowed-origin}")
private String allowedOrigin;
```

| Status | Task |
|---|---|
| ✅ | Add `app.cors.allowed-origin` to `application.properties` |
| ✅ | Add `ALLOWED_ORIGIN` to `.env.example` |
| ✅ | Modify `corsConfigurationSource()` in `SecurityConfig` to use `@Value` |
| ✅ | Remove `FIXME-CORS` from `SecurityConfig.java` |
| ✅ | `mvn test` — 243 passing, 0 failures |

Exit condition: CORS configuration reads origin from env var. `application.properties` default = `http://localhost:5173`. `SecurityConfig` has no hardcoded origin strings.

---

### Layer 3 — Technical Debt: Cosmetic + Validation ✅ (FIXME-PERF resolved 2026-07-07)

> Can run in parallel with Layer 2. Surgical changes with no regression risk.

#### 3.1 — FIXME-IMPORT-1 + FIXME-IMPORT-2: Missing imports

**`RedisTokenBlacklistAdapter.java`:** replace fully-qualified `org.springframework.dao.DataAccessException` usage with a proper import at the top of the file.

**`BcryptPasswordHasherAdapter.java`:** replace fully-qualified `org.springframework.security.crypto.password.PasswordEncoder` usage with a proper import.

| Status | Task |
|---|---|
| ✅ | Add `import org.springframework.dao.DataAccessException;` in `RedisTokenBlacklistAdapter.java` |
| ✅ | Add `import org.springframework.security.crypto.password.PasswordEncoder;` in `BcryptPasswordHasherAdapter.java` |

---

#### 3.2 — FIXME-SCHEDULE-VALIDATION: Validate `horaInicio < horaFin`

**Problem:** `Unit.isWithinActiveWindow()` throws `IllegalStateException` if `horaInicio > horaFin`. Validation must happen at write time, not read time.

**File modified:** `application/service/UnitManagementService.java` — `updateSchedule()` method

**Validation added (before persisting):**
```java
if (!request.horaInicio().isBefore(request.horaFin())) {
    throw new ScheduleConflictException(
        "horaInicio must be before horaFin — overnight schedules are not supported");
}
```

`ScheduleConflictException` already exists in `domain/exception/`. Mapped to 409 in `GlobalExceptionHandler`.

| Status | Task |
|---|---|
| ✅ | Add `horaInicio < horaFin` validation in `UnitManagementService.updateSchedule()` before persisting |
| ✅ | Test in `UnitManagementServiceTest` — `updateSchedule_whenHoraInicioNotBeforeHoraFin_throwsScheduleConflictException` |
| ✅ | Remove `FIXME-SCHEDULE-VALIDATION` comment from `Unit.java` |
| ✅ | `mvn test` — 243 passing, 0 failures |

---

#### 3.3 — FIXME-PERF: Eliminate triple JWT parse per request (RESOLVED 2026-07-07)

**Problem:** `JwtAuthenticationFilter` calls `isTokenValid()`, `extractUserId()`, and `extractRole()` — three parses of the same JWT per request.

**Files to modify (Phase 7):**
- `application/port/out/TokenService.java` — add `parseToken()` method
- `infrastructure/security/JwtService.java` — implement `parseToken()`
- `infrastructure/security/JwtAuthenticationFilter.java` — use new method

**Port change (`TokenService`):**
```java
record TokenClaims(Long userId, String role, Duration remainingTtl) {}
TokenClaims parseToken(String token);
```

**`JwtService.parseToken()`:** parse once, extract `sub`, `role`, compute remaining TTL, return `TokenClaims`.

**`JwtAuthenticationFilter`:** replace the three calls with a single `tokenService.parseToken(token)`. Exception → 401. Claims → build `Authentication`.

**Rules:**
- `parseToken()` throws the same exceptions as `isTokenValid()` on invalid tokens — `JwtAuthenticationFilter` already handles them.
- `isTokenValid()`, `extractUserId()`, `extractRole()` remain in the port for compatibility with existing tests — do not remove.

| Status | Task |
|---|---|
| ✅ | Add `record TokenClaims(Long userId, String role, Duration remainingTtl)` in `TokenService.java` |
| ✅ | Add `TokenClaims parseToken(String token)` to the `TokenService` port |
| ✅ | Implement `parseToken()` in `JwtService.java` |
| ✅ | Refactor `JwtAuthenticationFilter` to use `parseToken()` — one parse per request |
| ✅ | Remove `FIXME-PERF` comment block from `TokenService.java` |
| ✅ | `mvn test` — 268 passing, 0 failures, 0 errors, 2 skipped (2026-07-07) |

Exit condition: `JwtAuthenticationFilter` calls `tokenService.parseToken()` once per request. Existing filter tests remain green. **RESOLVED 2026-07-07.**

---

### Layer 4 — Technical Debt: Supply Chain + BCrypt ✅ (RESOLVED 2026-07-08)

#### 4.1 — FIXME-Q6: dependency-check-maven (RESOLVED 2026-07-08)

**File to modify:** `pom.xml`

```xml
<plugin>
    <groupId>org.owasp</groupId>
    <artifactId>dependency-check-maven</artifactId>
    <version>10.0.4</version>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
    <configuration>
        <failBuildOnCVSS>7</failBuildOnCVSS>
        <suppressionFile>${project.basedir}/.dependency-check-suppressions.xml</suppressionFile>
    </configuration>
</plugin>
```

`failBuildOnCVSS=7` — build fails on any HIGH or CRITICAL CVE without an approved suppression.

Create `.dependency-check-suppressions.xml` empty file at project root (for future justified suppressions with reason comment + review date).

| Status | Task |
|---|---|
| ✅ | Add `dependency-check-maven` plugin to `pom.xml` (`verify` phase) — also added `nvdApiKeyEnvironmentVariable=NVD_API_KEY` and `ossindexAnalyzerEnabled=false` (Sonatype OSS Index needs separate, unconfigured credentials — NVD is our sole source) |
| ✅ | Create `.dependency-check-suppressions.xml` at project root — no local paths, not added to `.gitignore` |
| — | (superseded — file has real content, not empty; see suppressions below) |
| ✅ | Run `mvn dependency-check:check` — **BUILD SUCCESS 2026-07-08.** 0 CVEs with CVSS ≥ 7.0 unsuppressed. Required: bump `spring-boot-starter-parent` 3.5.14→3.5.16 (last 3.5.x OSS release), bump `jaxws-rt` 4.0.2→4.0.5, override `tomcat.version=10.1.56` property (fixes CVE-2026-53434/55276/53404), and 3 justified suppressions for confirmed false positives — `angus-activation` (CVE-2025-7962, real CVE is in angus-mail/SMTP, not activation), `jaxb-impl` (CVE-2026-2586/2587, real CVEs are GlassFish admin-console RCEs, we run embedded Tomcat not GlassFish), `log4j-api` + all `io.netty:*` (CVE-2026-34477/78/79/80/81 are in log4j-**core** Layout classes, absent from our tree — confirmed via `mvn dependency:tree`; CVE-2026-42582 is in netty-codec-http3, unused, and already fixed in the Netty version we carry). Remaining low-severity findings (`commons-lang3`, `hibernate-validator`, `jackson-databind`, swagger-ui's bundled DOMPurify) are all below CVSS 7.0 — informational only, not blocking. |
| ✅ | Remove `FIXME-Q6` marker — plugin fully wired, `mvn verify`/`dependency-check:check` green |

---

#### 4.2 — FIXME-TIMING: Validate BCrypt cost factor

**Problem:** The `DUMMY_HASH` in `AuthService` uses cost factor `$2a$10$...`. If production uses a different factor, the timing defense is ineffective.

**Resolution applied:** Cost factor 10 confirmed — `new BCryptPasswordEncoder()` default = 10; `DUMMY_HASH = $2a$10$...` matches. Documented in `application-prod.properties`. `FIXME-TIMING` comment removed from `AuthService.java`.

**Remaining hardening (Phase 7):** Make BCrypt strength configurable via env var so the cost factor can be increased without code changes.

**`application.properties` addition (Phase 7):**
```properties
app.security.bcrypt-strength=${BCRYPT_STRENGTH:10}
```

**`ApplicationConfig` change (Phase 7):** the `@Bean BCryptPasswordEncoder` reads the configured strength:
```java
@Bean
public PasswordEncoder passwordEncoder(@Value("${app.security.bcrypt-strength}") int strength) {
    return new BCryptPasswordEncoder(strength);
}
```

**`SecurityConfig` change (Phase 7):** remove the `@Bean PasswordEncoder` declared there — consolidate in `ApplicationConfig` per ADR-009.

| Status | Task |
|---|---|
| ✅ | Confirm BCrypt cost factor 10 matches `DUMMY_HASH` — documented in `application-prod.properties` |
| ✅ | Remove `FIXME-TIMING` comment from `AuthService.java` |
| ✅ | Add `app.security.bcrypt-strength=${BCRYPT_STRENGTH:10}` to `application.properties` |
| ✅ | Add `BCRYPT_STRENGTH=10` to `.env.example` |
| ✅ | Move `@Bean PasswordEncoder` from `SecurityConfig` to `ApplicationConfig` with `@Value("${app.security.bcrypt-strength}")` |
| ✅ | Remove `@Bean PasswordEncoder` from `SecurityConfig` (+ removed orphaned `BCryptPasswordEncoder`/`PasswordEncoder` imports) |
| ✅ | Update `DUMMY_HASH` comment in `AuthService` — indicate it must match `BCRYPT_STRENGTH` |
| ✅ | **Beyond original scope (security audit finding, user-approved):** added a fail-fast constructor guard in `AuthService` — parses the cost factor embedded in `DUMMY_HASH` and throws `IllegalStateException` at startup if it doesn't match the injected `bcryptStrength`. Prevents a silent ASVS V2.7.1 timing-defense regression if `BCRYPT_STRENGTH` is ever changed without regenerating `DUMMY_HASH`. Same fail-fast pattern as `AdminUserInitializer`. 2 new tests (`constructor_withStrengthMatchingDummyHash_constructsSuccessfully`, `constructor_withStrengthNotMatchingDummyHash_throwsIllegalStateException`). `AuthServiceTest` switched from `@InjectMocks` to manual `@BeforeEach` construction (same reason as `AdminUserInitializerTest` — non-mock primitive constructor param). |
| ✅ | `mvn test` — 270 passing, 0 failures, 0 errors, 2 skipped (2026-07-08) |

Exit condition: BCrypt strength is configurable via env var. `ApplicationConfig` is the sole `PasswordEncoder @Bean` declaration. `SecurityConfig` has no business beans.

---

### Layer 5 — Flyway: `pulse_log` Table ✅

#### 5.1 — `V5__pulse_log.sql`

File: `src/main/resources/db/migration/V5__pulse_log.sql`

```sql
CREATE TABLE pulse_log (
    id             BIGINT        AUTO_INCREMENT PRIMARY KEY,
    num_unidad     VARCHAR(100)  NOT NULL,
    status         ENUM(
                     'SENT',
                     'SKIPPED_INACTIVE',
                     'SKIPPED_OUT_OF_WINDOW',
                     'SKIPPED_STALE',
                     'SKIPPED_NO_COORDS',
                     'REJECTED',
                     'ERROR'
                   )             NOT NULL,
    lat            DECIMAL(9,6)  NULL,
    lon            DECIMAL(9,6)  NULL,
    provider       VARCHAR(50)   NULL,
    tracking_number VARCHAR(255) NULL,
    sent_at        DATETIME      NOT NULL,
    error_message  VARCHAR(500)  NULL,
    INDEX idx_pulse_log_num_unidad (num_unidad),
    INDEX idx_pulse_log_status     (status),
    INDEX idx_pulse_log_sent_at    (sent_at),
    INDEX idx_pulse_log_unit_sent  (num_unidad, sent_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

**Design rules:**
- `lat`, `lon`, `provider`, `tracking_number` are NULL — a skipped dispatch has no coordinates.
- `error_message` is NULL for successful dispatches.
- `sent_at` is the moment the outcome is known (success or failure), not the GPS event time.
- `DECIMAL(9,6)` — 9 total digits, 6 decimal places. Sufficient for sub-meter coordinate precision.
- No FK to `units(num_unidad)` — `pulse_log` is an auditable historical record that must survive unit deletion.

| Status | Task |
|---|---|
| ✅ | Create `V5__pulse_log.sql` with schema + all indexes |
| ✅ | Start Docker + Spring Boot — confirm Flyway applies V5 without errors |
| ✅ | `SHOW CREATE TABLE pulse_log` in MySQL — verify all indexes present |
| ✅ | `mvn test` — Flyway validates schema in tests (`@DataJpaTest` uses H2 — confirmed H2-compatible) |

Exit condition: `pulse_log` table present in DB. Flyway reports `V5__pulse_log.sql` as applied. Tests free of schema errors.

---

### Layer 6 — Domain: PulseLog Model + PulseLogRepository Port ✅

#### 6.1 — `PulseLogStatus.java`

File: `domain/model/PulseLogStatus.java`

```java
public enum PulseLogStatus {
    SENT,
    SKIPPED_INACTIVE,
    SKIPPED_OUT_OF_WINDOW,
    SKIPPED_STALE,
    SKIPPED_NO_COORDS,
    REJECTED,
    ERROR
}
```

Rules: pure domain enum. No Spring or JPA imports. Values must match the MySQL ENUM in V5 exactly.

#### 6.2 — `PulseLog.java`

File: `domain/model/PulseLog.java`

```java
public record PulseLog(
    String numUnidad,
    PulseLogStatus status,
    BigDecimal lat,           // null for skips
    BigDecimal lon,           // null for skips
    String provider,          // null for skips
    String trackingNumber,    // null for skips
    ZonedDateTime sentAt,
    String errorMessage       // null if successful
) {
    public PulseLog {
        Objects.requireNonNull(numUnidad, "numUnidad must not be null");
        Objects.requireNonNull(status,    "status must not be null");
        Objects.requireNonNull(sentAt,    "sentAt must not be null");
    }

    public static PulseLog sent(String numUnidad, GpsReading reading,
                                 String trackingNumber, ZonedDateTime sentAt) { ... }

    public static PulseLog skipped(String numUnidad, PulseLogStatus status, ZonedDateTime sentAt) { ... }

    public static PulseLog failed(String numUnidad, GpsReading reading,
                                   String trackingNumber, ZonedDateTime sentAt, String errorMessage) { ... }
}
```

Rules:
- `record` — immutable, no setters.
- Factory methods `sent()`, `skipped()`, `failed()` cover all three use cases — no ad-hoc construction.
- `lat`, `lon` are nullable `BigDecimal` — same type as `GpsReading`.
- No `id` field — infrastructure concern.

#### 6.3 — `PulseLogRepository.java`

File: `application/port/out/PulseLogRepository.java`

```java
public interface PulseLogRepository {
    void save(PulseLog pulseLog);

    List<PulseLog> findByFilters(
        String numUnidad,         // null = all units
        PulseLogStatus status,    // null = all statuses
        ZonedDateTime from,       // null = no lower bound
        ZonedDateTime to,         // null = no upper bound
        int page,
        int size
    );

    long countByFilters(
        String numUnidad,
        PulseLogStatus status,
        ZonedDateTime from,
        ZonedDateTime to
    );
}
```

Rules:
- Pure port in `application/` — no Spring, no JPA.
- `findByFilters` returns `List<PulseLog>` — pagination managed by the caller (controller or test).
- `countByFilters` is needed so the controller can return `totalElements` in the paginated response.

| Status | Task |
|---|---|
| ✅ | Create `PulseLogStatus.java` enum in `domain/model/` |
| ✅ | Create `PulseLog.java` record in `domain/model/` with factory methods |
| ✅ | Create `PulseLogRepository.java` port in `application/port/out/` |
| ✅ | `mvn test` — ArchUnit passes with new classes (pure domain, no forbidden imports) |

Exit condition: All 3 files compile. ArchUnit reports no violations. `PulseLog` is an immutable record. `PulseLogRepository` imports nothing from Spring or JPA.

---

### Layer 7 — GPS: Cache + Traccar Receiver + Adapter ✅

#### 7.1 — `GpsPositionCache.java`

File: `infrastructure/adapter/out/cache/GpsPositionCache.java`

```java
@Component  // justified exception: stateful infrastructure singleton, not a port adapter
public class GpsPositionCache {

    public record CachedReading(GpsReading reading, Instant receivedAt) {}

    private final ConcurrentHashMap<String, CachedReading> cache = new ConcurrentHashMap<>();
    private final Clock clock;
    private final long maxAgeSeconds;

    public GpsPositionCache(Clock clock,
            @Value("${gps.max-coordinate-age-seconds}") long maxAgeSeconds) { ... }

    public void store(String numUnidad, GpsReading reading) { ... }
    public Optional<CachedReading> findLatest(String numUnidad) { ... }
    public boolean isStale(CachedReading cached) { ... }
}
```

Rules:
- `ConcurrentHashMap` — thread-safe for simultaneous writes from multiple phones.
- `store()` always overwrites — most recent push is the newest reading.
- No eviction — 5 units max, negligible footprint.
- `Clock` constructor-injected — testable with `Clock.fixed()`.
- `maxAgeSeconds` from `gps.max-coordinate-age-seconds` — already present in `application.properties`.

#### 7.2 — `TraccarPositionController.java`

File: `infrastructure/adapter/in/web/TraccarPositionController.java`

Two endpoints:

```
GET  /api/gps/position — OsmAnd protocol (query params: id, lat, lon, timestamp, accuracy, speed, bearing)
POST /api/gps/position — Traccar Client native HTTP (JSON body: device_id, location.coords)
```

Both endpoints are `permitAll()` — OsmAnd/Traccar push protocols have no authentication headers.

Validations (both handlers):
- `lat ∈ [-90.0, 90.0]` — otherwise: 400 + `/errors/invalid-coordinates`
- `lon ∈ [-180.0, 180.0]` — otherwise: 400 + `/errors/invalid-coordinates`
- `lat == 0.0 AND lon == 0.0` — 400 (GPS not locked)
- `id`/`device_id` blank/null — 400 + `/errors/validation-failed`
- Unknown `id`: silently accepted, 200 (no 404 — prevents unit enumeration)

Logging: coordinates rounded to 4 decimal places — never log full precision.

#### 7.3 — `TraccarCoordinateAdapter.java`

File: `infrastructure/adapter/out/gps/TraccarCoordinateAdapter.java`

Implements: `GpsCoordinateProvider`

- `isAvailable(numUnidad)` — returns `false` if no reading exists or reading is stale.
- `getCoordinates(numUnidad)` — returns cached `GpsReading` if fresh; throws `GpsProviderUnavailableException` on miss or stale.
- No `@Component` — declared as `@Bean` in `GpsProviderConfig`.

#### 7.4 — Update `GpsProviderConfig.java`

Activate the TRACCAR branch:

```java
@Bean
@ConditionalOnProperty(name = "gps.provider", havingValue = "traccar")
public GpsCoordinateProvider traccarCoordinateAdapter(GpsPositionCache cache) {
    return new TraccarCoordinateAdapter(cache);
}
```

Change default in `application.properties`:
```properties
gps.provider=${GPS_PROVIDER:traccar}
```

Remove the `// Phase 6 — uncomment when Traccar is introduced` comment.

#### 7.5 — Remove `FIXME-PHASE6` guard in `PulseController.java`

The guard that rejected `coordinateMode=AUTOMATIC` has been removed. `TraccarCoordinateAdapter` now provides real coordinates. Force-dispatch with `AUTOMATIC` is fully operational.

| Status | Task |
|---|---|
| ✅ | Create `GpsPositionCache.java` in `infrastructure/adapter/out/cache/` |
| ✅ | Create `TraccarPositionController.java` in `infrastructure/adapter/in/web/` (GET handler) |
| ✅ | Add `POST /api/gps/position` handler to `TraccarPositionController` (Traccar Client native HTTP) |
| ✅ | Add `TraccarClientPushRequest.java` DTO record in `infrastructure/adapter/in/web/dto/` |
| ✅ | Register `TraccarPositionController` in `ApplicationConfig` as `@Bean` |
| ✅ | Create `TraccarCoordinateAdapter.java` in `infrastructure/adapter/out/gps/` |
| ✅ | Activate TRACCAR branch in `GpsProviderConfig.java` |
| ✅ | Change `gps.provider=traccar` default in `application.properties` |
| ✅ | Remove `FIXME-PHASE6` guard from `PulseController.java` |
| ✅ | Remove `// Phase 6 — uncomment` comment from `GpsProviderConfig.java` |
| ✅ | Add `GET /api/gps/position` and `POST /api/gps/position` to `SecurityConfig` as `permitAll()` |
| ✅ | `mvn test` — 263 passing, 0 failures, 0 ArchUnit violations |

Exit condition: App starts with `gps.provider=traccar`. `GpsPositionCache` receives pushes from both GET (OsmAnd) and POST (Traccar Client) handlers. `TraccarCoordinateAdapter` is the active `GpsCoordinateProvider`. `PulseController` accepts `coordinateMode=AUTOMATIC`.

---

### Layer 8 — Pulse Log: Persistence + Dispatch Integration + REST API ✅

#### 8.1 — `PulseLogEntity.java` + `PulseLogJpaRepository.java`

File: `infrastructure/adapter/out/persistence/entity/PulseLogEntity.java`

```java
@Entity
@Table(name = "pulse_log")
public class PulseLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "num_unidad", nullable = false)
    private String numUnidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PulseLogStatusEntity status;

    @Column(precision = 9, scale = 6) private BigDecimal lat;
    @Column(precision = 9, scale = 6) private BigDecimal lon;
    @Column(length = 50)              private String provider;
    @Column(name = "tracking_number") private String trackingNumber;
    @Column(name = "sent_at", nullable = false) private LocalDateTime sentAt;
    @Column(name = "error_message", length = 500) private String errorMessage;

    @PrePersist void onPersist() { if (sentAt == null) sentAt = LocalDateTime.now(); }
}
```

`PulseLogStatusEntity` — JPA enum with the same values as the domain `PulseLogStatus`:
```java
public enum PulseLogStatusEntity {
    SENT, SKIPPED_INACTIVE, SKIPPED_OUT_OF_WINDOW,
    SKIPPED_STALE, SKIPPED_NO_COORDS, REJECTED, ERROR
}
```

File: `infrastructure/adapter/out/persistence/PulseLogJpaRepository.java`

```java
public interface PulseLogJpaRepository extends JpaRepository<PulseLogEntity, Long> {

    @Query("""
        SELECT p FROM PulseLogEntity p
        WHERE (:numUnidad IS NULL OR p.numUnidad = :numUnidad)
          AND (:status    IS NULL OR p.status    = :status)
          AND (:from      IS NULL OR p.sentAt   >= :from)
          AND (:to        IS NULL OR p.sentAt   <= :to)
        ORDER BY p.sentAt DESC
    """)
    Page<PulseLogEntity> findByFilters(
        @Param("numUnidad") String numUnidad,
        @Param("status")    PulseLogStatusEntity status,
        @Param("from")      LocalDateTime from,
        @Param("to")        LocalDateTime to,
        Pageable pageable
    );
}
```

#### 8.2 — `PulseLogJpaAdapter.java`

File: `infrastructure/adapter/out/persistence/PulseLogJpaAdapter.java`

Implements: `PulseLogRepository`

Explicit mapping `PulseLog ↔ PulseLogEntity` — same pattern as other adapters:
- `toDomain(PulseLogEntity)` → `PulseLog`
- `toEntity(PulseLog)` → `PulseLogEntity` with `id = null` (forces INSERT)
- `LocalDateTime` ↔ `ZonedDateTime` with explicit `FLEET_TIMEZONE`

#### 8.3 — Update `PulseOrchestrationService.java`

Inject `PulseLogRepository` by constructor. Write log entry for each dispatch outcome:

| Case | `PulseLogStatus` | Data recorded |
|---|---|---|
| `!unit.isActive()` | `SKIPPED_INACTIVE` | no coords |
| `!unit.isWithinActiveWindow()` | `SKIPPED_OUT_OF_WINDOW` | no coords |
| `!gpsProvider.isAvailable()` | `SKIPPED_NO_COORDS` | no coords |
| `GpsProviderUnavailableException` (stale) | `SKIPPED_STALE` | no coords |
| `pulseSender.send()` successful | `SENT` | coords + provider + tracking |
| `PulseSendException` (SOAP rejected) | `REJECTED` | coords + error_message |
| Any other exception | `ERROR` | coords if available + error_message |

Capture `GpsProviderUnavailableException` in `sendPulse()` — log WARN `SKIPPED_STALE` + write pulse_log + continue (no re-throw).

#### 8.4 — `PulseLogController.java` + `PulseLogResponse.java`

File: `infrastructure/adapter/in/web/PulseLogController.java`

```
GET /api/pulse-log — ADMIN + USER

Query params (all optional):
  numUnidad   String           — filter by unit
  status      PulseLogStatus   — filter by status
  from        String ISO-8601  — start date (inclusive)
  to          String ISO-8601  — end date (inclusive)
  page        int default 0
  size        int default 20, max 100

Response 200:
  {
    "content": [ PulseLogResponse... ],
    "page": { "number": 0, "size": 20, "totalElements": 150, "totalPages": 8 }
  }
```

`PulseLogResponse` record:
```java
public record PulseLogResponse(
    Long id,
    String numUnidad,
    String status,
    BigDecimal lat,
    BigDecimal lon,
    String provider,
    String trackingNumber,
    String sentAt,       // ISO-8601
    String errorMessage
) {}
```

Rules:
- `size > 100` → 400 `/errors/validation-failed`.
- `from` and `to` parsed as `ZonedDateTime` in controller — format error → 400.
- Controller injects `PulseLogRepository` directly (no use case port for log reads — simple read query).
- Added to `SecurityConfig`: `.requestMatchers(HttpMethod.GET, "/api/pulse-log").hasAnyAuthority(ROLE_ADMIN, ROLE_USER)`

| Status | Task |
|---|---|
| ✅ | Create `PulseLogStatusEntity.java` enum in `infrastructure/adapter/out/persistence/entity/` |
| ✅ | Create `PulseLogEntity.java` in `infrastructure/adapter/out/persistence/entity/` |
| ✅ | Create `PulseLogJpaRepository.java` with paginated `findByFilters()` |
| ✅ | Create `PulseLogJpaAdapter.java` implementing `PulseLogRepository` |
| ✅ | Declare `PulseLogJpaAdapter` as `@Bean` in `ApplicationConfig` |
| ✅ | Inject `PulseLogRepository` into `PulseOrchestrationService` by constructor |
| ✅ | Write `PulseLog` entry in `PulseOrchestrationService` for each dispatch outcome |
| ✅ | Capture `GpsProviderUnavailableException` in `sendPulse()` → `SKIPPED_STALE` + pulse_log + continue |
| ✅ | Remove any remaining `FIXME-PHASE6` from `PulseController.java` |
| ✅ | Create `PulseLogResponse.java` record in `infrastructure/adapter/in/web/dto/` |
| ✅ | Create `PulseLogController.java` — `GET /api/pulse-log` paginated and filterable |
| ✅ | Add `/api/pulse-log` to `SecurityConfig` authorization matrix |
| ✅ | `mvn test` — 263 passing, 0 failures, 0 ArchUnit violations |

Exit condition: Every dispatch writes to `pulse_log`. `GET /api/pulse-log` returns paginated history with filters. `SKIPPED_STALE` appears in logs and in `pulse_log`. `coordinateMode=AUTOMATIC` in force-dispatch works end-to-end.

---

### Layer 9 — Tests ✅

> Implement in parallel where possible. Each test class is independent.

#### 9.1 — `GpsPositionCacheTest` (pure Java — no Spring)

| # | Test | Setup | Expected |
|---|---|---|---|
| 1 | `store_andFindLatest_returnsReading` | `store("Peugeot", reading)` | `findLatest("Peugeot")` → `Optional.of(cached)` |
| 2 | `findLatest_unknownUnit_returnsEmpty` | No prior store | `findLatest("Peugeot")` → `Optional.empty()` |
| 3 | `store_overwritesPreviousReading` | Two `store()` calls with different coords | Second reading overwrites the first |
| 4 | `isStale_withFreshReading_returnsFalse` | `Clock.fixed(now)`, reading with `receivedAt = now - 100s` | `isStale()` → `false` |
| 5 | `isStale_withStaleReading_returnsTrue` | `Clock.fixed(now)`, reading with `receivedAt = now - 400s` | `isStale()` → `true` |
| 6 | `isStale_atExactBoundary_returnsFalse` | `receivedAt = now - 300s` exactly | `isStale()` → `false` (not strictly before) |

Use `Clock.fixed(Instant.parse("2026-07-02T10:00:00Z"), FLEET_TIMEZONE)`.

---

#### 9.2 — `TraccarCoordinateAdapterTest` (Mockito — no Spring)

| # | Test | Setup | Expected |
|---|---|---|---|
| 1 | `getCoordinates_withFreshReading_returnsGpsReading` | Cache returns `Optional.of(freshCached)`, `isStale = false` | Returns `reading` |
| 2 | `getCoordinates_withNoReading_throwsGpsProviderUnavailable` | Cache returns `Optional.empty()` | Throws `GpsProviderUnavailableException` |
| 3 | `getCoordinates_withStaleReading_throwsGpsProviderUnavailable` | Cache returns `Optional.of(staleCached)`, `isStale = true` | Throws `GpsProviderUnavailableException` |
| 4 | `isAvailable_withFreshReading_returnsTrue` | Cache has fresh reading | `isAvailable("Peugeot")` → `true` |
| 5 | `isAvailable_withStaleReading_returnsFalse` | Cache has stale reading | `isAvailable("Peugeot")` → `false` |
| 6 | `isAvailable_withNoReading_returnsFalse` | Empty cache | `isAvailable("Peugeot")` → `false` |

---

#### 9.3 — `TraccarPositionControllerTest` (@SpringBootTest + MockMvc)

| # | Test | Request | Expected |
|---|---|---|---|
| 1 | `receivePosition_withValidCoords_returns200` | `?id=Peugeot&lat=19.4326&lon=-99.1332` | 200 OK; `gpsPositionCache.store()` called with `"Peugeot"` |
| 2 | `receivePosition_withLatOutOfRange_returns400` | `?id=Peugeot&lat=91.0&lon=0.0` | 400 |
| 3 | `receivePosition_withLonOutOfRange_returns400` | `?id=Peugeot&lat=0.0&lon=181.0` | 400 |
| 4 | `receivePosition_withZeroZeroCoords_returns400` | `?id=Peugeot&lat=0.0&lon=0.0` | 400 |
| 5 | `receivePosition_withBlankId_returns400` | `?id=&lat=19.43&lon=-99.13` | 400 |
| 6 | `receivePosition_withUnknownUnit_returns200` | `?id=UnknownUnit&lat=19.43&lon=-99.13` | 200 OK (no 404 — prevents unit enumeration) |
| 7 | `receivePosition_withoutAuthHeader_returns200` | GET, no `Authorization` header | 200 OK (public endpoint) |
| 8 | `receivePosition_withMissingLatParam_returns400` | `?id=Peugeot&lon=-99.13` | 400 |
| 9 | `receivePosition_withSouthernHemisphereCoords_returns200` | `?id=Peugeot&lat=-33.8688&lon=151.2093` | 200 OK |
| 10 | `receivePositionFromClient_withValidJsonBody_returns200` | POST JSON `{"device_id":"Peugeot","location":{"coords":{...}}}` | 200 OK; `store()` called with `"Peugeot"` |
| 11 | `receivePositionFromClient_withInvalidLatitude_returns400` | POST JSON with `latitude: 95.0` | 400 |
| 12 | `receivePositionFromClient_withoutAuthHeader_returns200` | POST JSON, no auth header | 200 OK (public endpoint) |

Rules:
- `@MockitoBean GpsPositionCache` — do not touch the real cache in controller tests.
- Test #6/unknown unit: `verify(gpsPositionCache).store(eq("UnknownUnit"), any())` — endpoint accepts any `id` without DB validation.

---

#### 9.4 — `PulseLogJpaAdapterTest` (@DataJpaTest + H2)

| # | Test | Setup | Expected |
|---|---|---|---|
| 1 | `save_sentLog_persistsAllFields` | `PulseLog.sent(...)` | All fields present on retrieval |
| 2 | `save_skippedLog_persistsWithNullCoords` | `PulseLog.skipped(...)` | `lat`, `lon`, `provider`, `trackingNumber` = null |
| 3 | `findByFilters_byNumUnidad_returnsOnlyMatchingUnit` | 3 logs (2 Peugeot, 1 Kangoo) | Filter `numUnidad=Peugeot` → 2 results |
| 4 | `findByFilters_byStatus_returnsOnlyMatchingStatus` | 2 SENT + 1 SKIPPED_STALE | Filter `status=SENT` → 2 results |
| 5 | `findByFilters_byDateRange_returnsOnlyInRange` | 3 logs on distinct dates | Filter `from/to` → only entries within range |
| 6 | `findByFilters_noFilters_returnsAll` | 3 logs | No filters → 3 results |
| 7 | `findByFilters_empty_returnsEmptyList` | No logs | Empty list |

---

#### 9.5 — `PulseOrchestrationServiceTest` — extend existing class

Added to the existing class:

| # | Test | Description |
|---|---|---|
| + | `sendPulse_whenSent_writesSentLogEntry` | Successful dispatch → `pulseLogRepository.save()` called with `status=SENT` |
| + | `sendPulse_whenOutOfWindow_writesSkippedOutOfWindowLog` | Outside window → `save()` with `status=SKIPPED_OUT_OF_WINDOW` |
| + | `sendPulse_whenInactive_writesSkippedInactiveLog` | Inactive unit → `save()` with `status=SKIPPED_INACTIVE` |
| + | `sendPulse_whenStale_writesSkippedStaleLog` | `GpsProviderUnavailableException` stale → `save()` with `status=SKIPPED_STALE`, no re-throw |
| + | `sendPulse_whenNoCoords_writesSkippedNoCoordsLog` | `isAvailable() = false` → `save()` with `status=SKIPPED_NO_COORDS` |
| + | `sendPulse_whenSoapRejected_writesRejectedLog` | `PulseSendException` → `save()` with `status=REJECTED` |

Use `@Mock PulseLogRepository` injected alongside existing mocks.

---

#### 9.6 — `LoginRateLimitFilterTest` (@SpringBootTest + MockMvc)

| # | Test | Setup | Expected |
|---|---|---|---|
| 1 | `login_firstFiveAttempts_returns401NotRateLimited` | 5 logins with invalid creds | 5 × 401 `/errors/invalid-credentials` (not 429) |
| 2 | `login_sixthAttemptFromSameIp_returns429` | 6th attempt from same IP | 429 + `/errors/rate-limited` + `Content-Type: application/problem+json` |
| 3 | `login_attemptsFromDifferentIps_dontShareBucket` | 5 attempts IP-A, then 1 attempt IP-B | IP-B: 401 (not 429) |
| 4 | `login_afterWindowExpiry_bucketResets` | 5 attempts, wait >1 min, 1 more attempt | 401 (not 429) — bucket reset |

Note: Test #4 requires `Clock.fixed()` or `Thread.sleep(61000)` — tagged `@Tag("slow")` and run only in CI.

---

#### 9.7 — ArchUnit: Verify new packages

Verify in `HexagonalArchitectureTest` that the new `infrastructure/adapter/out/cache/` package complies with existing rules:
- Infrastructure can import domain (correct direction)
- No `@Transactional` on cache (not applicable — not JPA)
- No `@Autowired` on fields

No new rules added — the 11 existing rules already cover the new files.

| Status | Task |
|---|---|
| ✅ | `GpsPositionCacheTest` — 6 tests (pure Java, no Spring) |
| ✅ | `TraccarCoordinateAdapterTest` — 6 tests (Mockito) |
| ✅ | `TraccarPositionControllerTest` — 12 tests (@SpringBootTest + MockMvc — 9 GET + 3 POST) |
| ✅ | `PulseLogJpaAdapterTest` — 7 tests (@DataJpaTest + H2) |
| ✅ | `PulseOrchestrationServiceTest` — +6 tests added to existing class |
| ✅ | `LoginRateLimitFilterTest` — 4 tests (@SpringBootTest + MockMvc) |
| ✅ | ArchUnit — `HexagonalArchitectureTest` passes with new packages |
| ✅ | `mvn test` — 263 passing, 0 failures, 0 ArchUnit violations |

Exit condition: All test classes compile and pass. `mvn test` is clean. Test count reflects Phase 5 base (235) + all Phase 6 additions.

---

### Layer 10 — Manual Smoke Test (Gate for v1.1.0) ✅

> ⚠️ **LIVE EXTERNAL SERVICE.** Every push reaches QSolutions in production. Do not execute until Layer 9 is complete.

#### Prerequisites

| Prerequisite | How to verify |
|---|---|
| Traccar Client installed on at least one phone | App open, status = `Running` |
| Protocol = native HTTP, Server URL = ngrok or `http://<local-ip>:8080/api/gps/position` | App settings |
| Device Identifier = `Peugeot` (or the unit under test) | App settings |
| Docker + Spring Boot running with `gps.provider=traccar` | Startup logs free of errors |
| `GPS_MAX_COORDINATE_AGE_SECONDS=300` in `.env` | `grep GPS_MAX .env` |
| `QSOLUTIONS_TRACKING_NUMBER=RIVA` in `.env` | `grep QSOLUTIONS_TRACKING .env` |

#### Step A — GPS push received ✅

| Status | Task |
|---|---|
| ✅ | A.1 Open Traccar Client → force manual send or wait for interval |
| ✅ | A.2 Confirm in logs: `INFO GPS_RECEIVED numUnidad=Peugeot lat=XX.XXXX lon=-XX.XXXX` |
| ✅ | A.3 Confirm coordinates truncated to 4 decimal places in log (not 6) |
| ✅ | A.4 Coordinates are not `0.0, 0.0` (phone GPS has a fix) |

#### Step B — Dispatch with real coordinate ✅

| Status | Task |
|---|---|
| ✅ | B.1 `POST /api/units/Peugeot/pulse/force` with `coordinateMode=AUTOMATIC` → 204 No Content |
| ✅ | B.2 Confirm in logs: `INFO PULSE_SENT numUnidad=Peugeot provider=TRACCAR` |
| ✅ | B.3 Confirm in `pulse_log` table: `SELECT * FROM pulse_log WHERE status='SENT' ORDER BY sent_at DESC LIMIT 1` |
| ✅ | B.4 `lat`, `lon` in `pulse_log` match the Traccar Client reading |
| ✅ | B.5 QSolutions confirmed reception (`isProcessed = true` in logs) — tracking number `RIVA` |

#### Step C — SKIPPED_STALE verified

| Status | Task |
|---|---|
| ⬜ | C.1 Put phone in airplane mode (stop GPS sends) |
| ⬜ | C.2 Wait 310 seconds (over the 300 s stale threshold) |
| ⬜ | C.3 `POST /api/units/Peugeot/pulse/force` with `coordinateMode=AUTOMATIC` |
| ⬜ | C.4 Confirm in logs: `WARN SKIPPED_STALE numUnidad=Peugeot` |
| ⬜ | C.5 Confirm in `pulse_log`: `SELECT * FROM pulse_log WHERE status='SKIPPED_STALE'` has at least 1 row |
| ⬜ | C.6 QSolutions received NO pulse during this period |

#### Step D — Round scheduling with real GPS

| Status | Task |
|---|---|
| ⬜ | D.1 Reactivate Traccar Client (exit airplane mode) — confirm `GPS_RECEIVED` in logs |
| ⬜ | D.2 Start round for Peugeot → `POST /api/units/Peugeot/round/start` → 204 |
| ⬜ | D.3 Wait 2 full 15-min cycles — confirm `ROUND_PULSE_SENT` with `provider=TRACCAR` in logs |
| ⬜ | D.4 Confirm 2 `status=SENT` entries in `pulse_log` with real coordinates |
| ⬜ | D.5 Confirm `GET /api/pulse-log?numUnidad=Peugeot&status=SENT&size=5` returns the records |
| ⬜ | D.6 Stop round → `POST /api/units/Peugeot/round/stop` → 204 |

#### Step E — Pulse log API verified

| Status | Task |
|---|---|
| ✅ | E.1 `GET /api/pulse-log` → 200 with `content[]` and pagination |
| ✅ | E.2 `GET /api/pulse-log?numUnidad=Peugeot` → only Peugeot entries |
| ✅ | E.3 `GET /api/pulse-log?status=SENT` → only successful entries |
| ⬜ | E.4 `GET /api/pulse-log?status=SKIPPED_STALE` → entries from Step C |
| ⬜ | E.5 `GET /api/pulse-log?size=101` → 400 `/errors/validation-failed` |

**Exit condition:** Real Traccar coordinate present in at least one live `PULSE_SENT` event ✅. `GET /api/pulse-log` returns filtered history ✅. Steps C–D can be completed any time; they do not block v1.1.0 tagging since the primary gate is met.

---

### Layer 11 — Round Resilience: Transient GPS Loss No Longer Kills the Round (✅ RESOLVED — code 2026-07-10, real-hardware smoke test 2026-07-11)

**Discovered:** 2026-07-10, during Layer 10 manual smoke testing with real Traccar Client hardware (Peugeot + ngrok tunnel). Attempting to reproduce Step C (`SKIPPED_STALE`) revealed that the roadmap's Step C spec describes behavior from `PulseOrchestrationService.sendPulse()` — a method only ever called by `PulseSchedulerService`, which is **disabled by default** (`scheduler.pulse.global-enabled=false`) and is not the dispatch mechanism this project actually runs in production (round scheduling is). The real round-tick path (`RoundManagementService.processTick()`) had a materially different, untested-against-real-hardware behavior: **any** `GpsProviderUnavailableException` (stale OR missing coordinate) removed the round from `activeRounds` entirely — same treatment as `UnitNotActiveException` (unit deactivated by an ADMIN). This is confirmed intentional, pre-existing, tested behavior (`RoundManagementServiceTest` 9.4.21, 9.4.22, 9.4.24) — not a regression.

**Problem this causes in real operation:** a transient GPS gap (tunnel, dead zone, OS briefly killing background location) permanently stops automatic dispatch for that unit — round scheduling requires a human to notice and call `POST /round/start` again. At fleet scale this is expected to happen routinely, not as a rare edge case, and erodes trust in unattended automation. It also means the round path has **zero audit trail** in `pulse_log` when GPS fails — unlike `sendPulse()`, which already logs `SKIPPED_STALE`/`SKIPPED_NO_COORDS`.

**Decision:** `GpsProviderUnavailableException` (transient — GPS hardware/connectivity issue) and `UnitNotActiveException` (deliberate operator action — ADMIN deactivated the unit) are fundamentally different failure classes and must be handled differently:
- `GpsProviderUnavailableException` → **skip this tick only**, keep the round active, write a `pulse_log` entry (`SKIPPED_STALE` or `SKIPPED_NO_COORDS`, same distinction `sendPulse()` already makes), retry on the normal cadence.
- `UnitNotActiveException` → **unchanged.** Still removes the round immediately — deactivation is explicit and permanent until an ADMIN reactivates, there is nothing to "wait out."

**Root cause location:** `RoundManagementService.processTick()` line ~141 calls `gpsProvider.getCoordinates(numUnidad)` directly (not inside `dispatch()`) — the exception originates here, so the fix is fully contained to this class plus its dependency wiring. No change needed in `PulseController`, `PulseOrchestrationService.dispatch()`, or `TraccarCoordinateAdapter`.

**Rejected alternative (scope control):** redesigning `GpsProviderUnavailableException` with a typed `Reason` field (replacing the existing `e.getMessage().contains("stale")` string-sniffing in `PulseOrchestrationService.isStaleMessage()`) was considered and **rejected** — this exception class is shared by `QSolutionsSoapAdapter` (SOAP transport failure), `ManualCoordinateAdapter`, and `ProviderTestService`, none of which fit a stale/no-data `Reason` enum. Redesigning it would touch unrelated code far outside this fix's scope. `RoundManagementService` gets its own small `isStaleMessage()` helper mirroring `PulseOrchestrationService`'s existing (already-accepted) pattern instead.

**Retry-cadence detail (must not regress):** on a stale/skip outcome, `ultimoEnvio` in `RoundState` **must still be updated to the current tick's instant**, exactly like the success path. If left unchanged, the next tick (every `scheduler.round.tick-ms`, default 30s) would immediately retry `getCoordinates()` instead of waiting the normal `scheduler.round.interval-ms` (default 15 min) — turning a single stale reading into a 30-second retry storm instead of a normal-cadence retry.

#### Files to change

| File | Change |
|---|---|
| `application/service/RoundManagementService.java` | Add `PulseLogRepository` constructor dependency. Add private `isStaleMessage(GpsProviderUnavailableException)` (mirrors `PulseOrchestrationService`). Rewrite the `catch (GpsProviderUnavailableException e)` block in `processTick()`: remove `activeRounds.remove(numUnidad)`; instead update `ultimoEnvio` (same as success path), write `pulseLogRepository.save(PulseLog.skipped(numUnidad, skipStatus, now))`, log at `INFO` (matches `sendPulse()`'s level, not `WARN` — this becomes a routine/expected event, not an anomaly). `catch (UnitNotActiveException e)` block: **unchanged.** |
| `infrastructure/config/ApplicationConfig.java` | `roundManagementService(...)` bean gains a `PulseLogRepository` parameter, passed through to the constructor. |
| `test/.../RoundManagementServiceTest.java` | Add `@Mock PulseLogRepository pulseLogRepository`; update `setUp()` constructor call. Rewrite test 9.4.21 → round stays active, `pulse_log` gets `SKIPPED_STALE` entry (or split into two tests: stale-message vs no-data-message, mirroring `PulseOrchestrationServiceTest`'s existing coverage style). Update test 9.4.24 → Kangoo (GPS failure) must now **stay** in `activeRounds`, not be removed; add assertion that `pulseLogRepository.save(...)` was called for Kangoo. Add a new test confirming `ultimoEnvio` is updated on a skipped tick (retry-cadence regression guard). Test 9.4.22 (`UnitNotActiveException` → round removed) stays as-is, unchanged. |
| `CLAUDE.md` | New ADR documenting this decision (transient vs. permanent failure handling in round scheduling) and why it differs from `sendPulse()`'s legacy/unused global-scheduler behavior. |

#### Checklist

| Status | Task |
|---|---|
| ✅ | Add `PulseLogRepository` to `RoundManagementService` constructor |
| ✅ | Add `isStaleMessage()` private helper to `RoundManagementService` |
| ✅ | Rewrite `GpsProviderUnavailableException` catch block in `processTick()` — skip + log + pulse_log write + update `ultimoEnvio`, do NOT remove the round |
| ✅ | Confirm `UnitNotActiveException` catch block is untouched |
| ✅ | Update `ApplicationConfig.roundManagementService()` bean to inject `PulseLogRepository` |
| ✅ | Update `RoundManagementServiceTest` — new mock, rewritten 9.4.21 (+9.4.21b, +9.4.21c), updated 9.4.24 |
| ✅ | Add ADR-019 to `CLAUDE.md` documenting transient-vs-permanent failure handling in round scheduling |
| ✅ | `mvn test` — 278 passing, 0 failures, 0 errors, 2 skipped, 0 ArchUnit violations (2026-07-10) |
| ✅ | Manual smoke test (real Peugeot phone, 2026-07-11): airplane mode → 3× `ROUND_TICK_SKIPPED reason=SKIPPED_STALE` logged at the exact configured cadence (2 min, confirms the retry-storm guard works) → zero `ROUND_REMOVED_GPS_UNAVAILABLE` → round never re-started → 3 `pulse_log` rows confirmed via `GET /api/pulse-log?status=SKIPPED_STALE` → GPS resumed → round self-healed with a real `PULSE_SENT`/`DISPATCH_SENT`/`ROUND_PULSE_SENT` (QSolutions confirmed, tracking=RIVA), no manual `/round/start` needed |

**Exit condition — MET 2026-07-11:** A transient GPS gap during an active round produces a `pulse_log` entry and an `INFO` log line, but the round survives and resumes automatic dispatch once GPS data is fresh again — no operator intervention required. `UnitNotActiveException` behavior is provably unchanged (existing test 9.4.22 still green, unmodified). `mvn test` green (278 passing), 0 ArchUnit violations. Confirmed against real Traccar Client hardware, not just mocks.

---

### Known Debt Introduced / Confirmed in Phase 6

| ID | Location | Severity | Description | Resolution Phase |
|---|---|---|---|---|
| FIXME-SEC-FAMILY | `AuthService.java` | MEDIUM | Refresh Token Families not implemented (OAuth 2.0 BCP §4.14). Token theft is undetectable. | Post-frontend (Phase 10+) |
| FIXME-PROXY | `LoginRateLimitFilter.java` | LOW | Rate limit uses `getRemoteAddr()` — if API is behind nginx, read `X-Forwarded-For`. Add when proxy is configured. | Phase 9 deploy (L8, nginx) |
| FIXME-PERF | `TokenService.java` | LOW | Triple JWT parse per request (`isTokenValid` + `extractUserId` + `extractRole`). Resolution: `TokenClaims record` + `parseToken()`. | **RESOLVED 2026-07-07** ✅ |
| FIXME-Q6 | `pom.xml` | RESOLVED | `dependency-check-maven` configured, NVD API key wired, 0 unsuppressed CVEs ≥ CVSS 7.0. `mvn dependency-check:check` — BUILD SUCCESS. | Phase 6 §4.1 ✅ 2026-07-08 |
| FIXME-REFRESH-BODY | `AuthController.java` | CRITICAL→MEDIUM (reclassified 2026-07-06 — no active exploit, UX-driven not incident-driven) | Refresh token returned in JSON response body. Move to `httpOnly` + `Secure` + `SameSite=Strict` cookie so the frontend never needs `localStorage` and sessions survive page reload. | Phase 7 |
| ADR-003 | `User.java`, `UserRepository.java` | LOW | `userId` is a DB surrogate key in the domain. Deferred indefinitely — does not block frontend. | Post-frontend (Phase 10+) |

---

## Phase 7 — Auth Hardening + httpOnly Refresh Cookie
**Tag:** `v1.2.0`
**Exit condition:** Refresh token is never present in a JSON response body — delivered exclusively via `httpOnly` + `Secure` + `SameSite=Strict` cookie scoped to `Path=/api/auth`. `JwtAuthenticationFilter` parses each JWT exactly once per request. `dependency-check-maven` runs on `mvn verify` with zero unsuppressed HIGH/CRITICAL CVEs. BCrypt strength is configurable via env var. ADR-018 documents the CSRF re-evaluation. `mvn test` passes with zero failures and zero ArchUnit violations. Manual cookie smoke test executed with evidence captured.

> Decided 2026-07-06. Order: Layer 1 (carryover, independent, low risk) → Layer 2 (cookie migration, the sensitive one) → Layer 3 (ADR) → Layer 4 (tests + smoke).

### Layer 1 — Carryover from Phase 6 (fully specified above, execute now)

These three items were spec'd in detail during Phase 6 but explicitly deferred. No new design work — flip the checkboxes.

| Order | Item | Full spec | Status |
|---|---|---|---|
| 1.1 | FIXME-PERF — `TokenClaims` record + single-parse `parseToken()` | Phase 6 §3.3 above | ✅ RESOLVED 2026-07-07 |
| 1.2 | FIXME-Q6 — `dependency-check-maven` plugin, `failBuildOnCVSS=7` | Phase 6 §4.1 above | ✅ RESOLVED 2026-07-08 |
| 1.3 | BCrypt strength configurable via `app.security.bcrypt-strength` / `BCRYPT_STRENGTH` env var, `PasswordEncoder` bean consolidated into `ApplicationConfig` | Phase 6 §4.2 above | ✅ RESOLVED 2026-07-08 |

Exit condition: all three carryover checklists in Phase 6 §3.3/§4.1/§4.2 fully checked. `mvn test` green.

---

### Layer 2 — httpOnly Refresh Token Cookie Migration

**Problem (FIXME-REFRESH-BODY):** `POST /api/auth/login` and `POST /api/auth/refresh` currently return `refreshToken` in the JSON body. The frontend has no safe place to persist it: `localStorage` is readable by any injected script (XSS → full account takeover), and in-memory storage loses the session on every page reload (F5). Reclassified from an initial CRITICAL label to MEDIUM on 2026-07-06 — the in-memory plan was never actually vulnerable, the real driver is UX (session survives reload), and the fix (`httpOnly` cookie) is the OWASP-recommended pattern for SPA refresh tokens.

**Architectural note — this is (almost) an infrastructure-only change.** `AuthService`, `RefreshTokenRepository`, `TokenService`, and the domain layer need **zero modifications** for `login()`/`refresh()` — `AuthResult(accessToken, refreshToken, expiresAt)` is unchanged; only *where the controller puts* `refreshToken` changes (`Set-Cookie` instead of a JSON field). **One deviation found during implementation:** `AuthService.logout(accessToken, refreshToken)` now must tolerate `refreshToken == null`, because `@CookieValue(required = false)` can hand the controller `null` in the multi-tab case (user already logged out in another tab, cookie already cleared, but this tab's access token is still live). This is a legitimate application-layer change, not a hexagonal violation — `logout()`'s *contract* changes (nullable second parameter), which is a business-rule concern (what does "logout" mean when there's no refresh token to revoke), not an HTTP concern. Everything else stayed exactly where planned.

#### 2.1 — Cookie contract (definitive) ✅ RESOLVED 2026-07-13

```
Set-Cookie: refresh_token=<jwt>; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800
```

| Attribute | Value | Reason |
|---|---|---|
| `HttpOnly` | always | JS cannot read it — the entire point of the migration |
| `Secure` | always (`app.security.cookie-secure=${COOKIE_SECURE:true}`) | Modern browsers treat `http://localhost` as a secure-context exception, so `Secure` still works in local dev without HTTPS. Env toggle kept only for exotic dev setups (LAN IP, non-localhost tunnel). |
| `SameSite` | `Strict` | Refresh/logout are the only endpoints reading this cookie, and they are never triggered by a top-level cross-site navigation. `Strict` is stricter than `Lax` with no functional cost here. |
| `Path` | `/api/auth` | Scopes the cookie to `login`/`refresh`/`logout` only — it is never sent on `/api/units`, `/api/pulse-log`, etc. Reduces exposure surface. Must be **identical** on set and clear, or the browser creates a second cookie instead of deleting the first. |
| `Max-Age` | `604800` (7 days) on set · `0` on clear | Matches existing `RefreshToken.expiresAt` contract (04-jwt-hardening.md) |
| `Domain` | not set | Defaults to the current host. Phase 9 L8 serves frontend + API from the same origin via nginx, so no cross-domain cookie is ever needed. |

**Correction to an earlier (2026-07-06 same-day) assessment:** `SameSite` is evaluated on the registrable domain ("site"), which ignores port. `http://localhost:5173` (Vite dev server) and `http://localhost:8080` (API) are **same-site**, so `SameSite=Strict` does **not** block the cookie in local dev, contrary to what was flagged earlier. The real dev-mode requirement is **CORS**, not `SameSite`: exact-origin match (`ALLOWED_ORIGIN=http://localhost:5173`) plus `allowCredentials(true)` on the backend, and `withCredentials: true` (Axios) on the frontend. A Vite dev proxy is optional (it would avoid the CORS preflight entirely) but is not required for the cookie to work — do not treat it as a blocker for Phase 7.

#### 2.2 — DTO changes ✅ RESOLVED 2026-07-13

| File | Change |
|---|---|
| `dto/LoginResponse.java` | Remove `refreshToken` field → `record LoginResponse(String accessToken, Instant expiresAt)` |
| `dto/RefreshResponse.java` | Remove `refreshToken` field → `record RefreshResponse(String accessToken, Instant expiresAt)` |
| `dto/RefreshRequest.java` | **Delete.** No request body needed — token now arrives via `@CookieValue`. |
| `dto/LogoutRequest.java` | **Delete.** Same reason — no body needed on logout. |

#### 2.3 — `AuthController` changes ✅ RESOLVED 2026-07-13

```
POST /api/auth/login — permitAll
  Request:  LoginRequest (username, password)      — unchanged
  Response: LoginResponse (accessToken, expiresAt)  — refreshToken REMOVED from body
            Set-Cookie: refresh_token=...            — added
  200 OK / 401 on invalid credentials

POST /api/auth/refresh — permitAll
  Request:  none — @CookieValue(value = "refresh_token", required = false) String refreshToken
  Response: RefreshResponse (accessToken, expiresAt)
            Set-Cookie: refresh_token=...            — rotated value (single-use rotation unchanged)
  200 OK
  401 (/errors/token-invalid) if cookie is missing — see guard below, must NOT be a 500/NPE
  401 (/errors/token-expired | /errors/token-revoked) per existing AuthService exceptions

POST /api/auth/logout — authenticated (ADMIN or USER)
  Request:  none — @CookieValue(value = "refresh_token", required = false) String refreshToken
  Access token: still from Authorization header (ADR-010 — unchanged)
  Response: 204 No Content
            Set-Cookie: refresh_token=...; Max-Age=0  — clears the cookie, same Path/SameSite/Secure attrs
```

**Guard — missing cookie on refresh must not become a 500.** `AuthService` methods use `Objects.requireNonNull` per project null-safety rules (03-java-production.md) — passing a `null` refresh token straight through would throw `NullPointerException`, not a mapped `RefreshTokenNotFoundException`. `AuthController.refresh()` must check the cookie value itself and throw `RefreshTokenNotFoundException` explicitly before calling the use case:

```java
if (refreshToken == null) {
    throw new RefreshTokenNotFoundException("Missing refresh_token cookie");
}
```

**Guard — missing cookie on logout must still blacklist the access token.** This is the multi-tab case: user already logged out in another tab, cookie is gone, but the current tab still calls `/api/auth/logout` with a live access token. `AuthService.logout()` must tolerate a `null` refreshToken by skipping the DB revoke step (not calling `revokeByToken(null)`) while still blacklisting the access token — this extends the "full logout regardless" behavior already built in Phase 6 L1 (FIXME-LOGOUT-REFRESH) to the new null case.

**Cookie construction** — use Spring's `ResponseCookie` (an HTTP concern, lives in the controller per ADR-010, not a Spring Security type so it does not violate any hexagonal prohibition):

```java
ResponseCookie cookie = ResponseCookie.from("refresh_token", token)
    .httpOnly(true)
    .secure(cookieSecure)
    .sameSite("Strict")
    .path("/api/auth")
    .maxAge(Duration.ofSeconds(604800))
    .build();
response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
```

#### 2.4 — CORS / `SecurityConfig` changes ✅ RESOLVED 2026-07-13

| Task | Detail |
|---|---|
| Add `.allowCredentials(true)` to `corsConfigurationSource()` | Required for the browser to send/receive the cookie cross-origin (dev mode) |
| Confirm `ALLOWED_ORIGIN` is never `*` | Already true since FIXME-CORS (Phase 6 L2) — `allowCredentials(true)` + wildcard origin is rejected by browsers anyway, so this is also a correctness check, not just security |
| **Do not** add `Set-Cookie` to `exposedHeaders` | Common misconception (present in the original plan) — `exposedHeaders` only controls whether **JavaScript** can read a response header. `httpOnly` cookies are set by the browser automatically regardless of CORS `exposedHeaders`; exposing `Set-Cookie` would be both useless and pointless here since JS must never read this cookie |

#### 2.5 — ADR-018 (document in `CLAUDE.md` when this layer starts) ✅ RESOLVED 2026-07-13 — added to CLAUDE.md §3 ADR table (renumbering note: the informal "ADR-018" label previously used for the Phase 6 GPS `id`↔`numUnidad` design note was never formalized in CLAUDE.md's table, so it was relabeled as an unnumbered design note to free this slot for the real ADR-018 below)

> **ADR-018 — `csrf.disable()` remains safe after introducing the refresh-token cookie.**
> Status: ACCEPTED. Context: `SecurityConfig` disables CSRF globally (stateless API, Bearer-only access token). Introducing a cookie could reopen CSRF if not scoped correctly. Resolution: the cookie is `Path=/api/auth`, read only by `refresh` and `logout` — no state-mutating business endpoint (`/api/units`, `/api/pulse-log`, etc.) ever reads a cookie, they only accept `Authorization: Bearer`. `SameSite=Strict` blocks the cookie on any cross-site request. Combined, a forged cross-site request cannot trigger `refresh` or `logout` with attacker-controlled effect beyond what an unauthenticated request could already do. Revisit if `SameSite` is ever relaxed to `Lax`/`None`.

#### 2.6 — Test changes ✅ RESOLVED 2026-07-13

| Test class | Change | Status |
|---|---|---|
| `AuthControllerTest` | Rewrite login/refresh assertions: `result.getResponse().getCookie("refresh_token")` instead of `jsonPath("$.refreshToken")`. Assert cookie attributes (`isHttpOnly()`, `getSecure()`, `getPath()`, `getMaxAge()`). Add regression test: login response body does **not** contain a `refreshToken` key at all. Add: refresh with missing cookie → 401, not 500. Add: logout sets `Max-Age=0`. | ✅ |
| `AuthServiceTest` | Add: `logout_withNullRefreshToken_stillBlacklistsAccessTokenWithoutCallingRevoke` — `verify(refreshTokenRepository, never()).revokeByToken(any())`, `verify(tokenBlacklist).blacklist(...)` still called | ✅ |
| `JwtAuthenticationFilterTest` | No change expected — access token still travels via `Authorization` header only | ✅ confirmed no change needed |

**Evidence:** `mvn test` — 282 tests, 0 failures, 0 errors, 2 skipped, `HexagonalArchitectureTest` 11/11 ArchUnit rules passing, BUILD SUCCESS. Confirmed 2026-07-13 (net +4 over the 278 baseline: +3 `AuthControllerTest`, +1 `AuthServiceTest`).

#### 2.7 — Manual smoke test (cookie jar, curl or Postman) ✅ RESOLVED 2026-07-13

| Step | Command / action | Expected | Actual result |
|---|---|---|---|
| 1 | `POST /api/auth/login` with valid credentials | 200, body has `accessToken` + `expiresAt` only (no `refreshToken`), `Set-Cookie` present with `HttpOnly; Secure; SameSite=Strict; Path=/api/auth` | ✅ exact match. Cookie payload decoded: `{"sub":"1","iat":...,"exp":...}`, `exp-iat=604800s`, no `role` claim — matches `04-jwt-hardening.md` refresh token contract |
| 2 | `POST /api/auth/refresh` using cookie from step 1 | 200, new `accessToken`, `Set-Cookie` rotated to a new value | ✅ exact match, new cookie value confirmed distinct from step 1's |
| 3 | Replay the **old** cookie value from step 1 against `/api/auth/refresh` | 401, single-use rotation holds | ✅ `401 /errors/token-invalid` (corrected from the original spec's `/errors/token-revoked` guess — `RefreshTokenNotFoundException` and `RefreshTokenRevokedException` share one `GlobalExceptionHandler` handler, both map to `token-invalid`) |
| 4 | `POST /api/auth/logout` with `Authorization` header + cookie from step 2 | 204, `Set-Cookie: refresh_token=; Max-Age=0` | ✅ exact match, clearing cookie carries identical `Secure/HttpOnly/SameSite/Path` attributes as the set cookie (required for the browser to recognize it as the same cookie) |
| 5 | `POST /api/auth/refresh` again with the now-revoked cookie from step 2 | 401 `/errors/token-invalid` | ✅ exact match |
| 6 | `POST /api/auth/refresh` with **no cookie at all** | 401 `/errors/token-invalid` — not 500 | ✅ exact match, confirmed via the explicit `AuthController.refresh()` null-guard, no NPE |
| 7 | Inspect every response from steps 1–6 | `accessToken` never appears in a `Set-Cookie` header; `refresh_token` never appears in a JSON body | ✅ verified character-by-character across all 6 responses — zero cross-contamination in either direction |

**Environment note for future runs (Windows/PowerShell):** `curl.exe` invoked from Windows PowerShell 5.1 mangles JSON bodies containing embedded double quotes passed via `-d '...'` or `-d "..."` directly on the command line (a native-argv quoting bug, not an application bug — surfaced as `HttpMessageNotReadableException` / `UNREADABLE_BODY` server-side). Workaround used: write the JSON to a file with `[System.IO.File]::WriteAllText(path, json)` (UTF-8 without BOM, unlike `Set-Content -Encoding utf8` which adds a BOM in PS 5.1) and pass it via `-d "@file.json"`.

Exit condition: all 7 steps produce the expected result with evidence (terminal output) captured above. **v1.2.0 gate fully met** — see Phase 7 exit condition at the top of this section.

**Finding surfaced during this smoke test — logged as new debt, not fixed here (out of scope for Layer 2):**

| ID | Location | Severity | Description | Resolution Phase |
|---|---|---|---|---|
| FIXME-EXPIRES-AT-AMBIGUOUS | `AuthUseCase.AuthResult`, `LoginResponse`, `RefreshResponse` | MEDIUM (contract-clarity, not a security bug) | `AuthResult.expiresAt()` is populated in `AuthService.login()`/`.refresh()` from `TokenService.GeneratedRefreshToken.expiresAt()` — i.e. it is the **refresh token's** expiry (+7 days), not the access token's. Confirmed by decoding the real JWTs from the smoke test: `accessToken`'s own `exp` claim is 900s (15 min) after `iat`, while the response body's `expiresAt` field reads +604800s. A frontend naively using `expiresAt` from the login/refresh response to schedule a proactive silent-refresh timer would wait 7 days instead of ~15 minutes, leaving the UI to rely entirely on reactive 401-triggered refresh instead. Not a security hole (the access token still genuinely expires at 15 min server-side, so no token gets improperly honored) — but a real contract-precision defect for whoever builds the Phase 9 frontend. Fix options to evaluate in Phase 8: (a) rename the field to `refreshTokenExpiresAt` for honesty, (b) add a second `accessTokenExpiresAt` field, or (c) drop the field entirely and let the frontend decode the JWT's own `exp` claim client-side. | Phase 8 Layer 1 (UI-to-endpoint contract audit) — flag explicitly, do not let it slide into Phase 9 unaddressed |

---

## Phase 8 — Pre-Frontend Readiness
**Tag:** `v1.3.0`
**Exit condition:** every screen planned for Phase 9 has a verified, frozen backend contract — verified with real HTTP calls (not just unit tests), not merely assumed to exist. Phase 9 must be design-and-connect only; any contract change discovered during Phase 9 is treated as a Phase 8 regression.

> This phase exists because "the backend has endpoints" and "the backend is ready for a frontend to consume" are different claims. Phase 4–6 built the endpoints for their own layer's exit conditions; nobody has yet checked them against the four screens designed in `PHASE7_PLAN.txt` (Login, Pulse Log Dashboard, Unit Management, User Management).

### Layer 1 — UI-to-Endpoint Contract Audit

Produce one row per UI action, across all four screens. This is a research/audit deliverable, not code — output is a completed version of the table below, committed to `PHASE7_PLAN.txt` or a new `docs/api-contract-audit.md`.

**Audit method:** read every controller in `infrastructure/adapter/in/web/` (`AuthController`, `UnitController`, `PulseController`, `RoundController`, `UserController`, `PulseLogController`) plus their application-layer ports (`ManageUnitUseCase`, `UserManagementUseCase`) directly against `PHASE7_PLAN.txt`'s screen-by-screen UI design (the original, pre-renumbering plan — still the only source for what each screen needs). Completed 2026-07-13.

| Screen | UI action | Endpoint | Status | Notes |
|---|---|---|---|---|
| Login | Submit credentials | `POST /api/auth/login` | ✅ confirmed | Phase 7 changed response shape (no `refreshToken` in body, cookie instead) |
| Login | Session survives reload | `POST /api/auth/refresh` (silent, on app boot) | ✅ confirmed | See FIXME-EXPIRES-AT-AMBIGUOUS (§2.7) — do not build the silent-refresh timer off the response's `expiresAt` field, it's the refresh token's expiry not the access token's |
| Pulse Log Dashboard | Paginated table + filters (unit, status, date range) | `GET /api/pulse-log` | ⚠️ confirmed with a deviation | `numUnidad`/`status`/`from`/`to`/`page`/`size` all present, filter set matches the UI plan exactly. **But** `size > 100` silently clamps to 100 (`Math.min`, still `200 OK`) instead of rejecting with `400` — contradicts `07-API-Design.md`'s mandatory rule ("Reject with 400 if size > 100") and **corrects a wrong claim** in this document's Phase 6 §Layer 10 Step E.5, which said this was "already covered by automated tests" as a 400 case — the actual test (`PulseLogControllerTest` 9.9.5, `listLogs_withOversizedPage_capsAt100`) asserts the clamp, not a rejection. See Layer 2 gap 2.4. |
| Pulse Log Dashboard | Stat cards (sent/skipped/errors today, units active) | none | ❌ gap confirmed | No aggregate endpoint exists anywhere in the codebase. See Layer 2 gap 2.2. |
| Unit Management | List units as cards | `GET /api/units` | ⚠️ confirmed with a deviation | Returns a raw `List<UnitResponse>` — **not paginated**, contradicts `07-API-Design.md`'s mandatory list-pagination rule. Low real-world severity today (fleet is 5 units), but it's a documented rule violation, not a judgment call already made. `roundActive` **is** present per ADR-017 ✅. See Layer 2 gap 2.5. |
| Unit Management | Edit unit / schedule | `PUT /api/units/{numUnidad}/schedule` | ⚠️ narrower than planned | Schedule update exists and works exactly as ADR-016/017 describe. **There is no generic `PUT /api/units/{numUnidad}`** — only 3 narrow sub-resource endpoints exist: `/activate`, `/deactivate`, `/schedule`. If "edit unit" in the UI plan meant anything beyond toggling active + schedule (e.g. renaming `numUnidad`), that endpoint does not exist. Given `numUnidad` is a natural key used everywhere (DB, JWT-adjacent flows, Traccar device config), renaming is unlikely to ever be a real requirement — recommend confirming the UI plan only needs schedule+activate/deactivate, not a full rename/edit, rather than building a new endpoint for it. |
| Unit Management | Force dispatch button | `POST /api/units/{numUnidad}/pulse/force` | ✅ confirmed, `204` | **Requires a request body** (`coordinateMode`: `MANUAL` needs `lat`/`lon`, `AUTOMATIC` reads the live Traccar cache) — not a bare no-input button click. Frontend needs at minimum a `coordinateMode` default (`AUTOMATIC`, since that's the real production provider per `gps.provider=traccar`). |
| Unit Management | Create new unit | `POST /api/units` | ❌ gap confirmed — does not exist at any layer | No controller mapping (confirmed via grep across every controller), no `ManageUnitUseCase.createUnit()` method, no application-service support. Today the fleet is provisioned exclusively via Flyway/DB seed — there has never been a runtime "add a unit" operation. See Layer 2 gap 2.6 — this is a real scope decision, not a small fix. |
| Unit Management | Toggle round scheduling on/off | `POST /api/units/{numUnidad}/round/start`, `POST /api/units/{numUnidad}/round/stop` | ✅ confirmed | `start` requires `coordinateMode` (+`lat`/`lon` if `MANUAL`); `stop` takes no body. Both return `204`. |
| User Management | List users, table | `GET /api/users` | ✅ confirmed, paginated | `PagedResponse<UserResponse>`, `page`/`size` query params, `size` capped via `@Max(100)` validation (this one correctly rejects with `400` on violation, unlike pulse-log — see gap 2.4 for the inconsistency) |
| User Management | Create user | `POST /api/users` | ✅ confirmed, `201` + `Location` header | — |
| User Management | Edit role / deactivate | `PUT /api/users/{id}` (role/active), `DELETE /api/users/{id}` (deactivate) | ⚠️ confirmed with a real gap | `DELETE` (deactivate) is clean — no body, `204`, works standalone. **But `PUT` is a full-replace requiring a brand-new `rawPassword`** (`@NotBlank @Size(min=8)`) on every single call — a UI action that's "just toggle the role dropdown" or "just flip active" is forced to also submit a password change nobody asked for. No partial-update path exists. See Layer 2 gap 2.7. |

Exit condition: every row has a final status of ✅ (confirmed working as-is) or a linked task in Layer 2 (needs a change). **Met** — 6 of 12 rows needed a Layer 2 gap opened (2.2 and 2.4–2.7 below; 2.1 and 2.3 were already open from before this audit), 6 confirmed working as-is.

### Layer 2 — Close Contract Gaps ✅ COMPLETE 2026-07-15

Each gap found becomes one row here with the same rigor as any other layer in this document (file, change, test, exit condition). Populated from Layer 1's completed audit above (2026-07-13) — these are confirmed gaps, not speculative candidates.

All 7 gaps were reviewed with the user via `AskUserQuestion` on 2026-07-13 — every row now has a **Decision** (chosen direction) and a **Status** (implementation progress). See the plan file used for implementation: `Phase 8 Layer 2 — Close Contract Gaps` (approved 2026-07-14/15). All 7 gaps resolved and committed individually (one commit per gap, per the user's explicit request), 2026-07-15. Along the way, 3 additional issues were found and fixed that weren't part of the original 7: (1) `PulseLogControllerTest`'s gap-2.4 fix initially used the wrong exception type (`HandlerMethodValidationException` instead of the actual `ConstraintViolationException` thrown by `@Validated`'s AOP interceptor), caught by a real failing test run and corrected before commit; (2) `corsConfigurationSource()` was missing `PATCH` from `setAllowedMethods()` entirely, which would have silently broken gap 2.7's new endpoint for a browser frontend; (3) `UserController` had zero test coverage of any kind before gap 2.7, fixed by creating `UserControllerTest` from scratch covering all 5 endpoints.

| Order | Gap | Why it matters | Decision | Status |
|---|---|---|---|---|
| 2.1 | `UnitResponse` may be missing a `lastPulseAt` timestamp | Unit Management cards need it per the Phase 7-session UI design | Added `ZonedDateTime lastPulseAt` to `UnitResponse`. `PulseLogRepository` gained `findLatestSentAt(numUnidad)` (single-unit) and `findLatestSentAtForAllUnits()` (one `GROUP BY` query for `listUnits()`, avoids N+1 across the fleet per `05-jpa-performance.md`). `UnitController` now injects `PulseLogRepository` directly — follows the same driving-adapter-uses-output-port precedent already set by `PulseLogController`. All 5 `UnitResponse.from()` call sites updated. `UnitControllerTest` gained 2 new cases (populated `lastPulseAt` for `listUnits`/`getUnit`) plus explicit null-default assertions on the 2 existing happy-path tests. | ✅ RESOLVED 2026-07-15 |
| 2.2 | No aggregate "stats today" endpoint (`GET /api/pulse-log` has no summary/count-by-status mode) | Dashboard stat cards (sent/skipped/errors today) have nothing to bind to | New `GET /api/pulse-log/stats?date=` (optional, defaults to today in `FLEET_TIMEZONE` via the injected `Clock` bean). `PulseLogRepository.countGroupedByStatus()` — single `GROUP BY status` query, not one query per enum value. Scoped to pulse-log stats only — "units active" is derived by the frontend client-side from the already-loaded `GET /api/units` response, not blended into this endpoint. New `PulseLogStatsResponse` DTO (`date`, `countsByStatus`, `total`). New `SecurityConfig` matcher — `/stats` is a distinct path from the exact-match `GET /api/pulse-log` rule and needed its own explicit line, or it would have fallen through to `anyRequest().denyAll()` (403 for everyone, including ADMIN). 6 new `PulseLogControllerTest` cases. | ✅ RESOLVED 2026-07-15 — **Phase 8 Layer 2 fully closed, all 7 gaps resolved** |
| 2.3 | FIXME-EXPIRES-AT-AMBIGUOUS — `LoginResponse.expiresAt`/`RefreshResponse.expiresAt` is the refresh token's 7-day expiry, not the access token's 15-min expiry | The Login screen's silent-refresh-on-reload logic must not be built against a naive reading of this field | Renamed `AuthResult.expiresAt` → `refreshTokenExpiresAt` (`AuthUseCase.java`, `LoginResponse.java`, `RefreshResponse.java`, 2 asserts in `AuthServiceTest.java`). `AuthService.java` needed no change — it constructs `AuthResult` positionally. `AuthControllerTest.java` needed no change — it never asserted on this field. Frontend decodes its own `accessToken`'s JWT `exp` claim client-side for refresh timing — no second field added. | ✅ RESOLVED 2026-07-15 |
| 2.4 | `GET /api/pulse-log?size>100` silently clamps instead of rejecting with `400`, inconsistent with `GET /api/users` | A frontend bug sending `size=10000` fails silently on one endpoint but loudly on the other | `PulseLogController` now rejects with `400` via `@Min(0)` on `page` + `@Min(1) @Max(100)` on `size` + class-level `@Validated`, matching `UserController`. Clamp line and dead `MAX_SIZE`/`DEFAULT_PAGE`/`DEFAULT_SIZE` constants removed (the latter two were already unused dead code, cleaned up incidentally). New `GlobalExceptionHandler.handleParameterValidationFailure(HandlerMethodValidationException)` maps to the existing `/errors/validation-failed` type URI — this also retroactively fixes `UserController`'s existing `size>100` 400 to use our documented registry entry instead of Spring's generic `about:blank`. `PulseLogControllerTest` 9.9.5 rewritten (clamp assertion → 400 assertion), new 9.9.5b for `page=-1`. | ✅ RESOLVED 2026-07-15 |
| 2.5 | `GET /api/units` returns an unpaginated `List<UnitResponse>`, contradicting `07-API-Design.md`'s mandatory list-pagination rule | An intentional exception needs to be a documented ADR, not a silent gap | **ADR-020** added to `CLAUDE.md` §3 — fleet size is fixed/small (~5 units), pagination would be pure overhead. No code change. | ✅ RESOLVED 2026-07-15 |
| 2.6 | `POST /api/units` (create unit) does not exist at any layer | Confirmed no runtime "add a unit" path exists; corroborated by a dead `SecurityConfig` rule found pointing at this nonexistent endpoint | Dropped from Phase 9 scope — fleet is small/static, ops-provisioned via Flyway. Dead `SecurityConfig.java` matcher for `POST /api/units` removed (replaced with an explanatory comment; `anyRequest().denyAll()` already covers the path safely). | ✅ RESOLVED 2026-07-15 |
| 2.7 | `PUT /api/users/{id}` requires a new `rawPassword` on every call, even for a role/active-only change | Forces the ADMIN to invent/retype a password just to flip a dropdown | New `PATCH /api/users/{id}` endpoint (`UpdateUserRoleRequest` DTO — role + active only, no password field in the DTO at all, structurally absent, not just ignored). `PUT` unchanged for full-replace edits. New `UserManagementUseCase.updateRoleAndActive()`, reuses `UserRepository.save()`. New `SecurityConfig` matcher for `PATCH /api/users/**`. **Also found and fixed along the way:** `corsConfigurationSource().setAllowedMethods()` was missing `PATCH` entirely — harmless for `curl`/tests (CORS is browser-only), but would have silently broken this exact endpoint the moment Phase 9's browser frontend tried to call it cross-origin. **Also found and fixed:** `UserController` had zero tests of any kind before this gap — new `UserControllerTest` created from scratch, covering all 5 endpoints (`GET`/`POST`/`PUT`/`DELETE`/new `PATCH`), not just the new one. `UserManagementServiceTest` gained 2 cases for `updateRoleAndActive`. | ✅ RESOLVED 2026-07-15 |
| 2.8 *(found during post-Layer-2 verification, not one of the original 7)* | No `GET /api/users/{id}` — only the paginated `GET /api/users` list exists | Found manually testing gap 2.7's `PATCH` smoke test 2026-07-16. A Phase 9 "edit user" screen will want to fetch one user directly (e.g. deep-linking to an edit URL, or the target user isn't on the currently loaded list page) instead of paging through the full list client-side to find it | Not fixed — logged as `FIXME-USER-GET-BY-ID` in `CLAUDE.md` §7 per explicit user request, to schedule deliberately. Small, well-understood fix when it happens: mirrors the existing `GET /api/units/{numUnidad}` single-resource pattern exactly. | ⬜ logged as debt, not scheduled to a layer yet |

**Post-Layer-2 verification (2026-07-16):** all 7 gaps re-verified against a real running instance + real MySQL (not just the mocked `mvn test` suite) — this matters because gaps 2.1 and 2.2 both added brand-new JPQL `@Query` methods that Mockito-based controller tests never actually execute (a JPQL syntax error would only surface at runtime, not in `mvn test`). Evidence:
- `GET /api/units` — `lastPulseAt` populated correctly for `Peugeot` (`2026-07-11T19:46:25-06:00`, matching the known real Traccar hardware event from Phase 6 Layer 11), `null` correctly for the other 4 units with no pulse history.
- `GET /api/pulse-log/stats` (no `date`, defaults to "today") — `{"countsByStatus":{},"total":0}`, confirms the empty-day case returns cleanly, not an error.
- `GET /api/pulse-log/stats?date=2026-07-11` — `{"countsByStatus":{"SKIPPED_STALE":3,"SENT":8},"total":11}`, matches the real ADR-019 hardware smoke test evidence from that day exactly — confirms the `GROUP BY` aggregation is correct against real data, not just the empty case.
- `PATCH /api/users/2` (a real user, `operadorJuan`) — `{"id":2,"username":"operadorJuan","role":"ADMIN","active":false}`, confirms username/password untouched, no password field ever in the response, then manually reverted to `USER`/`active:true` after the test.

**`mvn clean test` checkpoint (2026-07-16) found and fixed a separate, unrelated build-reproducibility bug (`FIXME-CLEAN-BUILD`, see `CLAUDE.md` §7):** the SOAP stub code generation (`jaxws-maven-plugin` → `org.tempuri.ReceiveGPSInfoSoap`/`GPSInfo`, from `ReceiveGPSInfo.wsdl`) lived inside a Maven profile (`soap-codegen`) that was never active by default. Every prior `mvn test` in this project's history had run without `clean`, so `target/generated-sources/wsimport` still held stub classes generated once, manually, back in Phase 4 Layer 1 — nobody had ever actually exercised a from-scratch build. `mvn clean test` wiped `target/` and failed with `package org.tempuri does not exist`, proving a fresh clone or a first CI run would fail identically. Fixed by adding `<activation><activeByDefault>true</activeByDefault></activation>` to the profile in `pom.xml`. Confirmed fixed by re-running `mvn clean test` after the change — see git history for the exact commit and test count.

### Layer 3 — OpenAPI Contract Freeze

| Status | Task |
|---|---|
| ✅ | **Prerequisite #1 found while starting this layer, not originally scoped:** `GET /v3/api-docs` and `/swagger-ui.html` had no `SecurityConfig` rule at all — fell through to `anyRequest().denyAll()`, `403` for everyone including a valid ADMIN token. Decided with the user (`AskUserQuestion` 2026-07-16): authenticated, any role (`hasAnyAuthority(ADMIN, USER)`) — same level as `GET /api/pulse-log`, not public, consistent with ADR-008. Added `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` matchers. |
| ✅ | **Prerequisite #2 found immediately after #1 was fixed:** with the security block gone, `/v3/api-docs` returned `500` — `NoSuchMethodError: ControllerAdviceBean.<init>(Object)`. `springdoc-openapi-starter-webmvc-ui:2.5.0` is binary-incompatible with the `spring-webmvc:6.2.19` this project has run since the FIXME-Q6 CVE-driven Spring Boot bump (Phase 6 §4.1, 2026-07-08) — a silent breakage that existed for over a week because nobody had ever actually hit this endpoint before today (and prerequisite #1 was blocking it anyway). Bumped to `2.8.6` (confirmed against Maven Central's actual index, not a search-engine summary that briefly surfaced a version number that doesn't exist). See `FIXME-SPRINGDOC-VERSION` in `CLAUDE.md` §7. |
| ✅ | Regenerated `/v3/api-docs` (2026-07-16, after both prerequisite fixes) — all 17 route entries present (18 counting method variants: `GET`/`POST` on `/api/gps/position`, `GET`/`POST` on `/api/users`, `PUT`/`DELETE`/`PATCH` on `/api/users/{id}`), every one has `@Operation` summary + a `responses` map with a description per status code and a `ProblemDetail` schema ref on every error path. Minor stylistic gap, not structural: a few endpoints (`GET /api/units`, `GET /api/units/{numUnidad}`) have only a `summary`, no extended `description` — acceptable, not blocking. |
| ✅ | Exported the live `/v3/api-docs` JSON, pretty-printed (`ConvertFrom-Json`/`ConvertTo-Json -Depth 100`, explicit `-Encoding utf8` on both read and write — the first attempt without it mangled every em-dash in the doc into `â€”` mojibake) and committed to the repo root as `openapi.json` (1987 lines formatted, vs. the single-line minified fetch). |
| ✅ | No CI pipeline exists in this project (single-developer, manual `mvn`/git workflow throughout) — established as a manual discipline instead, documented in `CLAUDE.md` §9: before touching any controller from here forward, re-fetch `/v3/api-docs` and diff against the committed `openapi.json`; a diff with no matching `ROADMAP.md` update is a Phase 8 regression. |

**Layer 3 exit condition met 2026-07-16.** `openapi.json` committed, reflects the real, currently-running contract (including every Layer 2 addition), both blocking prerequisites found and fixed along the way.

### Layer 4 — End-to-End Integration Test Suite (new) ✅ COMPLETE 2026-07-16

Distinct from the existing unit/controller test suite — this exercises the full stack in one flow, the way the frontend actually will.

**Design decisions (confirmed with the user via `AskUserQuestion`, deviating from the original spec):**
- **H2, not Testcontainers MySQL** — consistent with the rest of the 312-test suite (all `@SpringBootTest` classes already run against H2 via `application-test.properties`). The point of this layer is proving the endpoints *compose*, which doesn't depend on the SQL dialect — MySQL-specific behavior was already verified manually against real MySQL during the Phase 8 post-Layer-2 audit.
- **Real Redis via Testcontainers, not mocked** — the assertion "the access token is rejected after logout" would be meaningless against a mocked `TokenBlacklist` (it would only prove the mock returns what we told it to). Same `@Container`/`@DynamicPropertySource` pattern already established in `RedisTokenBlacklistAdapterTest`.
- **Only `PulseSender` is mocked** — the one genuine external boundary (the real QSolutions SOAP service) that must never be hit by an automated test. Everything else runs for real: `AuthService`, real password verification, real JWT issuance, `JwtAuthenticationFilter`, the httpOnly cookie, `RoundManagementService`/`PulseOrchestrationService`, `PulseLogRepository`, `UnitRepository`. This is also the **first test in the entire suite that calls `POST /api/auth/login` with real credentials** — every other test generates a JWT directly via `TokenService`, bypassing the real login path.

| Status | Task |
|---|---|
| ✅ | New test class `FullDispatchFlowIntegrationTest` (`com.fleetpulse.api.integration` — new package, deliberately separate from the per-controller/per-service test packages) |
| ✅ | Flow implemented exactly as specified: login → cookie + access token captured manually (`TestRestTemplate` does not manage a cookie jar automatically, unlike a browser) → `GET /api/units` confirms a seeded test unit → `POST /api/units/{numUnidad}/pulse/force` (`MANUAL` coordinates) → `GET /api/pulse-log` confirms a new `SENT` entry → `POST /api/auth/refresh` via the captured cookie, both access token and cookie rotate → `POST /api/auth/logout` with the rotated credentials → `GET /api/units` with the now-blacklisted access token confirms `401` |

**Evidence:** `mvn test` — 313 passing (312 + this one), 0 failures, 0 errors, 2 skipped, `BUILD SUCCESS`, passed on the first run.

Exit condition met: the flow passes end-to-end in a single test run, proving the endpoints compose correctly — not just individually.

### Layer 5 — Browser-Realistic CORS/Cookie Verification ✅ COMPLETE 2026-07-16

MockMvc and `TestRestTemplate` do not exercise real browser CORS/cookie enforcement. Before trusting Phase 7's cookie work, prove it under an actual browser.

| Status | Task |
|---|---|
| ✅ | `scratch/cors-test.html` (gitignored, not committed) served via `npx http-server` on `http://localhost:5173` — the exact origin `SecurityConfig`'s `ALLOWED_ORIGIN` already permits — with 4 buttons hitting `/api/auth/login`, `/api/auth/refresh`, `GET /api/units`, `/api/auth/logout`, all with `fetch(..., { credentials: 'include' })` |
| ✅ | Confirmed in real Chrome/Edge DevTools Network tab (not simulated): login `200`, `Set-Cookie` present with every attribute correct — `Path=/api/auth; Max-Age=604800; Secure; HttpOnly; SameSite=Strict`; `access-control-allow-origin` echoes the exact origin (never `*`) with `access-control-allow-credentials: true`; the `refresh` call's **Request Headers** show `cookie: refresh_token=...` sent automatically by the browser — no JS in the test page ever touches the cookie value, proving the round-trip is real, not simulated |
| ✅ | Confirmed the cookie does **not** leak to `GET /api/units` — that request's Request Headers show other (unrelated, pre-existing `localhost`-wide analytics) cookies but **no `refresh_token`**, proving `Path=/api/auth` scoping works for real, not just in a unit test's assumptions |

**Evidence:** captured directly from DevTools Network tab, both `Response Headers` (login/refresh `Set-Cookie`) and `Request Headers` (refresh/units `Cookie`), reviewed 2026-07-16.

### Layer 6 — Full Manual Smoke Test Script

One documented pass per screen, in the same style as the Phase 5 production-replacement smoke test.

| Status | Task |
|---|---|
| ✅ | Login flow (2026-07-17, real prod instance): bad credentials → `401 /errors/invalid-credentials` ✓; good credentials → `200`, `accessToken` + `refreshTokenExpiresAt` only, `Set-Cookie` with all correct attributes ✓; refresh → `200`, new access token + rotated cookie ✓ |
| ✅ | Pulse Log Dashboard flow (2026-07-17, real data): no filters → `200`, 19 entries ✓; `numUnidad=Peugeot` → 19 (all real activity so far is Peugeot) ✓; `status=SENT` → 16, genuinely excludes the 3 `SKIPPED_STALE` ✓; date range `2026-07-11` → 11, matches gap 2.2's evidence exactly (3 `SKIPPED_STALE` + 8 `SENT`) ✓; combined `numUnidad`+`status` → 16, consistent ✓ |
| ✅ (2026-07-17, real prod instance) | List units → `200`, all 5 real units ✓. Deactivate/reactivate `Sentra` (a unit with no real GPS history, chosen deliberately over `Peugeot` to avoid disrupting the real hardware unit's live round) → both `200` ✓ as ADMIN, both `403` ✓ as USER (see FIXME-ERROR-DISPATCH-403 below — found and fixed along the way). Edit schedule on `Sentra` → `200` ✓ (see note below — a real diagnostic detour, not an app bug). Force dispatch on `Peugeot` (real, per user's explicit choice — not the dry-run endpoint) → `204`, a genuine `PULSE_SENT` against QSolutions ✓. Round toggle on `Sentra` (start → `204`, stop → `204`) — incidentally produced a real dispatch during the active window, confirmed by `Sentra` gaining a non-null `lastPulseAt` afterward, unplanned but welcome end-to-end evidence that round scheduling still works. **"Create unit" removed from this script 2026-07-16 — dropped from scope entirely in gap 2.6, no endpoint exists to test.** |
| | **Diagnostic detour (2026-07-17, not an app bug):** the schedule-edit step initially failed repeatedly with `400 /errors/validation-failed "cannot be parsed as valid JSON"` even though `curl -v` confirmed the exact right byte count was sent and the file content was valid JSON (ruled out both `-d` and `--data-binary`). Root cause: `ScheduleUpdateRequest.horaInicio`/`horaFin` are annotated `@JsonFormat(pattern = "HH:mm")` — the smoke test script was sending `"07:00:00"` (matching the `HH:mm:ss` shape used elsewhere in this API, e.g. `sentAt`), but this endpoint specifically requires the shorter `HH:mm` shape with no seconds. Jackson's format-mismatch `DateTimeParseException` gets wrapped into the same generic `HttpMessageNotReadableException` as a truly-malformed body, which is why the error message didn't point at the real cause. Not a code defect — corrected the test input (`"07:00"` instead of `"07:00:00"`) and it passed immediately. |
| | **Real bug found + fixed (2026-07-17), FIXME-ERROR-DISPATCH-403:** `PUT /api/units/Sentra/deactivate`/`activate` returned `401` instead of `403` for a valid, correctly-authenticated USER-role token — reproduced identically in curl and Postman, ruling out any client-side artifact. Root cause, found via temporary diagnostic logging in `JwtAuthenticationFilter` and a temporary custom `accessDeniedHandler` in `SecurityConfig` (both removed after diagnosis): `JwtAuthenticationFilter` extends `OncePerRequestFilter`, which by default skips itself on the internal `DispatcherType.ERROR` forward (`shouldNotFilterErrorDispatch()` defaults to `true`) that Tomcat performs after `response.sendError(403)` to render Spring Boot's error page. Spring Security's own `AuthorizationFilter`/`ExceptionTranslationFilter` **do** run on that forward — since `/error` had no explicit rule, it fell through to `anyRequest().denyAll()`, found no `Authentication` (because our filter never re-ran to set one), and its `401` clobbered the original, correct `403`. This means **every** authenticated-but-wrong-role request in the real running app (not just this endpoint) had been silently returning `401` instead of `403` since `SecurityConfig` was first written — the entire 313-test MockMvc suite passed throughout because MockMvc never performs a real container-level error dispatch, so it could not catch this. Fixed with one line: `.requestMatchers("/error").permitAll()` added to `authorizeHttpRequests` (must come before `anyRequest().denyAll()`). Re-verified: `mvn test` still 313 passing/0 failures/0 ArchUnit violations, and the real server now returns `403` for both `activate` and `deactivate` under the USER token. See `CLAUDE.md` §7 for the permanent FIXME record. |
| ✅ | User Management flow (2026-07-17, real prod instance): `GET /api/users` as ADMIN → `200`, 3 real users ✓. Create `temp_test_user` → `201` + `Location: /api/users/4`, no `passwordHash` in body ✓. `PATCH /api/users/4` role → `ADMIN` → `200` ✓. `DELETE /api/users/4` → `204` ✓. USER token (`smoketest_user`) on all four (`GET`/`POST`/`PATCH`/`DELETE`) → `403` on every one ✓ (confirms the `FIXME-ERROR-DISPATCH-403` fix holds across the whole controller, not just Unit Management). **Bonus test, not in the original script:** deactivated `smoketest_user` (id 3) via `DELETE`, then attempted login → `401 /errors/invalid-credentials` (not a distinct "account disabled" message — deliberate, `GlobalExceptionHandler` groups `InvalidCredentialsException` and `UserNotActiveException` into the same response to avoid leaking account existence/state, confirmed by reading `AuthService.login()` + `GlobalExceptionHandler` directly). Reactivated via `PATCH` (`active:true`) → login succeeded again with a fresh `200` + token, proving the full deactivate/reactivate cycle round-trips correctly. |

### Layer 7 — Contract Freeze Declaration

| Status | Task |
|---|---|
| ⬜ | Tag `v1.3.0` only after Layers 1–6 are complete |
| ⬜ | Add explicit note to `CLAUDE.md` Phase 9 readiness checklist: "No backend contract changes during Phase 9 except genuine bugs found while integrating — any UI-driven shape change is a Phase 8 regression" |

Exit condition: `v1.3.0` tagged. Every screen in Phase 9 can be built against a contract that will not move under it.

---

## Phase 9 — React Frontend (renumbered from the original Phase 7 plan)
**Tag:** `v2.0.0`
**Exit condition:** frontend deployed on the same origin as the API via nginx. All four screens functional against the frozen Phase 8 contract. Auth flow uses the Phase 7 httpOnly cookie — no token ever touches `localStorage`.

> Built screen by screen, in the order a user actually encounters them: Login → Home/Dashboard shell → Pulse Log (the default landing screen for both roles) → Unit Management → User Management. Backend cookie prep (previously "L0" in the original plan) is now fully covered by Phase 7 — removed from this phase.

Stack: Vite + TypeScript, shadcn/ui (Tailwind + Radix), Zustand (auth store) + TanStack Query (server state), React Hook Form + Zod, React Router v6, Axios.

### Layer 1 — Scaffold

| Status | Task |
|---|---|
| ⬜ | `frontend/` directory inside the monorepo, Vite + TypeScript template |
| ⬜ | ESLint + Prettier configured |
| ⬜ | shadcn/ui + Tailwind CSS initialized with the color tokens decided in `PHASE7_PLAN.txt` (sidebar `#0f172a`, background `#f8fafc`, primary `#6366f1`) |
| ⬜ | Folder structure: `src/screens/`, `src/components/`, `src/lib/api/`, `src/store/` |

### Layer 2 — API Client

| Status | Task |
|---|---|
| ⬜ | Axios instance, `baseURL` from env, `withCredentials: true` (required for the Phase 7 cookie) |
| ⬜ | Response interceptor: on 401 from a non-auth endpoint, attempt silent `POST /api/auth/refresh`, then retry the original request |
| ⬜ | Race-condition guard: while a refresh is in flight, queue concurrent 401s instead of firing parallel refresh calls — resolve the queue when the in-flight refresh completes |
| ⬜ | RFC 7807 error normalization — one place that turns `ProblemDetail` JSON into a typed error the UI can render |

### Layer 3 — Screen: Login

| Status | Task |
|---|---|
| ⬜ | Login form — username/password, client-side validation via Zod, submit via `POST /api/auth/login` |
| ⬜ | Zustand auth store holds `accessToken` + `role` **in memory only** — never persisted to `localStorage`/`sessionStorage` |
| ⬜ | On app boot, silently call `POST /api/auth/refresh` (cookie-driven) to restore a session without asking the user to log in again |
| ⬜ | Protected route wrapper — redirects to `/login` when the store has no valid access token and silent refresh fails |
| ⬜ | UI per `PHASE7_PLAN.txt`: centered card, error banner on invalid credentials, loading state on submit |

### Layer 4 — Shell / Layout (role-aware)

| Status | Task |
|---|---|
| ⬜ | Sidebar + topbar layout, dark sidebar per design spec |
| ⬜ | Nav items driven by role from the auth store, matching the authorization matrix in `04-jwt-hardening.md`: **USER** sees Pulse Log only; **ADMIN** sees Pulse Log + Units + Users |
| ⬜ | User menu — avatar, role chip, logout (calls `POST /api/auth/logout`, clears Zustand store, redirects to `/login`) |

### Layer 5 — Screen: Pulse Log Dashboard (home screen, both roles)

| Status | Task |
|---|---|
| ⬜ | Stat cards (sent/skipped/errors today, units active) — endpoint decided in Phase 8 §2.2 |
| ⬜ | Paginated table via TanStack Query against `GET /api/pulse-log`, columns per `PHASE7_PLAN.txt` |
| ⬜ | Filters: unit, status, date range — wired to query params confirmed in Phase 8 Layer 1 |
| ⬜ | 30s auto-refresh (TanStack Query `refetchInterval`) + manual refresh button |
| ⬜ | Status chips colored per the palette in `PHASE7_PLAN.txt` |

### Layer 6 — Screen: Unit Management (dual-role panel)

| Status | Task |
|---|---|
| ⬜ | Card grid, one per unit, via `GET /api/units` |
| ⬜ | Force-dispatch button visible to **both** ADMIN and USER (matrix: `/api/units/*/pulse/**` is dual-role) |
| ⬜ | Edit / create / delete controls visible to **ADMIN only** — hidden (not just disabled) for USER, since the backend already 403s them; hiding avoids a confusing error state |
| ⬜ | Schedule edit modal, round on/off toggle |

### Layer 7 — Screen: User Management (ADMIN-only panel)

| Status | Task |
|---|---|
| ⬜ | Route guarded — USER role never reaches this screen client-side (defense in depth; server-side 403 is the real boundary) |
| ⬜ | Nav item hidden for USER role (Layer 4) |
| ⬜ | Table: username, role chip, active status, actions dropdown (edit role, deactivate) |
| ⬜ | Create-user form |

### Layer 8 — Deployment

| Status | Task |
|---|---|
| ⬜ | nginx config: reverse-proxy `/api/**` to the Spring Boot process, serve the built frontend static assets on `/`, single origin — eliminates CORS entirely in production and satisfies `SameSite=Strict` with zero cross-site edge cases |
| ⬜ | FIXME-PROXY resolved here: `LoginRateLimitFilter` reads `X-Forwarded-For` behind nginx |
| ⬜ | HTTPS termination at nginx, `Secure` cookie now backed by real TLS instead of the localhost exception |

### Layer 9 — Tests

| Status | Task |
|---|---|
| ⬜ | Vitest + React Testing Library configured |
| ⬜ | Auth flow test — login, silent refresh, logout, protected route redirect (mocked API) |
| ⬜ | Smoke-level render test per screen |

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
