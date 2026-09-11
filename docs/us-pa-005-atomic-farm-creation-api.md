# PA-005 — Atomic farm creation API

This document explains **every file added or changed** for creating a farm, owner membership, subscription, and features in one operation. There is **no list page and no create wizard** here (PA-006 / PA-008).

Related: [us-pa-002-farm-and-membership-database-foundation.md](./us-pa-002-farm-and-membership-database-foundation.md), [us-pa-003-dynamic-feature-catalog.md](./us-pa-003-dynamic-feature-catalog.md), [us-pa-004-keycloak-owner-lookup-and-invitation.md](./us-pa-004-keycloak-owner-lookup-and-invitation.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| HTTP | `POST /api/v1/platform/farms` with `Idempotency-Key`. `PLATFORM_ADMIN` only. Returns `201` + `Location`. |
| List path | `GET /api/v1/platform/farms` is implemented in PA-006. |
| Atomic DB write | One transaction: farm (`SETUP`), `FARM_OWNER` membership, subscription, `farm_feature` rows. |
| Keycloak | Resolve/provision **after** validation and **before** the DB write. A DB transaction cannot roll Keycloak back. |
| Compensation | If this operation **created** a Keycloak user and the DB write fails, delete that user. If delete fails, keep `farm_onboarding_compensation` for retry. Existing users are never deleted. |
| Idempotency | Same key + same body → original `201` body, no second farm. Same key + different body → `409 IDEMPOTENCY_CONFLICT`. |
| Farm code | Server-generated. Never accepted from the client. |
| Plan limits | Server catalog. Client values above the cap → `400 PLAN_LIMIT_INVALID`. Persisted limits are always the catalog caps, not the React numbers. |
| Features | Validated by catalog code **before** Keycloak. `HEALTH` / coming-soon → `400 FEATURE_NOT_AVAILABLE`, no farm, no user. |
| `nameEn` | Optional. If omitted, copied from `nameFr` (same backfill as HARRI). |
| Governorate | Existing `TN-xx` codes. The story example `TUNIS` is rejected. |
| Creating admin | Existing JPA audit `created_by` = JWT `sub`, or `preferred_username` if Keycloak 26 omitted `sub`. |

### Error codes

| HTTP | `code` | When |
|---|---|---|
| 400 | `IDEMPOTENCY_KEY_REQUIRED` | Missing or non-UUID `Idempotency-Key` |
| 400 | `FEATURE_NOT_AVAILABLE` | Coming-soon / inactive / retired feature |
| 400 | `PLAN_NOT_FOUND` | Unknown `planCode` |
| 400 | `PLAN_LIMIT_INVALID` | Animals/team above the plan cap, or a trial date on a paid plan |
| 400 | `INVALID_FARM_STATUS_TRANSITION` | `initialStatus` other than `SETUP` |
| 400 | `IDENTITY_DISABLED` | Owner account exists but is disabled |
| 409 | `IDEMPOTENCY_CONFLICT` | Key reused with a different body, or an in-flight key timed out |
| 503 | `IDENTITY_PROVIDER_UNAVAILABLE` | Keycloak down during lookup/provision |

### Server plan catalog

| Plan | Max animals | Max team | Trial end |
|---|---|---|---|
| `TRIAL` | 50 | 3 | Default +30 days; optional date must be in the future and within 365 days |
| `ESSENTIAL` | 300 | 10 | Must be omitted |
| `PROFESSIONAL` | 2000 | 50 | Must be omitted |

---

## 2. How the pieces connect

```
POST /api/v1/platform/farms
  PLATFORM_ADMIN
  validate fields + catalog features + plan caps   (no I/O)
  claim idempotency key (STARTED, committed)
    replay COMPLETED ──► original 201
    in-flight STARTED ──► poll until COMPLETED
  IdentityProvisioningService.resolveOwner
    enabled user ──► membership ACTIVE
    missing     ──► create Keycloak user + execute-actions email, membership INVITED
  one DB transaction
    farm + subscription + membership + farm_feature
    mark idempotency COMPLETED
  on DB failure
    mark FAILED
    if identityCreated ──► delete Keycloak user
      delete failed ──► farm_onboarding_compensation
```

---

## 3. File-by-file

#### `V8__farm_onboarding_idempotency.sql` *(new)*

`farm_onboarding_request` (unique key, fingerprint, status, stored response) and `farm_onboarding_compensation`.

#### `FarmOnboardingService` / `FarmOnboardingPersistence` / `FarmOnboardingIdempotencyService` *(new)*

Orchestration, one-transaction persist, and `REQUIRES_NEW` claim/fail so retries can see the key after a rollback.

#### `PlatformFarmCreationController` *(new)*

`POST /api/v1/platform/farms` only. Controllers still return records.

#### `FarmTenantService` *(changed)*

`persistDraftFarm` (no subscription), `persistSubscription`, `assignOwner(..., status)` so invited owners are not forced to `ACTIVE`. Existing `createFarm` still writes a 30-day trial.

#### `FarmPlanLimits` *(new)*

Server caps. Shared by onboarding and the existing trial defaults.

#### Errors / i18n / CORS *(changed)*

New codes above. `Idempotency-Key` allowed on CORS. `Location` exposed.

---

## 4. How to test

```powershell
mvn -f apps/api/pom.xml test "-Dtest=FarmOnboardingApiTest,FarmPlanLimitsTest,FarmTenantFoundationTest,FlywayMigrationTest,IdentityServicesTest" "-Daether.connector.https.securityMode=insecure"
```

Restart the API so Flyway applies V8. Create still needs a `PLATFORM_ADMIN` token and a working `herdcommand-admin` client for live Keycloak.

```powershell
curl -s -D - http://localhost:8080/api/v1/platform/farms -H "Authorization: Bearer PASTE" -H "Content-Type: application/json" -H "Idempotency-Key: 11111111-1111-1111-1111-111111111111" -d "{\"nameAr\":\"مزرعة النخيل\",\"nameFr\":\"Ferme des Palmiers\",\"governorateCode\":\"TN-11\",\"defaultLanguage\":\"ar\",\"owner\":{\"email\":\"owner@example.tn\",\"displayName\":\"Mohamed\"},\"subscription\":{\"planCode\":\"TRIAL\"},\"enabledFeatureCodes\":[\"ANIMAL_MANAGEMENT\"]}"
```

---

## 5. Assumptions and gaps

- No React wizard (PA-008). Farm-settings still only looks up the owner.
- `GET /api/v1/platform/farms/{id}` is not implemented (PA-006). `Location` points at that future path.
- Phone is validated (E.164) and not stored; membership has no phone column.
- Invitation email still depends on Keycloak SMTP. Send failure keeps membership `INVITED` and `invitationEmailSent=false`.
- `INVITED` → `ACTIVE` after the owner finishes Keycloak setup is still a later story.
- No TanStack Query. No farm list UI.

---

## 6. Definition of done

- [x] `POST /api/v1/platform/farms` for platform admins
- [x] Existing owner → `ACTIVE`; missing owner → invitation + `INVITED`
- [x] Coming-soon feature rejected with no farm
- [x] One DB transaction; Keycloak compensated on DB failure
- [x] Idempotent create + concurrency coverage
- [x] Server-side plan caps and generated farm code
- [x] Creating administrator recorded on audit columns
- [x] Automated tests above
