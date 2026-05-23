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
**Exit condition:** ADMIN and USER roles enforce the authorization matrix. No endpoint reachable without valid token except `/api/auth/*`. Login returns access token + refresh token. Logout blacklists access token in Redis AND marks refresh token as `revoked = true` in DB. First ADMIN provisioned via `AdminUserInitializer` on startup. All tests pass.

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

### Layer 4 — Infrastructure / Security

| Status | Task |
|---|---|
| ⬜ | `JwtService.java` in `infrastructure/security/` — implements `TokenService` port; HS256 signing via JJWT; key from `JWT_SECRET` env var |
| ⬜ | `JwtAuthenticationFilter.java` — `OncePerRequestFilter`; extracts Bearer token, checks blacklist, sets `SecurityContextHolder` |
| ⬜ | `UserDetailsServiceImpl.java` — implements Spring `UserDetailsService`; loads user via `UserRepository` port |
| ⬜ | `SecurityConfig.java` — full authorization matrix; `hasAuthority()` (no `ROLE_` prefix); stateless session; `/api/auth/**` and `/api/gps/position` are the only public paths |
| ⬜ | `BCryptPasswordEncoder` bean declared in `SecurityConfig` |
| ⬜ | `RefreshTokenJpaAdapter.java` + `RefreshTokenEntity.java` — implements `RefreshTokenRepository`; maps between domain `RefreshToken` and `RefreshTokenEntity` |
| ⬜ | `RedisTokenBlacklistAdapter.java` — implements `TokenBlacklist`; TTL set to `remainingTtl` argument (not fixed value); fail closed on Redis unavailable |

### Layer 5 — Controllers + DTOs

| Status | Task |
|---|---|
| ⬜ | `AuthController.java` — `POST /api/auth/login`, `POST /api/auth/refresh`, `POST /api/auth/logout` |
| ⬜ | `UserController.java` — ADMIN-only: `GET /api/users`, `POST /api/users`, `PUT /api/users/{id}`, `DELETE /api/users/{id}` |
| ⬜ | DTOs: `LoginRequest`, `LoginResponse`, `RefreshRequest`, `RefreshResponse`, `CreateUserRequest`, `UserResponse` |
| ⬜ | Global `@ControllerAdvice` — RFC 7807 `application/problem+json` error responses for all 4xx/5xx; no Spring whitelabel error page |

### Layer 6 — First ADMIN

| Status | Task |
|---|---|
| ⬜ | `AdminUserInitializer.java` — `ApplicationRunner`; checks if any ADMIN exists via `UserRepository`; if not, reads `INITIAL_ADMIN_PASSWORD` from env — fail fast with `IllegalStateException` if absent; BCrypt-hashes and inserts via `UserRepository` |

### Layer 7 — Tests

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
