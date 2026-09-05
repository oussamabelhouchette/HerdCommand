# PA-003 — Dynamic feature catalog

This document explains **every file added or changed** for the third farm-management story: what it does, why it exists, and how to test it.

There is **no farm list, create wizard, or Keycloak owner lookup** here. Those are PA-004+. This story stores released modules in a catalog so a new feature is a row, not a Boolean on `farm` or a hardcoded React card.

Related: [us-pa-001-platform-admin-security-foundation.md](./us-pa-001-platform-admin-security-foundation.md), [us-pa-002-farm-and-membership-database-foundation.md](./us-pa-002-farm-and-membership-database-foundation.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Catalog | Flyway **V7** `feature_catalog`. Codes are unique and immutable. |
| Farm assignment | `farm_feature` with unique `(farm_id, feature_id)`. Optional `configuration_json` must be a JSON object when present. |
| Seed | Idempotent `ANIMAL_MANAGEMENT` (`AVAILABLE`, enableable). Optional `HEALTH` as `COMING_SOON` (visible when requested, not enableable). |
| No farm flags | No `animalManagementEnabled` (or any feature Boolean) on `farm`. |
| List API | `GET /api/v1/platform/features`. Default = available only. `?includeComingSoon=true` also returns coming-soon rows. |
| Labels | One `name` / `description` resolved from `Accept-Language`: `ar`, `en`, `fr`. Parsed in the catalog service so adding French does not change global i18n fallback. |
| Assignment | `POST /api/v1/platform/farms/{farmId}/features`. Inactive, retired, or `COMING_SOON` → `400 FEATURE_NOT_AVAILABLE`. |
| Who | `PLATFORM_ADMIN` only. Farm owners get 403. |
| React | Farm-settings landing loads the catalog and renders cards from the API. Checkboxes use `feature.code`. Coming-soon items are disabled. |
| Cache | Not used. Catalog writes would need eviction; there is no catalog write API yet. |

### API

| Method | Path | Permission | Success |
|---|---|---|---|
| GET | `/api/v1/platform/features` | `PLATFORM_ADMIN` | 200 `{ items: [...] }` |
| GET | `/api/v1/platform/features?includeComingSoon=true` | `PLATFORM_ADMIN` | 200 including `COMING_SOON` |
| POST | `/api/v1/platform/farms/{farmId}/features` | `PLATFORM_ADMIN` | 200 assignment record |

List item: `id`, `code`, `name`, `description`, `iconCode`, `releaseStatus`, `enableable`, `displayOrder`.

### Error codes

| HTTP | `code` | When |
|---|---|---|
| 400 | `FEATURE_NOT_AVAILABLE` | Enable inactive, retired, or `COMING_SOON` |
| 400 | `FEATURE_ASSIGNMENT_INVALID` | `configurationJson` is not a JSON object |
| 404 | `FEATURE_NOT_FOUND` | Unknown feature id |
| 404 | `NOT_FOUND` | Unknown farm |
| 401 / 403 | existing | No token / not platform admin |

---

## 2. How the pieces connect

```
GET /api/v1/platform/features
    Accept-Language ar|en|fr ──► localized name
    includeComingSoon=false ──► AVAILABLE only
    includeComingSoon=true  ──► AVAILABLE + COMING_SOON (enableable=false)

POST /api/v1/platform/farms/{farmId}/features
    farm missing            ──► 404
    feature missing         ──► 404 FEATURE_NOT_FOUND
    not AVAILABLE+active    ──► 400 FEATURE_NOT_AVAILABLE
    else upsert farm_feature

/admin/farm-settings
    listFeatures(..., true)
    FarmSettings maps items by id/code
    checkbox value = feature.code
    !enableable ──► disabled
```

---

## 3. File-by-file

#### `apps/api/src/main/resources/db/migration/V7__feature_catalog.sql` *(new)*

Creates catalog + assignment tables. Seeds `ANIMAL_MANAGEMENT` and `HEALTH` with `WHERE NOT EXISTS`.

#### Domain *(new)*

`FeatureCatalog`, `FarmFeature`, repositories, `FeatureReleaseStatus`, `FeatureLocales`, `FeatureCatalogService`, `FarmFeatureService`.

`FeatureCatalog.code` is `updatable = false`. `isEnableable()` is `active && AVAILABLE`.

#### API *(new)*

`FeatureCatalogController`, DTOs under `api/platform/feature`. Controllers return records, not entities.

#### Errors / i18n *(changed)*

`FEATURE_NOT_FOUND`, `FEATURE_NOT_AVAILABLE`, `FEATURE_ASSIGNMENT_INVALID`. `BadRequestException` for 400. Arabic/English messages.

#### Web

| File | Why |
|---|---|
| `apps/web/src/lib/features.ts` | Catalog client + `selectedFeatureCodes` |
| `farm-settings/page.tsx` | Loads catalog with coming soon |
| `FarmSettings.tsx` | Renders API items; removes hardcoded soon cards |
| `ar.json` / `en.json` | Feature section copy |

---

## 4. How to test

```powershell
mvn -f apps/api/pom.xml test "-Dtest=FeatureCatalogApiTest,FlywayMigrationTest,PlatformAdminSecurityTest" "-Daether.connector.https.securityMode=insecure"
npm --prefix apps/web test
```

Restart the API so Flyway applies V7. Then as a platform admin:

```powershell
curl -s "http://localhost:8080/api/v1/platform/features?includeComingSoon=true" -H "Authorization: Bearer PASTE" -H "Accept-Language: ar"
```

Expect `ANIMAL_MANAGEMENT` enableable and `HEALTH` coming soon. Open `/admin/farm-settings`: Animal management is checked; Health records is disabled. English `/en/admin/farm-settings` shows English labels.

---

## 5. Assumptions and gaps

- `name_en` is stored so the existing English admin locale works. The backlog listed `name_ar` / `name_fr` only.
- `HEALTH` is a placeholder coming-soon module. A later story can add more rows without a schema change.
- Assignment `POST` is the minimum needed to reject unreleased features over HTTP. Farm create (PA-005) will reuse `FarmFeatureService`.
- No TanStack Query. The “query hook” is `listFeatures` on the server page, same pattern as PA-001.
- French is used for catalog labels only. Global API error messages still fall back to Arabic for `Accept-Language: fr`.

---

## 6. Definition of done

- [x] V7 catalog + `farm_feature`; no feature columns on `farm`
- [x] Idempotent `ANIMAL_MANAGEMENT` seed; available and enableable
- [x] `COMING_SOON` cannot be enabled (`400 FEATURE_NOT_AVAILABLE`)
- [x] Localized `name` from `Accept-Language`
- [x] Farm-settings cards come from the API
- [x] Selections stored by feature code / id
- [x] Automated tests above
