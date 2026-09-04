# US-AC-001 — Configuration foundation

This document explains **every file added or changed** for the animal-configuration foundation story: what it does, why it exists, and how to test it.

There are **no administration screens** in this story. The API now has a secure, consistent base so later stories can add breeds, statuses, and groups without inventing new conventions.

Related: [implementation-guide.md](./implementation-guide.md), [authentication.md](./authentication.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Migrations | **Flyway** (not Liquibase). SQL lives in `apps/api/src/main/resources/db/migration`. |
| Audit columns | Reusable JPA mapped superclass: `createdAt`, `createdBy`, `updatedAt`, `updatedBy`. |
| Auditor | Spring Data JPA auditing. `createdBy` / `updatedBy` come from the JWT **subject**. |
| Permissions | `ANIMAL_CONFIG_VIEW`, `BREED_MANAGE`, `STATUS_CONFIG_MANAGE`, `GROUP_VIEW`, `GROUP_MANAGE`. |
| Method security | `@EnableMethodSecurity` + `@PreAuthorize("hasAuthority('…')")`. |
| Errors | One JSON envelope: `code`, `message`, `fieldErrors`, `timestamp`, `path`. |
| Language | `Accept-Language`. Supported: `ar`, `en`. **Default and fallback: Arabic**. |
| OpenAPI | Existing Springdoc path; `ApiError` schema documented. |
| UI | None. |

Standard error body:

```json
{
  "code": "BREED_CODE_ALREADY_EXISTS",
  "message": "A breed with this code already exists.",
  "fieldErrors": [
    { "field": "code", "message": "Code must be unique." }
  ],
  "timestamp": "2026-09-04T12:00:00Z",
  "path": "/api/v1/admin/animal-breeds"
}
```

`X-Correlation-Id` stays on the **HTTP header** (tracing). It is **not** inside this JSON body.

---

## 2. How the pieces connect

```
Browser / test
    │  Authorization: Bearer <Keycloak access token>
    │  Accept-Language: ar | en
    ▼
SecurityFilterChain  ── no token ──► 401 UNAUTHORIZED (JsonAuthHandlers)
    │
    │  JWT valid
    ▼
JwtAuthoritiesConverter
    realm_access.roles  →  ROLE_<role>
                        →  Permission set (RolePermissionMapper)
    permissions claim   →  extra Permission authorities
    ▼
@PreAuthorize on /api/v1/admin/**
    missing permission ──► 403 FORBIDDEN
    ▼
Controller / JPA
    validation fail    ──► 400 VALIDATION_ERROR
    ConflictException  ──► 409 + specific code
    not found          ──► 404 NOT_FOUND
    save entity        ──► AuditedEntity fills created*/updated* from JWT subject
```

---

## 3. File-by-file

Paths are relative to the repo root unless noted.

### 3.1 Build and environment

#### `apps/api/pom.xml` *(changed)*

**Why:** The API had web + JWT security only. This story needs a database, schema migrations, and an in-memory database for tests.

**What changed:** four dependencies:

| Dependency | Scope | Why |
|---|---|---|
| `spring-boot-starter-data-jpa` | compile | Entities, repositories, auditing |
| `flyway-core` | compile | Versioned SQL migrations |
| `postgresql` | runtime | Real local/prod database |
| `h2` | test | Isolated tests without Docker |

**How to check:** `mvn -f apps/api/pom.xml test` must resolve these artifacts.

#### `.env.example` *(changed)*

**Why:** Datasource settings must not be hardcoded in Java. Local defaults match `docker-compose.yml` (`herdcommand` / `herdcommand` / database `herdcommand`).

**What changed:** added `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`.

**How to use:** copy to `.env` or export in the shell before `mvn spring-boot:run`. Do not commit real passwords.

#### `apps/api/src/main/resources/application.yml` *(changed)*

**Why:** Spring Boot needs to know where Postgres is, that Flyway owns the schema, that Hibernate must **validate** (not create) tables, that Arabic is the message fallback, and that JSON dates stay ISO-8601.

**Important keys:**

| Key | Meaning |
|---|---|
| `spring.datasource.*` | Local default: **same Postgres server as Keycloak** (`localhost:5432`, user `postgres`), **different database** `herdcommand` (Keycloak keeps `keyclockDB`). Override with `DATABASE_*` in other environments. |
| `spring.jpa.hibernate.ddl-auto: validate` | Hibernate checks entities against Flyway tables; it does not invent schema |
| `spring.jpa.open-in-view: false` | Avoid lazy-loading after the HTTP request has left the service |
| `spring.flyway.baseline-on-migrate: true` | An **existing** database without Flyway history can still be adopted |
| `spring.messages.fallback-to-system-locale: false` | Missing locale does not silently become Windows English |
| `spring.jackson.serialization.write-dates-as-timestamps: false` | `timestamp` is an ISO string, not epoch millis |

**How to test:** start Postgres, run the API, then check logs for `Flyway` applying `V1` and Hibernate starting without `ddl-auto` create.

#### `apps/api/src/test/resources/application-test.yml` *(changed)*

**Why:** CI and `mvn test` must not need Postgres or Keycloak.

**What it does:**

- In-memory H2 in PostgreSQL compatibility mode
- Flyway still runs against H2 (proves the SQL is portable enough for this baseline)
- Excludes OAuth2 resource-server auto-config so tests inject a fake `JwtDecoder`

**How to test:** `mvn -f apps/api/pom.xml test` with nothing else running.

---

### 3.2 Database

#### `apps/api/src/main/resources/db/migration/V1__configuration_foundation.sql` *(new)*

**Why:** Flyway requires a first version. Later breed/status/group tables will be `V2`, `V3`, … and must reuse the audit column names documented here.

**What it does:** creates `herdcommand_schema_info` and inserts one row (`configuration-foundation`). That is a bootstrap marker, not farm or user data.

**Convention for later tables:**

```text
id            UUID PK
created_at    TIMESTAMP WITH TIME ZONE NOT NULL
created_by    VARCHAR(64) NOT NULL   -- JWT subject
updated_at    TIMESTAMP WITH TIME ZONE NOT NULL
updated_by    VARCHAR(64) NOT NULL
```

**How to test (empty DB):**

Same Postgres **server** as Keycloak (`localhost:5432`, user `postgres` / password `admin`). Different **database**: `herdcommand` (Keycloak stays on `keyclockDB`). Create it once:

```sql
CREATE DATABASE herdcommand;
```

Then:

```powershell
mvn -f apps/api/pom.xml spring-boot:run "-Daether.connector.https.securityMode=insecure"
```

Then in `psql` or any SQL client:

```sql
SELECT * FROM flyway_schema_history;
SELECT * FROM herdcommand_schema_info;
```

You should see version `1` and module `configuration-foundation`.

**How to test (existing DB):** `baseline-on-migrate: true` lets Flyway record V1 on a database that already has objects. After migrate, `flyway_schema_history` must contain the applied version.

---

### 3.3 Localization

#### `apps/api/src/main/resources/messages.properties` *(new)*

**Why:** Default bundle. Spring uses this when the locale has no more specific file, and as the last fallback. Arabic lives here so an unknown `Accept-Language` (for example `fr`) still returns Arabic.

#### `apps/api/src/main/resources/messages_ar.properties` *(new)*

**Why:** Explicit Arabic bundle when `Accept-Language: ar`. Same keys as the default file.

#### `apps/api/src/main/resources/messages_en.properties` *(new)*

**Why:** English copy when `Accept-Language: en`.

Keys used today:

| Key | Used for |
|---|---|
| `error.unauthorized` | HTTP 401 |
| `error.forbidden` | HTTP 403 |
| `error.validation` | HTTP 400 envelope `message` |
| `error.not_found` | HTTP 404 |
| `error.conflict` | Generic conflict |
| `error.internal` | HTTP 500 |
| `error.breed.codeExists` | Example conflict code `BREED_CODE_ALREADY_EXISTS` |
| `validation.notBlank` | Field-level Bean Validation |
| `validation.codeUnique` | Ready for the breed uniqueness story |

**How to test:** see [section 5](#5-how-to-test) (locale curls and `MessageResolutionTest`).

#### `apps/api/src/main/java/com/herdcommand/api/config/I18nConfig.java` *(new)*

**Why:** Wire `Accept-Language` into Spring MVC and Bean Validation.

**What it does:**

- `AcceptHeaderLocaleResolver` with default **`ar`** and supported locales **`ar`**, **`en`**
- UTF-8 `MessageSource` from `classpath:messages`, `fallbackToSystemLocale=false`
- `LocalValidatorFactoryBean` so `{validation.notBlank}` in annotations resolves through the same files

**How to test:** send the same 401 request with `Accept-Language: en`, `ar`, and `fr`. `fr` must match Arabic.

---

### 3.4 Audit / JPA

#### `apps/api/src/main/java/com/herdcommand/api/domain/audit/AuditedEntity.java` *(new)*

**Why:** Every future configuration entity (breed, status, group) must share the same four audit fields. A `@MappedSuperclass` avoids copy-paste.

**What it does:**

- UUID primary key assigned on persist
- `@CreatedDate` / `@LastModifiedDate` → `Instant` (UTC)
- `@CreatedBy` / `@LastModifiedBy` → `String` (JWT subject)
- `@EntityListeners(AuditingEntityListener.class)` so Spring Data fills the fields

This class is **not** a table by itself. Concrete `@Entity` classes extend it.

**How to test:** `JpaAuditingTest` (see test files below). There is no production table yet besides the Flyway marker.

#### `apps/api/src/main/java/com/herdcommand/api/config/JpaAuditingConfig.java` *(new)*

**Why:** Auditing is off until `@EnableJpaAuditing`. The auditor must be the authenticated user, not a hardcoded name.

**What it does:** `AuditorAware<String>` reads `SecurityContext`:

1. If principal is a `Jwt`, use `jwt.getSubject()`
2. Else use `authentication.getName()` unless it is blank or `anonymousUser`

No farm IDs and no demo usernames are stored in this class.

**How to test:** `JpaAuditingTest` authenticates as JWT subject `auditor-1` and asserts `createdBy` / `updatedBy`.

---

### 3.5 Permissions and JWT

#### `apps/api/src/main/java/com/herdcommand/api/security/Permission.java` *(new)*

**Why:** Single enum so method security, OpenAPI, and later services use the same names as the story.

Values: `ANIMAL_CONFIG_VIEW`, `BREED_MANAGE`, `STATUS_CONFIG_MANAGE`, `GROUP_VIEW`, `GROUP_MANAGE`.

#### `apps/api/src/main/java/com/herdcommand/api/security/RolePermissionMapper.java` *(new)*

**Why:** Keycloak today issues **realm roles** (`owner`, `manager`, …), not the five permission strings. The API must map roles → permissions so `@PreAuthorize` can work without waiting for a Keycloak permission redesign.

| Realm role | Permissions granted |
|---|---|
| `owner`, `administrator`, `manager` | All five |
| `veterinarian`, `worker` | `ANIMAL_CONFIG_VIEW`, `GROUP_VIEW` |
| `accountant` | `ANIMAL_CONFIG_VIEW` |

Unknown roles grant nothing from this matrix.

**How to test:** `JwtAuthoritiesConverterTest.managerRoleReceivesConfigurationPermissions`.

#### `apps/api/src/main/java/com/herdcommand/api/config/JwtAuthoritiesConverter.java` *(new)*

**Why:** Spring Security needs `GrantedAuthority` values that match `hasAuthority('ANIMAL_CONFIG_VIEW')`. The old converter only added `ROLE_<realmRole>` and OAuth scopes.

**What it adds to each JWT:**

1. OAuth2 scopes as `SCOPE_*` (unchanged Spring behaviour)
2. Each `realm_access.roles` entry as `ROLE_<name>`
3. Mapped `Permission` authorities from those roles
4. Any JWT `permissions` claim whose value matches a `Permission` enum constant
5. Any realm role that is already named like a permission (so Keycloak can assign `BREED_MANAGE` directly later)

**How to test:** `JwtAuthoritiesConverterTest` (no HTTP server). Live check: call `GET /api/v1/me` with a manager token, then `GET /api/v1/admin/animal-config` with the same token (should be 200).

#### `apps/api/src/main/java/com/herdcommand/api/config/SecurityConfig.java` *(changed)*

**Why:** Method security was not enabled. Admin routes must be authenticated at the HTTP layer, then permission-checked on the method.

**What changed:**

- `@EnableMethodSecurity`
- Inject `JwtAuthoritiesConverter` into `JwtAuthenticationConverter`
- Explicit `.requestMatchers("/api/v1/admin/**").authenticated()`
- JSON 401/403 handlers unchanged in role (still `JsonAuthHandlers`)

**How to test:** `AnimalConfigSecurityTest` (401 without token, 403 with a token that has no permission, 200 with `ANIMAL_CONFIG_VIEW`).

#### `apps/api/src/main/java/com/herdcommand/api/config/JsonAuthHandlers.java` *(changed)*

**Why:** Filter-chain 401/403 never reach `GlobalExceptionHandler`. They must still use the **same** error JSON and the same language files.

**What changed:** messages come from `MessageSource` + `LocaleResolver.resolveLocale(request)` (reads `Accept-Language`). Codes are `UNAUTHORIZED` / `FORBIDDEN`. `X-Correlation-Id` is still set on the response (security often runs before `CorrelationIdFilter`).

**How to test:** unauthenticated `GET /api/v1/admin/animal-config` with `Accept-Language: en` vs `ar`.

---

### 3.6 Standard errors

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ApiError.java` *(changed)*

**Why:** The story defined one envelope. The old shape used `errors` + `correlationId` in the body and lowercase codes.

**What it is now:** `code`, `message`, `fieldErrors[]` (`field` + `message`), `timestamp`, `path`. OpenAPI `@Schema` examples match the story.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ErrorCodes.java` *(new)*

**Why:** Avoid string typos (`UNAUTHORIZED` vs `unauthorized`) in handlers and tests.

Notable codes: `UNAUTHORIZED`, `FORBIDDEN`, `VALIDATION_ERROR`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`, `BREED_CODE_ALREADY_EXISTS`.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ApiException.java` *(new)*

**Why:** Business exceptions need a stable `code`, HTTP status, and **message key** (not a raw English string), so Arabic/English resolution stays in one place.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ResourceNotFoundException.java` *(new)*

**Why:** Later stories throw this instead of returning empty 200s. Maps to `NOT_FOUND` / `error.not_found`.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ConflictException.java` *(new)*

**Why:** Duplicate breed codes (next story) need `409` and a specific `code` such as `BREED_CODE_ALREADY_EXISTS`.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/GlobalExceptionHandler.java` *(changed)*

**Why:** Validation, not-found, conflict, and forbidden must serialize the same way. Messages must follow `Accept-Language`.

**Maps:**

| Exception | HTTP | `code` |
|---|---|---|
| `MethodArgumentNotValidException`, `ConstraintViolationException`, unreadable body | 400 | `VALIDATION_ERROR` |
| `ApiException` (including conflict / not-found subclasses) | from exception | from exception |
| `AuthenticationException` | 401 | `UNAUTHORIZED` |
| `AccessDeniedException` | 403 | `FORBIDDEN` |
| Spring “no handler / no resource” | 404 | `NOT_FOUND` |
| anything else | 500 | `INTERNAL_ERROR` (logged) |

Field errors use Bean Validation messages (localized). Envelope `message` uses `error.validation` (localized).

**How to test:** `ValidationErrorSerializationTest`.

---

### 3.7 Admin foundation API (no UI)

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/animalconfig/AnimalConfigController.java` *(new)*

**Why:** Acceptance criteria need a real administration URL: unauthenticated → 401, missing permission → 403. This is **not** a CRUD screen.

**Endpoint:** `GET /api/v1/admin/animal-config`  
**Permission:** `ANIMAL_CONFIG_VIEW`  
**Body:** default locale `ar`, supported locales, and the five permission names (metadata only).

OpenAPI documents 200 / 401 / 403 with `ApiError`.

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/animalconfig/AnimalConfigFoundationResponse.java` *(new)*

**Why:** Typed JSON for the GET above. No farm IDs.

#### `apps/api/src/main/java/com/herdcommand/api/config/OpenApiConfig.java` *(changed)*

**Why:** Story asked to document the error format if Springdoc is already in the project.

**What changed:** component schema `ApiError` with the example `BREED_CODE_ALREADY_EXISTS` payload.

**How to test:** with the API running, open `http://localhost:8080/swagger-ui.html` or `http://localhost:8080/api/v1/openapi`.

---

### 3.8 Tests (and test-only helpers)

These files are **not** production API. Probe routes exist only when `spring.profiles.active=test`.

#### `apps/api/src/test/java/com/herdcommand/api/support/TestJwtConfig.java` *(new)*

Fake `JwtDecoder` so `@SpringBootTest` does not call Keycloak.

#### `apps/api/src/test/java/com/herdcommand/api/support/JwtAuth.java` *(new)*

MockMvc helpers: authenticated JWT with **no** config permission, or with named permissions.

#### `apps/api/src/test/java/com/herdcommand/api/support/AnimalConfigProbeController.java` *(new)*

**Why:** This story does not implement breed POST, but it must prove 400 / 404 / 409 envelopes. The probe is `@Profile("test")` under `/api/v1/admin/animal-config/*-check`.

| Probe | Purpose |
|---|---|
| `POST .../validation-check` | Empty `code` → 400 + `fieldErrors` |
| `GET .../not-found-check` | Throws `ResourceNotFoundException` |
| `GET .../conflict-check` | Throws `ConflictException` with `BREED_CODE_ALREADY_EXISTS` |

#### `apps/api/src/test/java/com/herdcommand/auditprobe/JpaAuditProbe.java` *(new)*  
#### `apps/api/src/test/java/com/herdcommand/auditprobe/JpaAuditProbeRepository.java` *(new)*

**Why:** `AuditedEntity` is abstract. Tests need a tiny entity **outside** `com.herdcommand.api` so `@SpringBootTest` + `ddl-auto: validate` does not expect a production `jpa_audit_probe` table.

#### `apps/api/src/test/java/com/herdcommand/api/ApiContractTest.java` *(changed)*

Health still public. `/api/v1/me` 401 now expects `UNAUTHORIZED`, `fieldErrors`, `path`, `timestamp`, and header `X-Correlation-Id`. JWT `/me` still returns subject/roles.

#### `apps/api/src/test/java/com/herdcommand/api/admin/AnimalConfigSecurityTest.java` *(new)*

Story scenarios: 401, 403, 200 with `ANIMAL_CONFIG_VIEW`.

#### `apps/api/src/test/java/com/herdcommand/api/error/ValidationErrorSerializationTest.java` *(new)*

400 / 404 / 409 envelopes, English messages.

#### `apps/api/src/test/java/com/herdcommand/api/i18n/MessageResolutionTest.java` *(new)*

401 messages for `en`, `ar`, and `fr` → Arabic fallback.

#### `apps/api/src/test/java/com/herdcommand/api/persistence/FlywayMigrationTest.java` *(new)*

Asserts Flyway rank ≥ 1 and the foundation row exists on H2.

#### `apps/api/src/test/java/com/herdcommand/api/persistence/JpaAuditingTest.java` *(new)*

`@DataJpaTest` + create-drop: persist/update fills audit columns from JWT subject `auditor-1`.

#### `apps/api/src/test/java/com/herdcommand/api/security/JwtAuthoritiesConverterTest.java` *(new)*

Manager role → all five permissions; explicit `permissions` claim → only those listed.

---

### 3.9 Unchanged files (still part of the API)

These were **not** rewritten for US-AC-001; they still matter:

| File | Role |
|---|---|
| `HerdCommandApiApplication.java` | Boot entry; scans `com.herdcommand.api` |
| `MeController.java` / `MeResponse.java` | `GET /api/v1/me` |
| `CorrelationIdFilter.java` | Sets `X-Correlation-Id` when the filter chain reaches it |
| `PageResponse.java` | Pagination convention for later list APIs |

---

## 4. Permission cheat sheet for later stories

Use these on new admin methods:

```java
@PreAuthorize("hasAuthority('ANIMAL_CONFIG_VIEW')")
@PreAuthorize("hasAuthority('BREED_MANAGE')")
@PreAuthorize("hasAuthority('STATUS_CONFIG_MANAGE')")
@PreAuthorize("hasAuthority('GROUP_VIEW')")
@PreAuthorize("hasAuthority('GROUP_MANAGE')")
```

Throw `new ConflictException(ErrorCodes.BREED_CODE_ALREADY_EXISTS, "error.breed.codeExists")` for duplicate codes. Extend `AuditedEntity` for new tables. Add Flyway `V2__…sql` — do not turn on Hibernate `ddl-auto: update`.

---

## 5. How to test

### 5.1 Automated (required)

From the repo root:

```powershell
mvn -f apps/api/pom.xml test
```

If Maven fails with a TLS/`PKIX` error to `repo.maven.apache.org` (corporate SSL inspection), retry:

```powershell
mvn -f apps/api/pom.xml test "-Daether.connector.https.securityMode=insecure"
```

**What must pass:**

| Test class | Covers |
|---|---|
| `AnimalConfigSecurityTest` | 401 / 403 / 200 |
| `ValidationErrorSerializationTest` | 400 shape, 404, 409 |
| `JpaAuditingTest` | `created*` / `updated*` from JWT subject |
| `MessageResolutionTest` | `en` / `ar` / fallback |
| `FlywayMigrationTest` | V1 applied |
| `JwtAuthoritiesConverterTest` | Role → permission mapping |
| `ApiContractTest` | Health, `/me`, error record shape |

Run one class:

```powershell
mvn -f apps/api/pom.xml -Dtest=AnimalConfigSecurityTest test
```

### 5.2 Manual — API + Postgres

1. Keycloak’s Postgres must already be running (same server, port **5432**). Create the API database once if it does not exist:

```sql
CREATE DATABASE herdcommand;
```

2. Start the API:

```powershell
mvn -f apps/api/pom.xml spring-boot:run "-Daether.connector.https.securityMode=insecure"
```

3. Confirm health (no token):

```powershell
curl -s http://localhost:8080/actuator/health
```

Expect `{"status":"UP"}`.

4. **401** — administration without a token:

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-config -H "Accept-Language: en"
```

Expect HTTP 401, `"code":"UNAUTHORIZED"`, English `message`, `"path":"/api/v1/admin/animal-config"`, `"fieldErrors":[]`.

Repeat with `Accept-Language: ar` and confirm the Arabic unauthorized sentence. Repeat with `Accept-Language: fr` and confirm **Arabic** (fallback).

5. **403 vs 200** — need a real access token from Keycloak (user with realm role `manager` or `owner` for 200; a user with no mapped role for 403).

Sign in through the web app, copy the access token from the session, or use a password grant only if your realm allows it (this project’s web client is public + PKCE; prefer the app session).

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-config `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Accept-Language: ar"
```

- Token missing/invalid → 401  
- Token valid, role `accountant` → 200 (has `ANIMAL_CONFIG_VIEW`)  
- Token valid, **no** mapped role and no `permissions` claim → 403  
- Token valid, `manager` / `owner` / `administrator` → 200 and JSON listing the five permissions  

6. OpenAPI: `http://localhost:8080/swagger-ui.html`

7. Flyway on the real database:

```sql
SELECT version, description, success FROM flyway_schema_history;
```

### 5.3 What you cannot click-test yet

Probe URLs (`/validation-check`, `/not-found-check`, `/conflict-check`) are **test profile only**. They are not registered when you run `spring-boot:run` with the default profile. Use `mvn test` for those envelopes, or the 401/403/200 paths above against the live server.

There is still **no** breed create/update UI or `POST /api/v1/admin/animal-breeds` in production. That is the next configuration story.

---

## 6. Definition of done (checklist)

- [x] Flyway configured; V1 runs on empty H2 in tests; Postgres via `DATABASE_*` + `baseline-on-migrate` for existing DBs  
- [x] Reusable audit fields + auditor = JWT subject  
- [x] Five permissions + `@PreAuthorize`  
- [x] One error JSON documented in OpenAPI  
- [x] `Accept-Language` `ar` / `en`, Arabic fallback  
- [x] No admin screens  
- [x] No secrets, hardcoded users, or hardcoded farm IDs in source  
- [x] Automated tests for 401, 403, validation JSON, JPA auditing, and locale resolution  
