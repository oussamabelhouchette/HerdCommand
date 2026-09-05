# US-AC-006 — Farm group management API

This document explains **every file added or changed** for farm-owned group CRUD. There is still **no animal table** and **no assignment endpoint**. Deactivate works like breeds. `GROUP_NOT_EMPTY`, farm membership, and farm switching wait for later stories.

Related: [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md), [us-ac-007-farm-group-administration-ui.md](./us-ac-007-farm-group-administration-ui.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Farm table | Flyway **`V4__farm_and_animal_group.sql`**. Seed one farm `HARRI`. Tests create extra farms with SQL. |
| Demo groups | Flyway **`V5__seed_harri_animal_groups.sql`**. Five active groups from the animal-list mock (`HARRI-DAMS`, `FARM-SIRES`, `PRODUCTION`, `GROUP-B`, `GROUP-C`). |
| Groups | `animal_group` with immutable `farm_id` + `code`. Unique `(farm_id, code)`. Codes stored uppercase. |
| Types | Allowlist: `DAMS`, `SIRES`, `YOUNG`, `FATTENING`, `ISOLATION`, `OTHER`. |
| Soft status | `PATCH …/status`. No DELETE. |
| Isolation | Unknown farm or a group that does not belong to the path farm → 404 `NOT_FOUND`. |
| Access | Anyone with `GROUP_VIEW` / `GROUP_MANAGE` may use any existing farm. No membership table yet. |
| `animalCount` | Always `0` until animals exist. |
| Assignment | `POST …/animals` is **not** mapped. |

### API

| Method | Path | Permission | Success |
|---|---|---|---|
| GET | `/api/v1/farms` | `GROUP_VIEW` | 200 list |
| GET | `/api/v1/farms/{farmId}/animal-groups` | `GROUP_VIEW` | 200 page |
| GET | `/api/v1/farms/{farmId}/animal-groups/{groupId}` | `GROUP_VIEW` | 200 |
| POST | `/api/v1/farms/{farmId}/animal-groups` | `GROUP_MANAGE` | 201 |
| PUT | `/api/v1/farms/{farmId}/animal-groups/{groupId}` | `GROUP_MANAGE` | 200 |
| PATCH | `/api/v1/farms/{farmId}/animal-groups/{groupId}/status` | `GROUP_MANAGE` | 200 |

List query: `search`, `active`, `groupTypeCode`, plus page/size/sort. Default sort `nameAr,asc`.

| Realm role | List/get | Create/update/status |
|---|---|---|
| `owner`, `administrator`, `manager` | yes | yes |
| `veterinarian`, `worker` | yes | no |
| `accountant` | no | no |

### Error codes

| HTTP | `code` | When |
|---|---|---|
| 401 | `UNAUTHORIZED` | No token |
| 403 | `FORBIDDEN` | Missing permission |
| 400 | `VALIDATION_ERROR` | Blank names, bad code, unknown type, capacity `< 1` |
| 404 | `NOT_FOUND` | Unknown farm or group / wrong farm |
| 409 | `GROUP_CODE_ALREADY_EXISTS` | Same code on the same farm (case-insensitive) |

Same code on a **different** farm is allowed.

### Deferred

- Animal entity and `POST …/animals`
- `GROUP_NOT_EMPTY` / `moveToGroupId` / unassign
- Farm membership / JWT farm claims
- Farm switcher in the UI (US-AC-007 uses the first farm)

---

## 2. How the pieces connect

```
GET /api/v1/farms
    GROUP_VIEW ──► FarmService.list (all farms today)

/api/v1/farms/{farmId}/animal-groups
    farm missing        ──► 404
    group wrong farm    ──► 404
    duplicate code      ──► 409 GROUP_CODE_ALREADY_EXISTS
    POST/PUT/PATCH      needs GROUP_MANAGE
```

---

## 3. File-by-file

#### `apps/api/src/main/resources/db/migration/V4__farm_and_animal_group.sql`

Creates `farm` and `animal_group`. Seeds `HARRI` / مزرعة حري. Capacity check: null or `> 0`. Unique per farm on stored (uppercase) code so H2 tests and Postgres both apply.

#### Domain

- `domain/farm/Farm.java`, `FarmRepository`, `FarmService`
- `domain/group/GroupTypeCode.java`, `AnimalGroup.java`, `AnimalGroupRepository`, `AnimalGroupService`

`AnimalGroup` stores `farmId` as a UUID column (no lazy farm join on list).

#### API

- `api/farm/FarmController`, `FarmResponse`
- `api/farm/group/AnimalGroupController` + create/update/status DTOs + `GroupResponse` (`animalCount` hardcoded 0)

#### Errors / i18n

`ErrorCodes.GROUP_CODE_ALREADY_EXISTS`. `GlobalExceptionHandler` maps the unique constraint. `messages_en.properties` / `messages_ar.properties`.

#### Tests

`apps/api/src/test/java/com/herdcommand/api/admin/AnimalGroupApiTest.java` — auth, seed create, duplicate, second-farm same code, cross-farm 404, unknown farm, validation, search + `animalCount` 0, PATCH, no assignment route.

---

## 4. How to test

```powershell
mvn -f apps/api/pom.xml test "-Dtest=AnimalGroupApiTest" "-Daether.connector.https.securityMode=insecure"
```

Restart the API so Flyway applies V4. Then:

```powershell
curl -s http://localhost:8080/api/v1/farms -H "Authorization: Bearer PASTE"
```

Expect `HARRI`. Create a group on that `id`, then GET the list. Duplicate the code → 409. OpenAPI tag **Animal groups**.

---

## 5. Definition of done

- [x] V4 farm + group tables; seed HARRI
- [x] CRUD + status scoped by path `farmId`
- [x] Unique code per farm; 404 across farms
- [x] `animalCount` is 0; no assignment route
- [x] Permissions `GROUP_VIEW` / `GROUP_MANAGE`
- [x] Automated tests above
