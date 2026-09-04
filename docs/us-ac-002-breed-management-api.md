# US-AC-002 — Breed management API

This document explains **every file added or changed** for the animal-breed administration story: what it does, why it exists, and how to test it.

There are **no React administration screens** in this story. There is **no DELETE**. Inactive breeds stay in the database and remain visible; later animal-assignment code must refuse to attach an inactive breed.

Related: [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md) (Flyway, audit, permissions, `ApiError`, i18n), [implementation-guide.md](./implementation-guide.md), [authentication.md](./authentication.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Table | Flyway **`V2__animal_breed.sql`**. Hibernate still `ddl-auto: validate`. |
| Identity | `code` is unique, stored **uppercase**, and **immutable** after create. |
| Species | Temporary Java enum `SpeciesCode` (`SHEEP`, `GOAT`, `CATTLE`, `CAMEL`). Not a table yet. |
| Soft status | `active` flag + `PATCH …/status`. No hard delete. |
| List | Paginated search on code / Arabic name / English name, optional species and active filters. |
| Permissions | `ANIMAL_CONFIG_VIEW` for reads; `BREED_MANAGE` for create / update / status. |
| Assignment | `AnimalBreedAssignmentGuard` throws `409 BREED_INACTIVE` if a later story tries to assign an inactive breed. |
| UI | None. |

### API

Base path: `/api/v1/admin/animal-breeds`. All routes need a Bearer JWT. There is no `DELETE`.

| Method | Path | Permission | Success | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/animal-breeds` | `ANIMAL_CONFIG_VIEW` | 200 `PageResponse<BreedResponse>` | Query: `search`, `speciesCode`, `active`, plus `page` / `size` / `sort`. Default size 20, sort `displayOrder,asc`. |
| `GET` | `/api/v1/admin/animal-breeds/{breedId}` | `ANIMAL_CONFIG_VIEW` | 200 `BreedResponse` | Unknown id → 404 `NOT_FOUND`. |
| `POST` | `/api/v1/admin/animal-breeds` | `BREED_MANAGE` | 201 `BreedResponse` | Code is trimmed and uppercased before validate/save. |
| `PUT` | `/api/v1/admin/animal-breeds/{breedId}` | `BREED_MANAGE` | 200 `BreedResponse` | Names, species, and display order only. Changing `code` → 400. |
| `PATCH` | `/api/v1/admin/animal-breeds/{breedId}/status` | `BREED_MANAGE` | 200 `BreedResponse` | Body `{ "active": true\|false }`. Labels stay. |

Roles that already map to these permissions (from US-AC-001 `RolePermissionMapper`):

| Realm role | Can list/get breeds | Can create/update/status |
|---|---|---|
| `owner`, `administrator`, `manager` | yes (`ANIMAL_CONFIG_VIEW`) | yes (`BREED_MANAGE`) |
| `veterinarian`, `worker`, `accountant` | yes | no (403 on POST/PUT/PATCH) |

### Error codes this story uses

Same envelope as US-AC-001 (`code`, `message`, `fieldErrors`, `timestamp`, `path`).

| HTTP | `code` | When |
|---|---|---|
| 401 | `UNAUTHORIZED` | No / invalid token |
| 403 | `FORBIDDEN` | Token valid but missing the permission above |
| 400 | `VALIDATION_ERROR` | Bean Validation (blank names, bad pattern, `displayOrder < 0`, unknown species JSON, …) **or** PUT that tries to change `code` |
| 404 | `NOT_FOUND` | Breed id does not exist |
| 409 | `BREED_CODE_ALREADY_EXISTS` | Create with a code that already exists (case-insensitive) |
| 409 | `BREED_INACTIVE` | Guard only — not an HTTP route in this story. Future assignment calls `requireAssignable`. |

PUT that sends a **different** code (after trim + uppercase) returns 400, not 409:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "The request contains invalid fields.",
  "fieldErrors": [
    { "field": "code", "message": "Breed code cannot be changed after creation." }
  ],
  "timestamp": "2026-09-04T12:00:00Z",
  "path": "/api/v1/admin/animal-breeds/<id>"
}
```

Duplicate create (`Accept-Language: en`):

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

`X-Correlation-Id` stays on the **HTTP header**. It is **not** inside this JSON body.

### Code normalization

`CreateBreedRequest` compact constructor (also used by `UpdateBreedRequest` for `code`):

1. `trim()`
2. `toUpperCase(Locale.ROOT)`

So `"  awassi_01  "` is stored and returned as `"AWASSI_01"`. The uniqueness check is `existsByCodeIgnoreCase`, so `"awassi_01"` and `"AWASSI_01"` collide.

Allowed pattern after normalization: `^[A-Z0-9][A-Z0-9_-]{0,39}$` (max 40 characters, first character letter or digit).

---

## 2. How the pieces connect

```
Browser / curl / test
    │  Authorization: Bearer <Keycloak access token>
    │  Accept-Language: ar | en
    ▼
SecurityFilterChain  ── no token ──► 401 UNAUTHORIZED
    │
    │  JWT valid
    ▼
JwtAuthoritiesConverter  →  ROLE_*  +  Permission authorities
    ▼
AnimalBreedController
    GET              needs ANIMAL_CONFIG_VIEW
    POST/PUT/PATCH   needs BREED_MANAGE
         missing ──► 403 FORBIDDEN
    ▼
Create/Update DTO compact constructor
    code = trim + uppercase
    names trimmed; create displayOrder defaults to 0
    ▼
Bean Validation  ── fail ──► 400 VALIDATION_ERROR + fieldErrors
    ▼
AnimalBreedService
    POST duplicate code     ──► 409 BREED_CODE_ALREADY_EXISTS
    PUT code changed        ──► 400 VALIDATION_ERROR (field code)
    missing id              ──► 404 NOT_FOUND
    save AnimalBreed        ──► AuditedEntity fills created*/updated* from JWT subject
    ▼
Postgres animal_breed
    UNIQUE (code)           ──► if two creates race, DataIntegrityViolationException
                                 → 409 BREED_CODE_ALREADY_EXISTS

Later animal stories (not HTTP yet)
    AnimalBreedAssignmentGuard.requireAssignable(id)
        missing  ──► 404
        inactive ──► 409 BREED_INACTIVE
```

---

## 3. File-by-file

Paths are relative to the repo root unless noted.

### 3.1 Database

#### `apps/api/src/main/resources/db/migration/V2__animal_breed.sql` *(new)*

**Why:** US-AC-001 reserved Flyway for schema. Breed columns must exist before Hibernate validates `AnimalBreed`.

**What it creates:**

| Column | Type | Notes |
|---|---|---|
| `id` | `UUID` PK | Assigned in `AuditedEntity` on persist |
| `code` | `VARCHAR(40)` NOT NULL | Unique (`ux_animal_breed_code`) |
| `name_ar` / `name_en` | `VARCHAR(100)` NOT NULL | Both languages stored; UI is later |
| `species_code` | `VARCHAR(30)` NOT NULL | Enum string until a species table exists |
| `display_order` | `INTEGER` NOT NULL DEFAULT 0 | `CHECK (display_order >= 0)` |
| `active` | `BOOLEAN` NOT NULL DEFAULT TRUE | Soft deactivate |
| `created_at` / `created_by` / `updated_at` / `updated_by` | same as V1 convention | JWT subject in `*_by` |

Index `ix_animal_breed_species_active_order` on `(species_code, active, display_order)` supports the default list sort and filters.

**How to test (empty or existing `herdcommand` DB):**

Same Postgres **server** as Keycloak (`localhost:5432`, user `postgres` / password `admin`). Different **database**: `herdcommand` (Keycloak stays on `keyclockDB`). Create it once if needed:

```sql
CREATE DATABASE herdcommand;
```

Start the API (Maven TLS workaround if corporate SSL inspection breaks `repo.maven.apache.org`):

```powershell
mvn -f apps/api/pom.xml spring-boot:run "-Daether.connector.https.securityMode=insecure"
```

Then:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT code, name_en, species_code, active FROM animal_breed;
```

You should see versions `1` and `2`. `animal_breed` starts empty (this story does not seed breeds).

**Flyway repair if a failed V2 was applied:**

If an earlier start created the V2 history row with `success = false` (or left a half-created table), Spring Boot will refuse to migrate. Inspect, drop leftovers, remove the failed row, restart:

```sql
SELECT installed_rank, version, description, success, checksum
FROM flyway_schema_history
ORDER BY installed_rank;

DROP TABLE IF EXISTS animal_breed;
DELETE FROM flyway_schema_history WHERE version = '2' AND success = false;
```

Then start the API again so Flyway can apply `V2__animal_breed.sql` cleanly. Do **not** edit V2 after it has succeeded on a shared database; change the checksum only as a last resort on a local DB you are willing to rebuild.

There is no Flyway Maven plugin in `apps/api/pom.xml`. Repair is SQL (or a Flyway CLI), not `mvn flyway:repair`.

---

### 3.2 Domain

#### `apps/api/src/main/java/com/herdcommand/api/domain/breed/SpeciesCode.java` *(new)*

**Why:** Species is not its own configuration table yet. A closed enum keeps JSON and the column consistent (`SHEEP`, `GOAT`, `CATTLE`, `CAMEL`).

**What it is:** four constants, stored with `@Enumerated(EnumType.STRING)`. Unknown JSON values fail Jackson → 400 `VALIDATION_ERROR` (unreadable / invalid body).

Replace this enum with a species entity in a later story; do not treat the four values as a permanent product catalog.

#### `apps/api/src/main/java/com/herdcommand/api/domain/breed/AnimalBreed.java` *(new)*

**Why:** First real configuration entity. Extends `AuditedEntity` so id + four audit columns are not copied.

**What it does:**

- Table `animal_breed`
- `code` is `updatable = false` at JPA as a second lock (the service also rejects code changes)
- Default `active = true` on the all-args constructor
- No setter for `code`

**How to test:** create via POST and assert audit fields; `JpaAuditingTest` still covers the superclass.

#### `apps/api/src/main/java/com/herdcommand/api/domain/breed/AnimalBreedRepository.java` *(new)*

**Why:** List needs filters that are not `findAll`. Uniqueness must be checked before insert.

**What it does:**

- `existsByCodeIgnoreCase(String code)` — create-time duplicate
- `search(search, speciesCode, active, pageable)` — JPQL: optional species, optional active, and case-insensitive `LIKE` on `code`, `nameAr`, `nameEn`. Empty / null `search` means “no text filter”.

**How to test:** `AnimalBreedApiTest.searchFiltersBySpeciesAndText`.

#### `apps/api/src/main/java/com/herdcommand/api/domain/breed/AnimalBreedService.java` *(new)*

**Why:** Controllers stay thin. Normalization, uniqueness, immutability, and mapping to `BreedResponse` live here.

**What it does:**

| Method | Behaviour |
|---|---|
| `search` | Trims the search term; wraps the Spring `Page` in `PageResponse` (`items`, `total`, `page`, `size`, `sort`) |
| `get` | 404 if missing |
| `create` | If `existsByCodeIgnoreCase` → `ConflictException(BREED_CODE_ALREADY_EXISTS)` with field error on `code`. New row is always active. |
| `update` | If request `code` is non-blank and not equal to the stored code → `ApiException` 400 `VALIDATION_ERROR` with `error.breed.codeImmutable`. Otherwise updates names, species, display order. |
| `updateStatus` | Sets `active` only |

Messages for field errors use `MessageSource` + `LocaleContextHolder` so `Accept-Language` applies.

**How to test:** `AnimalBreedApiTest` (create, duplicate, PUT reject, PUT success, PATCH, 404).

#### `apps/api/src/main/java/com/herdcommand/api/domain/breed/AnimalBreedAssignmentGuard.java` *(new)*

**Why:** Deactivating a breed must not delete history, but new animal assignments must not use an inactive breed. That rule belongs in one place before the animal table exists.

**What it does:** `requireAssignable(breedId)` loads the breed; missing → `ResourceNotFoundException`; `active == false` → `ConflictException(BREED_INACTIVE, "error.breed.inactive")`.

This class is **not** called from `AnimalBreedController`. PATCH may deactivate freely. Wire the guard when animal create/update is implemented.

**How to test:** `AnimalBreedAssignmentGuardTest`.

---

### 3.3 HTTP API (DTOs + controller)

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/breed/CreateBreedRequest.java` *(new)*

**Why:** Validate and normalize **before** the service hits the database.

**Fields:**

| Field | Rules |
|---|---|
| `code` | `@NotBlank`, `@Size(max=40)`, pattern `^[A-Z0-9][A-Z0-9_-]{0,39}$` after normalize |
| `nameAr` / `nameEn` | `@NotBlank`, `@Size(max=100)`, trimmed |
| `speciesCode` | `@NotNull` (`SpeciesCode`) |
| `displayOrder` | `@Min(0)`; `null` becomes `0` |

Compact constructor calls `normalizeCode` (trim + `Locale.ROOT` uppercase) and `trimToNull` (trim; empty stays `""` so `@NotBlank` still fires).

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/breed/UpdateBreedRequest.java` *(new)*

**Why:** PUT must send the editable fields. `code` is optional and **not** `@NotBlank` — clients may omit it or echo the existing code.

**What it does:** Reuses `CreateBreedRequest.normalizeCode` / `trimToNull`. `nameAr`, `nameEn`, `speciesCode`, and `displayOrder` are required (`displayOrder` has `@NotNull` + `@Min(0)`). If `code` is present and differs from the stored value, the **service** (not Bean Validation) returns 400.

Sending the same code in another case (`"nuaimi_x"` vs stored `"NUAIMI_X"`) is allowed and does not change the stored code.

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/breed/BreedStatusRequest.java` *(new)*

**Why:** Activate / deactivate without a full PUT.

**What it is:** `{ "active": boolean }` with `@NotNull`. Missing property → 400.

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/breed/BreedResponse.java` *(new)*

**Why:** One JSON shape for create, get, update, and status.

**Fields:** `id`, `code`, `nameAr`, `nameEn`, `speciesCode`, `displayOrder`, `active`, `createdAt`, `createdBy`, `updatedAt`, `updatedBy`. Factory: `BreedResponse.from(AnimalBreed)`.

#### `apps/api/src/main/java/com/herdcommand/api/api/admin/breed/AnimalBreedController.java` *(new)*

**Why:** Administration URL promised by the story. OpenAPI tag `Animal breeds`, security `bearer-jwt`.

**What it does:** Maps the five operations in the [API table](#api). `@PreAuthorize` on every method. `@PageableDefault(size = 20, sort = "displayOrder", direction = ASC)` on list. POST is `@ResponseStatus(CREATED)`.

No `@DeleteMapping`.

**How to test:** `AnimalBreedApiTest` and the live curls in [section 5](#5-how-to-test).

#### `apps/api/src/main/java/com/herdcommand/api/api/common/PageResponse.java` *(unchanged, now used)*

**Why:** US-AC-001 left this for the first list API.

**Shape:** `{ "items": [...], "total": n, "page": 0, "size": 20, "sort": "…" }`.

---

### 3.4 Errors and messages *(changed)*

These types existed in US-AC-001. This story uses them for real breed conflicts and adds one code plus a uniqueness race handler.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ErrorCodes.java` *(changed)*

**What changed:** `BREED_INACTIVE` (assignment guard). `BREED_CODE_ALREADY_EXISTS` was already reserved in US-AC-001; create now throws it for real.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ApiException.java` *(changed)*

**What changed:** optional `List<ApiError.FieldError>` so PUT-code-immutable and duplicate-code can attach `fieldErrors` while keeping a stable `code` + HTTP status + message key.

#### `apps/api/src/main/java/com/herdcommand/api/api/error/ConflictException.java` *(changed)*

**What changed:** constructor `(code, messageKey, fieldErrors)` used by `AnimalBreedService.duplicateCode()`. Two-arg constructor still used by the assignment guard (`BREED_INACTIVE` has no field errors).

#### `apps/api/src/main/java/com/herdcommand/api/api/error/GlobalExceptionHandler.java` *(changed)*

**What changed:** `DataIntegrityViolationException` → 409. If the cause message mentions `animal_breed` and `code`, the body is `BREED_CODE_ALREADY_EXISTS` + `validation.codeUnique` on field `code`. Any other constraint failure stays generic `CONFLICT`.

This covers two concurrent POSTs that both passed `existsByCodeIgnoreCase` before either insert committed.

`ApiException` handling (400 / 404 / 409 from the service) was already in US-AC-001.

#### `apps/api/src/main/resources/messages.properties` *(changed)*  
#### `apps/api/src/main/resources/messages_ar.properties` *(changed)*  
#### `apps/api/src/main/resources/messages_en.properties` *(changed)*

**Why:** Default / Arabic / English must stay in lockstep. Default and `messages_ar` are Arabic (unknown `Accept-Language` still Arabic).

Keys added or now used in production for this story:

| Key | Used for |
|---|---|
| `error.breed.codeExists` | 409 envelope `message` |
| `error.breed.codeImmutable` | PUT code-change field error |
| `error.breed.inactive` | Guard 409 envelope `message` |
| `validation.notBlank` / `validation.notNull` | Required fields |
| `validation.size` | Max length (`{max}`) |
| `validation.min` | `displayOrder` (`{value}`) |
| `validation.codeUnique` | 409 field error on `code` |
| `validation.breed.codePattern` | Code charset / shape |

**How to test:** `AnimalBreedApiTest` with `Accept-Language: en` for English sentences; repeat a 409 curl with `Accept-Language: ar`.

---

### 3.5 Tests

These files are **not** production API. They use profile `test` (H2 + fake `JwtDecoder` from US-AC-001 `TestJwtConfig` / `JwtAuth`). JWT subject in HTTP tests is `user-1` (appears in `createdBy` / `updatedBy`).

#### `apps/api/src/test/java/com/herdcommand/api/admin/AnimalBreedApiTest.java` *(new)*

End-to-end MockMvc against the real controller + H2.

| Test | Asserts |
|---|---|
| `listRequiresAuthentication` | GET list without token → 401 `UNAUTHORIZED` |
| `listRequiresViewPermission` | Authenticated, no permissions → 403 `FORBIDDEN` |
| `createRequiresManagePermission` | `ANIMAL_CONFIG_VIEW` only → 403 on POST |
| `createNormalizesCodeAndReturnsAuditFields` | `"  awassi_…  "` → `AWASSI_…`, 201, audit subject `user-1`, GET by id |
| `duplicateCodeReturnsConflict` | Second POST with different case → 409 `BREED_CODE_ALREADY_EXISTS` |
| `invalidCreateReturnsValidationEnvelope` | Empty names / blank code / `displayOrder: -1` → 400 + `fieldErrors` |
| `putRejectsCodeChangeAndLeavesStoredCode` | PUT new code → 400; GET still has original code and name |
| `putUpdatesEditableFieldsWhenCodeUnchanged` | Same code, different case → 200; names / species / order update |
| `patchDeactivatesBreedAndKeepsLabel` | `active: false`; Arabic name and code unchanged |
| `searchFiltersBySpeciesAndText` | `search` + `speciesCode=SHEEP` + `active=true` |
| `missingBreedReturnsNotFound` | Random UUID → 404 `NOT_FOUND` |

Uniqueness and permission coverage live in this class (there is no separate uniqueness test file).

#### `apps/api/src/test/java/com/herdcommand/api/domain/breed/AnimalBreedAssignmentGuardTest.java` *(new)*

Spring context + H2, authenticates as JWT subject `assigner-1` so auditing can persist.

| Test | Asserts |
|---|---|
| `activeBreedCanBeAssigned` | Returns the same id, `active` true |
| `inactiveBreedCannotBeNewlyAssigned` | `ConflictException` with `BREED_INACTIVE` |
| `missingBreedCannotBeAssigned` | `ResourceNotFoundException` |

#### `apps/api/src/test/java/com/herdcommand/api/persistence/FlywayMigrationTest.java` *(unchanged)*

Still asserts Flyway rank ≥ 1 and the V1 marker row. V2 is applied automatically on H2 because the file is on the Flyway classpath; `AnimalBreedApiTest` is what proves `animal_breed` is usable.

---

### 3.6 Unchanged files this story depends on

Not rewritten for US-AC-002; still required:

| File | Role |
|---|---|
| `AuditedEntity.java` / `JpaAuditingConfig.java` | UUID + `created*` / `updated*` from JWT subject |
| `Permission.java` / `RolePermissionMapper.java` / `JwtAuthoritiesConverter.java` | `ANIMAL_CONFIG_VIEW`, `BREED_MANAGE` |
| `SecurityConfig.java` | `@EnableMethodSecurity`, `/api/v1/admin/**` authenticated |
| `ResourceNotFoundException.java` | 404 from `get` / guard |
| `I18nConfig.java` + `application.yml` | `Accept-Language`, datasource `herdcommand` / `postgres` / `admin` |
| `apps/api/src/test/java/com/herdcommand/api/support/JwtAuth.java` | Test tokens with named permissions |
| `apps/web/**` | **No breed screens** |

---

## 4. What is intentionally not here

- **No DELETE** — deactivate with PATCH. Historical animals will keep the breed id.
- **No React / Next.js pages** — do not add `apps/web` admin UI in this story.
- **No species table** — `SpeciesCode` is temporary.
- **No seed data** — operators create breeds through the API (or later UI).
- **Guard is not an HTTP endpoint** — do not curl `/assign`. Call `AnimalBreedAssignmentGuard` from the future animal service.

---

## 5. How to test

### 5.1 Automated (required)

From the repo root:

```powershell
mvn -f apps/api/pom.xml test "-Daether.connector.https.securityMode=insecure"
```

If Maven can reach Central without corporate TLS interception, the `-Daether…` flag can be omitted. If you see `PKIX` / TLS errors on `repo.maven.apache.org`, keep the flag.

**What must pass for this story:**

| Test class | Covers |
|---|---|
| `AnimalBreedApiTest` | 401 / 403, normalize, 409 uniqueness, 400 validation, PUT code lock, PATCH, search, 404 |
| `AnimalBreedAssignmentGuardTest` | Active ok; inactive → `BREED_INACTIVE`; missing → 404 |

US-AC-001 tests (`AnimalConfigSecurityTest`, `FlywayMigrationTest`, …) should still pass; V2 runs on the same H2.

Run one class:

```powershell
mvn -f apps/api/pom.xml -Dtest=AnimalBreedApiTest test "-Daether.connector.https.securityMode=insecure"
```

```powershell
mvn -f apps/api/pom.xml -Dtest=AnimalBreedAssignmentGuardTest test "-Daether.connector.https.securityMode=insecure"
```

### 5.2 Manual — API + Postgres

1. Keycloak’s Postgres must already be running (same server, port **5432**). API database is **`herdcommand`**, not `keyclockDB`. User **`postgres`** / password **`admin`**. Create the database once if needed:

```sql
CREATE DATABASE herdcommand;
```

2. Start the API:

```powershell
mvn -f apps/api/pom.xml spring-boot:run "-Daether.connector.https.securityMode=insecure"
```

If startup fails on Flyway V2, use the [repair SQL](#flyway-repair-if-a-failed-v2-was-applied) above.

3. Health (no token):

```powershell
curl -s http://localhost:8080/actuator/health
```

Expect `{"status":"UP"}`.

4. **401** — list without a token:

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds -H "Accept-Language: en"
```

Expect HTTP 401, `"code":"UNAUTHORIZED"`.

5. Get a real access token from Keycloak (sign in on the web app, copy the access token). Need realm role `manager` / `owner` / `administrator` for writes. `accountant` can GET but not POST.

6. **201 + normalization:**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Accept-Language: en" `
  -H "Content-Type: application/json" `
  -d "{\"code\":\"  awassi  \",\"nameAr\":\"عواسي\",\"nameEn\":\"Awassi\",\"speciesCode\":\"SHEEP\"}"
```

Expect HTTP 201, `"code":"AWASSI"`, `"active":true`, `"displayOrder":0`, `createdBy` = JWT subject. Copy `id` for the next calls.

7. **409 duplicate** (same code, different case):

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Accept-Language: en" `
  -H "Content-Type: application/json" `
  -d "{\"code\":\"awassi\",\"nameAr\":\"عواسي 2\",\"nameEn\":\"Awassi 2\",\"speciesCode\":\"SHEEP\"}"
```

Expect HTTP 409, `"code":"BREED_CODE_ALREADY_EXISTS"`, `fieldErrors[0].field` = `code`. Repeat with `Accept-Language: ar` and expect `يوجد سلالة بهذا الرمز بالفعل.`

8. **400 on code change:**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds/PASTE_BREED_ID `
  -X PUT `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Accept-Language: en" `
  -H "Content-Type: application/json" `
  -d "{\"code\":\"CHANGED\",\"nameAr\":\"عواسي محدث\",\"nameEn\":\"Awassi updated\",\"speciesCode\":\"SHEEP\",\"displayOrder\":1}"
```

Expect HTTP 400, `"code":"VALIDATION_ERROR"`, field `code`. GET the same id — stored code is still `AWASSI`.

9. **PUT allowed fields** (echo the existing code or omit it — example echoes):

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds/PASTE_BREED_ID `
  -X PUT `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d "{\"code\":\"AWASSI\",\"nameAr\":\"عواسي محدث\",\"nameEn\":\"Awassi updated\",\"speciesCode\":\"GOAT\",\"displayOrder\":5}"
```

Expect 200, new names, `speciesCode` `GOAT`, `displayOrder` 5, `code` still `AWASSI`.

10. **PATCH deactivate:**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds/PASTE_BREED_ID/status `
  -X PATCH `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d "{\"active\":false}"
```

Expect 200, `"active":false`, names unchanged.

11. **GET list + filters:**

```powershell
curl -s -D - "http://localhost:8080/api/v1/admin/animal-breeds?search=Awassi&speciesCode=GOAT&active=false&page=0&size=20&sort=displayOrder,asc" `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN"
```

12. **403** — token with only `ANIMAL_CONFIG_VIEW` (for example realm role `accountant`) on POST → `"code":"FORBIDDEN"`.

13. **No DELETE:**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-breeds/PASTE_BREED_ID `
  -X DELETE `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN"
```

Expect **405** (path exists for GET/PUT, not DELETE). The row must still be in `animal_breed`.

14. OpenAPI: `http://localhost:8080/swagger-ui.html` — tag **Animal breeds**.

### 5.3 What you cannot click-test yet

There is **no** breed screen in `apps/web`. Do not look for an admin menu. Use `mvn test` and the curls above.

`AnimalBreedAssignmentGuard` has no public URL. Use `AnimalBreedAssignmentGuardTest`, or call `requireAssignable` from a future animal service.

---

## 6. Definition of done (checklist)

- [x] Flyway V2 creates `animal_breed` with unique `code`, check on `display_order`, audit columns  
- [x] GET list/get with `ANIMAL_CONFIG_VIEW`; POST/PUT/PATCH with `BREED_MANAGE`  
- [x] Code trimmed + uppercased; duplicate → 409 `BREED_CODE_ALREADY_EXISTS`  
- [x] Code cannot change after create → 400 `VALIDATION_ERROR` on field `code`  
- [x] PATCH status deactivates without deleting; labels remain  
- [x] Species is a temporary enum  
- [x] Assignment guard blocks inactive breeds (`BREED_INACTIVE`)  
- [x] No DELETE, no React screens  
- [x] Automated tests for permissions, uniqueness, validation, immutability, search, and the guard  
