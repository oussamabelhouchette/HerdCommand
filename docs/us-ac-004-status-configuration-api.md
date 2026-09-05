# US-AC-004 — Animal status configuration API

This document explains **every file added or changed** for the animal-status configuration story: what it does, why it exists, and how to test it.

There are **no React administration screens** in this story (those are US-AC-005). There is **no POST** and **no DELETE**. The catalog is the four seeded lifecycle statuses. Administrators change presentation (labels, color token, order, filter visibility, active flag), not technical codes.

Related: [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md), [us-ac-002-breed-management-api.md](./us-ac-002-breed-management-api.md), [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Table | Flyway **`V3__animal_status_definition.sql`**. Hibernate still `ddl-auto: validate`. |
| Identity | `code` is the **primary key**, uppercase, immutable. No UUID. |
| Seed | `ACTIVE`, `PREGNANT`, `SICK`, `ISOLATED`. All `system_protected = true`. |
| Colors | Allowlist only: `success`, `purple`, `danger`, `warning`, `neutral`. Stored as tokens, not hex. |
| Soft status | `active` + `PATCH …/status`. Inactive rows stay in GET. |
| Required status | `ACTIVE` cannot be deactivated → 409 `STATUS_REQUIRED`. |
| List | Paginated search on code / Arabic label / English label, optional `active`. |
| Permissions | `ANIMAL_CONFIG_VIEW` for reads; `STATUS_CONFIG_MANAGE` for PUT/PATCH. |
| Cache | Active definitions cached; evicted after every write. Admin list is not cached. |
| Assignment | `AnimalStatusAssignmentGuard` throws 409 `STATUS_INACTIVE` if a later story assigns an inactive status. |
| UI | None. |

### API

Base path: `/api/v1/admin/animal-statuses`. All routes need a Bearer JWT.

| Method | Path | Permission | Success | Notes |
|---|---|---|---|---|
| `GET` | `/api/v1/admin/animal-statuses` | `ANIMAL_CONFIG_VIEW` | 200 `PageResponse<StatusResponse>` | Query: `search`, `active`, plus `page` / `size` / `sort`. Default size 20, sort `displayOrder,asc`. |
| `GET` | `/api/v1/admin/animal-statuses/{code}` | `ANIMAL_CONFIG_VIEW` | 200 `StatusResponse` | Code match is case-insensitive. Unknown → 404. |
| `PUT` | `/api/v1/admin/animal-statuses/{code}` | `STATUS_CONFIG_MANAGE` | 200 `StatusResponse` | Labels, color, order, `visibleInFilter`, `active`. Path is the code. |
| `PATCH` | `/api/v1/admin/animal-statuses/{code}/status` | `STATUS_CONFIG_MANAGE` | 200 `StatusResponse` | Body `{ "active": true\|false }`. |

| Realm role | Can list/get | Can PUT/PATCH |
|---|---|---|
| `owner`, `administrator`, `manager` | yes | yes |
| `veterinarian`, `worker`, `accountant` | yes | no (403) |

### Error codes

Same envelope as US-AC-001 (`code`, `message`, `fieldErrors`, `timestamp`, `path`).

| HTTP | `code` | When |
|---|---|---|
| 401 | `UNAUTHORIZED` | No / invalid token |
| 403 | `FORBIDDEN` | Missing the permission above |
| 400 | `VALIDATION_ERROR` | Blank labels, `displayOrder < 0`, missing `active` / `visibleInFilter` |
| 400 | `INVALID_COLOR_TOKEN` | `colorToken` not in the allowlist (`url(...)`, `#ff00aa`, …) |
| 404 | `NOT_FOUND` | Code does not exist |
| 409 | `STATUS_REQUIRED` | Deactivate `ACTIVE` |
| 409 | `STATUS_INACTIVE` | Guard only — not an HTTP route in this story |

Unsafe color (`Accept-Language: en`):

```json
{
  "code": "INVALID_COLOR_TOKEN",
  "message": "This color token is not allowed.",
  "fieldErrors": [
    { "field": "colorToken", "message": "This color token is not allowed." }
  ],
  "timestamp": "2026-09-05T12:00:00Z",
  "path": "/api/v1/admin/animal-statuses/PREGNANT"
}
```

### Seeded rows

| Code | labelEn | colorToken | displayOrder |
|---|---|---|---|
| `ACTIVE` | Active | success | 10 |
| `PREGNANT` | Pregnant | purple | 20 |
| `SICK` | Sick | danger | 30 |
| `ISOLATED` | Isolated | warning | 40 |

`created_by` / `updated_by` on seed rows is `system`.

---

## 2. How the pieces connect

```
Browser / curl / test
    │  Authorization: Bearer <Keycloak access token>
    │  Accept-Language: ar | en
    ▼
SecurityFilterChain  ── no token ──► 401 UNAUTHORIZED
    ▼
JwtAuthoritiesConverter  →  ROLE_*  +  Permission authorities
    ▼
AnimalStatusController
    GET              needs ANIMAL_CONFIG_VIEW
    PUT/PATCH        needs STATUS_CONFIG_MANAGE
         missing ──► 403 FORBIDDEN
    ▼
AnimalStatusService
    unknown code            ──► 404 NOT_FOUND
    color not in allowlist  ──► 400 INVALID_COLOR_TOKEN
    deactivate ACTIVE       ──► 409 STATUS_REQUIRED
    save                    ──► evict cache animalStatusActive
    ▼
Postgres animal_status_definition

Later animal stories (not HTTP yet)
    AnimalStatusAssignmentGuard.requireAssignable(code)
        missing  ──► 404
        inactive ──► 409 STATUS_INACTIVE
```

---

## 3. File-by-file

### 3.1 Database

#### `apps/api/src/main/resources/db/migration/V3__animal_status_definition.sql` *(new)*

**Why:** Status codes must exist before Hibernate validates the entity. Seed must be in SQL so every environment starts with the same four lifecycle values.

**What it creates:** PK `code`, bilingual labels, `color_token` check against the allowlist, `display_order >= 0`, `visible_in_filter`, `active`, `system_protected`, four audit columns. Index `(active, display_order)`. Four INSERTs.

**How to test:** start the API, then:

```sql
SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;
SELECT code, label_en, color_token, active FROM animal_status_definition ORDER BY display_order;
```

You should see versions `1`, `2`, and `3`, and four status rows.

---

### 3.2 Domain

#### `ColorToken.java` *(new)*

**Why:** Reject hex and CSS. JSON and the DB store the lowercase token (`success`), not `SUCCESS`.

`tryParse` returns null for unknown values so the service can emit `INVALID_COLOR_TOKEN` instead of a Jackson 400 `VALIDATION_ERROR`.

#### `ColorTokenConverter.java` *(new)*

Maps the enum to/from the `color_token` varchar.

#### `AnimalStatusDefinition.java` *(new)*

**Why:** `code` is the PK, so this entity cannot extend `AuditedEntity` (UUID `id`). Same audit listener and column names.

`isRequiredLifecycleStatus()` is true only for `ACTIVE`.

#### `AnimalStatusDefinitionRepository.java` *(new)*

`findByCodeIgnoreCase`, `findByActiveTrueOrderByDisplayOrderAsc`, and `search` (optional active + LIKE on code/labels).

#### `AnimalStatusService.java` *(new)*

| Method | Behaviour |
|---|---|
| `search` | Admin list. Not cached. |
| `get` | 404 if missing. |
| `listActive` | `@Cacheable("animalStatusActive")` for later filter stories. |
| `update` | Parse color; apply `ACTIVE` rule; save; evict cache. |
| `updateStatus` | Active flag only; same `ACTIVE` rule; evict cache. |

#### `AnimalStatusAssignmentGuard.java` *(new)*

`requireAssignable(code)` — not called from the controller. Wire it when animals are assigned a status.

#### `CacheConfig.java` *(new)*

`@EnableCaching` + `ConcurrentMapCacheManager` for `animalStatusActive`.

---

### 3.3 HTTP

#### `UpdateStatusRequest` / `StatusActiveRequest` / `StatusResponse` / `AnimalStatusController`

Controller path `/api/v1/admin/animal-statuses`. No POST. OpenAPI tag **Animal statuses**.

`colorToken` on the update body is a **string** so `"url(...)"` reaches the service (an enum field would fail earlier as `VALIDATION_ERROR`).

---

### 3.4 Shared

#### `ErrorCodes.java` *(changed)*

Adds `INVALID_COLOR_TOKEN`, `STATUS_REQUIRED`, `STATUS_INACTIVE`.

#### `messages_en.properties` / `messages_ar.properties` *(changed)*

`error.status.invalidColor`, `error.status.required`, `error.status.inactive`.

---

## 4. How to run and test

### 4.1 Automated

```powershell
mvn -f apps/api/pom.xml test "-Dtest=AnimalStatusApiTest,AnimalStatusAssignmentGuardTest" "-Daether.connector.https.securityMode=insecure"
```

Or the full suite:

```powershell
mvn -f apps/api/pom.xml test "-Daether.connector.https.securityMode=insecure"
```

### 4.2 curl (API + Keycloak running)

Need a token with `STATUS_CONFIG_MANAGE` (`owner` / `administrator` / `manager`).

1. **List seed**

```powershell
curl -s -D - "http://localhost:8080/api/v1/admin/animal-statuses?sort=displayOrder,asc" `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN"
```

Expect 200, `total` 4, first item `ACTIVE` / `success`.

2. **Update presentation**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-statuses/PREGNANT `
  -X PUT `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Accept-Language: en" `
  -H "Content-Type: application/json" `
  -d "{\"labelAr\":\"حامل\",\"labelEn\":\"Pregnant\",\"colorToken\":\"purple\",\"displayOrder\":20,\"visibleInFilter\":true,\"active\":true}"
```

Expect 200, `"code":"PREGNANT"`.

3. **Reject color**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-statuses/PREGNANT `
  -X PUT `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Accept-Language: en" `
  -H "Content-Type: application/json" `
  -d "{\"labelAr\":\"حامل\",\"labelEn\":\"Pregnant\",\"colorToken\":\"url(...)\",\"displayOrder\":20,\"visibleInFilter\":true,\"active\":true}"
```

Expect 400, `"code":"INVALID_COLOR_TOKEN"`.

4. **ACTIVE cannot be turned off**

```powershell
curl -s -D - http://localhost:8080/api/v1/admin/animal-statuses/ACTIVE/status `
  -X PATCH `
  -H "Authorization: Bearer PASTE_ACCESS_TOKEN" `
  -H "Content-Type: application/json" `
  -d "{\"active\":false}"
```

Expect 409, `"code":"STATUS_REQUIRED"`.

5. **403** — token with only `ANIMAL_CONFIG_VIEW` on PUT.

6. **No POST / DELETE** — those methods are not mapped (404 or 405). The four rows stay in the table.

OpenAPI: `http://localhost:8080/swagger-ui.html` — tag **Animal statuses**.

### 4.3 What you cannot click-test yet

There is **no** Status tab in `apps/web`. Use `mvn test` and the curls above. The assignment guard has no public URL.

---

## 5. Definition of done (checklist)

- [x] Flyway V3 creates `animal_status_definition` and seeds four protected statuses  
- [x] GET list/get with `ANIMAL_CONFIG_VIEW`; PUT/PATCH with `STATUS_CONFIG_MANAGE`  
- [x] Code is the path key and never changes  
- [x] Color allowlist; unknown token → 400 `INVALID_COLOR_TOKEN`  
- [x] `ACTIVE` cannot be deactivated → 409 `STATUS_REQUIRED`  
- [x] Other statuses may be deactivated and remain visible  
- [x] Active-status cache evicted after writes  
- [x] Assignment guard blocks inactive statuses (`STATUS_INACTIVE`)  
- [x] No POST, no DELETE, no React screens  
- [x] Automated tests for seed, auth, color, immutability, `ACTIVE`, search, guard, and cache  
