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
|---|---|---|---|---|
| 5.1 | DTOs + Commands | See 5.1 section | Layer 3 (ports defined) — COMPLETE | ⬜ |
| 5.2 | Thin Mappers (optional) | `DtoToCommandMapper.java`, `ResponseFromDomainMapper.java` | 5.1 (DTOs exist) | ⬜ |
| 5.3 | Global Exception Handler | `GlobalExceptionHandler.java` | Domain exceptions (Layer 1 — COMPLETE) | ⬜ |
| 5.4 | AuthController | `AuthController.java` | 5.1, 5.3, `AuthUseCase` port (Layer 3 — COMPLETE) | ⬜ |
| 5.5 | UserController | `UserController.java` | 5.1, 5.3, `UserManagementUseCase` port (Layer 3 — COMPLETE) | ⬜ |
| 5.6 | OpenAPI + SpringDoc | `pom.xml` + `@Tag`/`@Operation` on controllers | 5.4, 5.5 (controllers exist) | ⬜ |
| 5.7 | AdminUserInitializer | `AdminUserInitializer.java` | `UserRepository`, `PasswordHasher` (Layer 3 — COMPLETE). Parallel with 5.1. | ⬜ |

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
|---|---|
| ⬜ | `AdminUserInitializer.java` — `ApplicationRunner`; checks if any ADMIN exists via `UserRepository`; if not, reads `INITIAL_ADMIN_PASSWORD` from env — fail fast with `IllegalStateException` if absent; BCrypt-hashes and inserts via `UserRepository` |

### Layer 7 — Tests

> **NOTE:** Several tests in this layer overlap with Layer 5's test matrix or are already complete:
> `JwtServiceTest` ✅ and `RedisTokenBlacklistAdapterTest` ✅ — completed in commit `58ce7a0`.
> `AuthControllerTest`, `UserManagementServiceTest`, and `AdminUserInitializerTest` — covered by Layer 5 (components 5.4, 5.5, 5.7).
> Only `RefreshTokenJpaAdapterTest` remains as a standalone pending item in this layer.
> Update this layer after Layer 5 is complete and all duplicates are confirmed green.

> ⛔ Cannot begin until Layer 4.6 (`SecurityConfig`) is COMPLETE.

| Status | Task |
|---|---|
| ⬜ | `AuthControllerTest` — `@SpringBootTest` + `MockMvc`: correct login, wrong credentials, expired access token, logout then immediate reuse of blacklisted token returns 401 |
| ⬜ | `JwtServiceTest` — token generation and validation; no Spring context |
| ⬜ | `RefreshTokenJpaAdapterTest` — DB round-trip; `@DataJpaTest` |
| ⬜ | `RedisTokenBlacklistAdapterTest` — blacklist write and TTL expiry; Testcontainers `redis:7-alpine` |
| ⬜ | `UserManagementServiceTest` — create, deactivate, list with roles; Mockito, no Spring context |
| ⬜ | `AdminUserInitializerTest` — ADMIN created when none exists; no duplicate on second run; `IllegalStateException` thrown when `INITIAL_ADMIN_PASSWORD` not set |

---

## Phase 4 — SOAP Adapter ⚠️ Highest Risk
**Tag:** `v0.4.0`
**Exit condition:** `Protocolo.isProcessed() == true` confirmed against live QSolutions endpoint. Paste actual server response in GitHub release notes as auditable proof. Do not proceed to Phase 5 without this.

| Status | Task |
|---|---|
| ⬜ | Run `mvn generate-sources` — regenerate stubs with `jakarta.*` from local WSDL |
| ⬜ | Verify all 18 stub classes compile under Java 21 with `jakarta.*` namespace |
| ⬜ | `QSolutionsSoapAdapter.java` implements `PulseSender` |
| ⬜ | Credentials injected via `@Value` from environment variables — never hardcoded |
| ⬜ | `XMLGregorianCalendar` timestamps use `FLEET_TIMEZONE` explicitly |
| ⬜ | Send real test pulse to live QSolutions endpoint |
| ⬜ | Confirm `Protocolo.isProcessed() == true` — log and save response |
| ⬜ | **GATE: Do not proceed to Phase 5 until confirmed** |

---

## Phase 5 — Core Dispatch 🏁 Production Replacement Milestone
**Tag:** `v1.0.0`
**Exit condition:** 5 units receive pulses on the 15-minute cycle in staging. Skip conditions log correctly. Force dispatch works. JavaFX desktop app can be shut down.

| Status | Task |
|---|---|
| ⬜ | `GpsProviderConfig.java` — selects active `GpsCoordinateProvider` (default: MANUAL) |
| ⬜ | `SchedulerConfig.java` — configures global `@Scheduled` tick |
| ⬜ | `ManualCoordinateAdapter.java` implements `GpsCoordinateProvider` |
| ⬜ | `PulseOrchestrationService.java` — assembles `ScheduledPulse`, resolves `effectiveTrackingNumber` |
| ⬜ | Global `@Scheduled` tick every 15 minutes — reads live DB state on each tick |
| ⬜ | Skip conditions logged: `SKIPPED_OUT_OF_WINDOW`, `SKIPPED_NO_COORDINATES` |
| ⬜ | `POST /api/units/{numUnidad}/pulse/force` — real SOAP dispatch, maps to `SendPulseUseCase` |
| ⬜ | Smoke test — confirm all 5 units dispatch correctly on the 15-minute cycle in staging |
| ⬜ | **MILESTONE: Shut down JavaFX desktop app. fleet-pulse-api is production.** |

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
