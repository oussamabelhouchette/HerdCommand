# PA-006 — Farm list and details API

This document explains **every file added or changed** for searching and inspecting farms. There is **no React all-farms page** here (PA-007).

Related: [us-pa-002-farm-and-membership-database-foundation.md](./us-pa-002-farm-and-membership-database-foundation.md), [us-pa-005-atomic-farm-creation-api.md](./us-pa-005-atomic-farm-creation-api.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| List | `GET /api/v1/platform/farms` replaces the PA-001 empty probe. `PLATFORM_ADMIN` only. |
| Details | `GET /api/v1/platform/farms/{farmId}` — 404 `NOT_FOUND` if missing. |
| Summary | `GET /api/v1/platform/farms/summary` — totals for all / ACTIVE / SETUP / SUSPENDED / ARCHIVED, plus `activeAnimals`. |
| Search | Partial match on farm names (ar/en/fr), code, owner email, owner display name. |
| Filters | `status`, `planCode`, `featureCode` (enabled assignment). Combined with AND. |
| Sort | `createdAt` (default desc), `code`, `status`, `nameAr`, `nameEn`, `nameFr`. Other fields → `400 VALIDATION_ERROR`. |
| Page | Default 20, max 100. |
| N+1 | One farm page/count query, then one membership batch, one subscription batch, one feature-code batch. |
| Status labels | Technical codes (`ACTIVE`). React localizes later. |
| Localized name | `name` from `Accept-Language` (`ar` / `en` / `fr`). |
| Animals | There is still no animal table. `activeAnimalCount` and summary `activeAnimals` are **0**. |
| Owner name | Stored on `farm_membership.display_name` at assign/onboarding so the list does not call Keycloak. |

HARRI stays listed. It has no owner membership (grandfathered). Owner fields are omitted (`NON_NULL` JSON).

---

## 2. How the pieces connect

```
GET /api/v1/platform/farms
  PLATFORM_ADMIN
  sanitize page (max 100) + sort whitelist
  FarmRepository.searchPlatformFarms   (EXISTS filters, no per-row joins)
  batch: memberships, subscriptions, enabled feature codes
  PageResponse items

GET /api/v1/platform/farms/summary
  GROUP BY farm.status
  activeAnimals = 0

GET /api/v1/platform/farms/{id}
  same lookups for one farm
```

---

## 3. File-by-file

#### `V9__farm_membership_display_name.sql` *(new)*

Nullable `display_name` on `farm_membership` so owner search stays in PostgreSQL.

#### `PlatformFarmQueryService` / `PlatformFarmQueryController` *(new)*

List, details, summary. Controllers return records.

#### `FarmRepository.searchPlatformFarms` *(changed)*

`EXISTS` for plan, feature, and owner search so pagination sort stays on `farm`.

#### Probe removed

`GET /farms` on `PlatformAdminController` and `PlatformFarmsProbeResponse` are gone. `PlatformAdminSecurityTest` now expects a `PageResponse`.

---

## 4. How to test

```powershell
mvn -f apps/api/pom.xml test "-Dtest=FarmListApiTest,PlatformAdminSecurityTest,FlywayMigrationTest,FarmTenantFoundationTest,FarmOnboardingApiTest" "-Daether.connector.https.securityMode=insecure"
```

Restart the API so Flyway applies V9.

```powershell
curl -s "http://localhost:8080/api/v1/platform/farms?search=HARRI&status=ACTIVE&planCode=ESSENTIAL" -H "Authorization: Bearer PASTE"
curl -s http://localhost:8080/api/v1/platform/farms/summary -H "Authorization: Bearer PASTE"
```

---

## 5. Assumptions and gaps

- No React list page (PA-007).
- Animal usage is zero until an animal table exists.
- Owner display name is whatever was stored at assign time; later Keycloak profile edits are not synced here.
- `GET /api/v1/farms` (farm-user list) is unchanged.

---

## 6. Definition of done

- [x] Search by name, code, owner email, owner display name
- [x] Combined status + plan filters
- [x] Bounded queries for a page of farms
- [x] Summary counts
- [x] Details by id
- [x] Authorization and invalid-filter tests
