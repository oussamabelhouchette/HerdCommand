# PA-002 — Farm and membership database foundation

This document explains **every file added or changed** for the second farm-management story: what it does, why it exists, and how to test it.

There is **no farm list, create wizard, Keycloak owner lookup, or feature catalog** here. Those are PA-003+. This story stores farms as isolated tenants with generated codes, owner membership, a subscription row, and optimistic locking.

Related: [us-pa-001-platform-admin-security-foundation.md](./us-pa-001-platform-admin-security-foundation.md), [us-ac-006-farm-group-api.md](./us-ac-006-farm-group-api.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Farm table | **Evolved** the existing US-AC-006 `farm` table. Did **not** create a second farm table. `HARRI` and animal groups keep working. |
| Code | Server-generated `FARM-TN-0001` style from `farm_code_seq`. Immutable. Never accepted from a client. Seeded `HARRI` keeps its original code. |
| Names | `name_ar` / `name_en` stay for the existing bilingual UI. `name_fr` added as required by this story (HARRI backfilled from English). Max 150. |
| Location / locale | `governorate_code` (Tunisia ISO-style `TN-11`…), `address`, `timezone` default `Africa/Tunis`, `default_language` `ar` or `fr`, `currency_code` fixed `TND`. |
| Status | `SETUP`, `ACTIVE`, `SUSPENDED`, `ARCHIVED`. Existing `active` boolean is synced: only `ACTIVE` is `true`. HARRI stays `ACTIVE`. |
| Version | Optimistic lock on `farm` and `farm_subscription`. Stale version → `409 FARM_VERSION_CONFLICT`. |
| Membership | `farm_membership`: one row per farm + Keycloak `sub`. Role starts as `FARM_OWNER`. Status `INVITED` / `ACTIVE` / `SUSPENDED` / `REMOVED`. Email stored lowercase. |
| Owner rule | A farm **cannot be activated** without an active owner. An **active** farm cannot lose its last owner. Seeded HARRI is already active and has no membership (grandfathered). |
| Subscription | One current `farm_subscription` per farm. Plans `TRIAL` / `ESSENTIAL` / `PROFESSIONAL`. New farms created by the service get a 30-day `TRIAL`. HARRI gets seeded `ESSENTIAL`. |
| HTTP | No public create/list/edit farm API. `GET /api/v1/platform/farms` is still the PA-001 empty probe. Domain service is what later stories will call. |
| Entities | Controllers still return records, not JPA entities. |

### Error codes

| HTTP | `code` | When |
|---|---|---|
| 409 | `FARM_OWNER_REQUIRED` | Activate with no active owner, or remove the last owner of an active farm |
| 409 | `FARM_VERSION_CONFLICT` | Stale `version` or JPA optimistic lock |
| 409 | `FARM_CODE_ALREADY_EXISTS` | Duplicate farm code (should not happen for generated codes) |
| 409 | `OWNER_ALREADY_ASSIGNED` | Same Keycloak user already has a membership on that farm |
| 404 | `NOT_FOUND` | Unknown farm or membership / wrong farm (same non-disclosing 404 as groups) |

---

## 2. How the pieces connect

```
FarmTenantService.createFarm
    FarmCodeGenerator.nextCode ──► FARM-TN-0001 (sequence)
    persist farm status=SETUP, currency=TND
    persist farm_subscription plan=TRIAL
    return FarmTenantSnapshot (not the entity)

FarmTenantService.activate
    no ACTIVE FARM_OWNER ──► 409 FARM_OWNER_REQUIRED
    else status=ACTIVE, active=true

FarmTenantService.updateIdentity
    expectedVersion != row.version ──► 409 FARM_VERSION_CONFLICT
    JPA @Version race               ──► same code via GlobalExceptionHandler

Animal groups
    still use farm.id / farm.active / name_ar / name_en
```

---

## 3. File-by-file

Paths are relative to the repo root unless noted.

#### `apps/api/src/main/resources/db/migration/V6__farm_membership_and_subscription.sql` *(new)*

**Why:** Hibernate `ddl-auto: validate` plus H2 tests. Must work without `gen_random_uuid()` or expression indexes.

**What it does:** widens name columns; adds tenant columns with defaults so HARRI and existing group-test inserts keep loading; creates `farm_code_seq`, `farm_membership`, `farm_subscription`; seeds HARRI’s subscription.

#### `domain/farm/Farm.java` *(changed)*

Adds French name, governorate, address, timezone, language, currency, status, `@Version`. `applyStatus` keeps the old `active` flag in sync for US-AC-006.

#### `FarmMembership.java` / `FarmSubscription.java` *(new)*

Audited entities. Membership unique `(farm_id, keycloak_user_id)`. Subscription unique `(farm_id)`.

#### Enums / helpers *(new)*

`FarmStatus`, `FarmLanguage` (+ converter so the DB stores `ar`/`fr`), `FarmMembershipRole`, `FarmMembershipStatus`, `FarmPlanCode`, `FarmSubscriptionStatus`, `GovernorateCodes`.

#### Repositories *(changed / new)*

`FarmRepository.existsByCodeIgnoreCase`. `FarmMembershipRepository` and `FarmSubscriptionRepository` methods take `farmId` so later APIs cannot load another farm’s row by id alone.

#### `FarmCodeGenerator` / `FarmTenantService` *(new)*

Server-side code. Create / activate / assign owner / remove owner / versioned identity update. Commands and `FarmTenantSnapshot` are records — not JPA types.

#### Errors / i18n *(changed)*

`ErrorCodes` plus `messages_en.properties` / `messages_ar.properties`. `GlobalExceptionHandler` maps optimistic lock and farm unique constraints.

#### Tests

| Class | What it proves |
|---|---|
| `FlywayMigrationTest` | V6 tables, sequence, HARRI `ACTIVE` |
| `FarmTenantFoundationTest` | unique code, owner required, membership uniqueness, farm-scoped queries, stale version, JPA optimistic lock, HARRI compatibility |
| `FarmVersionConflictApiTest` | `409 FARM_VERSION_CONFLICT` envelope (test-only probe, `PLATFORM_ADMIN` only) |
| `AnimalGroupApiTest` | second-farm insert includes the new NOT NULL columns |

`apps/api/src/test/java/com/herdcommand/api/support/FarmVersionProbeController.java` is test-profile only. It is **not** the PA-006/PA-009 public API.

---

## 4. How to test

```powershell
mvn -f apps/api/pom.xml test "-Dtest=FlywayMigrationTest,FarmTenantFoundationTest,FarmVersionConflictApiTest,AnimalGroupApiTest" "-Daether.connector.https.securityMode=insecure"
```

Restart the API so Flyway applies V6. HARRI still appears on `GET /api/v1/farms`. `GET /api/v1/platform/farms` is still an empty probe.

There is no create-farm screen or public write endpoint in this story.

---

## 5. Assumptions and gaps

- Existing `name_en` is kept so animal settings stay Arabic/English. `name_fr` and `default_language` `ar`/`fr` match the platform-admin backlog (Tunisia).
- HARRI has no owner membership. The owner rule applies to **activation** and **removing the last owner**, not to farms that were already active.
- Farm codes for **new** farms use `FARM-TN-####`. Legacy codes such as `HARRI` remain valid.
- Governorate allowlist is Tunisia `TN-xx`. Unknown codes fail validation.
- No React changes. No Keycloak user lookup (PA-004). No atomic create-with-owner HTTP API (PA-005). No farm list (PA-006).

---

## 6. Definition of done

- [x] Flyway V6 evolves `farm`; adds membership + subscription
- [x] Server-generated unique immutable farm code
- [x] Activation without an active owner → `FARM_OWNER_REQUIRED`
- [x] Stale version → `409 FARM_VERSION_CONFLICT`
- [x] Repositories scope membership/subscription by `farmId`
- [x] JPA entities are not returned from production controllers
- [x] HARRI + animal groups still load
- [x] Automated tests above
