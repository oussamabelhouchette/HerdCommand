# PA-007 — React all-farms administration page

This document explains **every file added or changed** for the all-farms administration screen. The APIs are PA-006. There is **no** create wizard (PA-008) and **no** edit/suspend (PA-009).

Related: [us-pa-006-farm-list-and-details-api.md](./us-pa-006-farm-list-and-details-api.md), [us-pa-001-platform-admin-security-foundation.md](./us-pa-001-platform-admin-security-foundation.md), [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md), [nextjs.md](./nextjs.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Route | Existing `/admin/farm-settings` (not `/platform/farms`). Arabic default; English `/en/admin/farm-settings`. |
| Who sees it | Unchanged: Keycloak `PLATFORM_ADMIN` via `requirePlatformAdmin`. |
| Data | Server first paint + client reload of `GET /api/v1/platform/farms`, `/summary`, and `/{farmId}`. Feature filter options from `GET /api/v1/platform/features`. |
| Search | 400 ms debounce on name, code, owner email / display name. |
| Filters | Technical codes: `status`, `planCode`, `featureCode`. Labels are localized. |
| URL | `search`, `status`, `planCode`, `featureCode`, `page`, `sort` are written with `router.replace` and restored on refresh. |
| Actions | **Open details** loads one farm. Add / Edit / Suspend stay disabled. |
| Empty / error | Empty row copy. Retryable list and details errors. Loading skeleton rows. |
| Hardcoded farms | None. HARRI and later farms come from the API. |
| Query library | No TanStack Query. Same `useEffect` + session token pattern as breeds. |

### Screens

| URL (Arabic) | English | What you see |
|---|---|---|
| `/admin/farm-settings` | `/en/admin/farm-settings` | Summary cards + searchable farm table |
| same `?status=ACTIVE&planCode=ESSENTIAL` | same | Filters restored from the URL |
| `/admin/farm-settings` as owner | same | Existing platform-forbidden copy |

---

## 2. How the pieces connect

```
Browser  /admin/farm-settings?status=ACTIVE
    │
    ▼
requirePlatformAdmin
    ▼
farm-settings/page.tsx
    farmListQueryFromSearchParams
    listPlatformFarms + getPlatformFarmSummary + listFeatures
    ▼
FarmSettings (client)
    GET /api/v1/platform/farms
    GET /api/v1/platform/farms/summary
    GET /api/v1/platform/farms/{id}   (details dialog)
    401 ──► federated logout
```

---

## 3. File-by-file

#### `apps/web/src/lib/farms.ts` *(new)*

List, summary, and details clients. Allow-lists for status, plan, and sort. URL parse/serialize helpers. Debounce constant `400`.

#### `apps/web/src/app/[locale]/admin/farm-settings/page.tsx` *(changed)*

Reads `searchParams`, loads the page of farms and summary, passes them to the client.

#### `apps/web/src/components/admin/FarmSettings.tsx` *(changed)*

Client table: stats, search, filters, sort, pagination, skeleton, empty, retry, details dialog. Owner lookup stays below the table (PA-004). Add/edit/suspend buttons are present and disabled.

#### `apps/web/src/components/admin/FarmSettings.module.css` *(changed)*

Table, toolbar, status pills, details dialog, skeleton.

#### Messages

`farmSettings.*` list copy, `status.*`, `plan.*`, `sort.*` in `ar.json` / `en.json`.

#### `apps/web/src/lib/farms.test.mjs` *(new)*

Source-lock: API paths, URL helpers, no hardcoded farm names, details enabled, write actions disabled, `requirePlatformAdmin`.

---

## 4. How to test

```powershell
pnpm.cmd --filter @herdcommand/web test
```

Click-test (API must have PA-006):

1. Sign in as `PLATFORM_ADMIN` → `/admin/farm-settings`. Stats and rows come from the API (HARRI should appear if seeded).
2. Search `HARRI` (wait ~400 ms). Filter status `ACTIVE` and plan `ESSENTIAL`. Refresh: the URL still has those params and the same rows.
3. Open details on a row. Close the dialog.
4. Add / Edit / Suspend stay disabled.
5. English `/en/admin/farm-settings` shows English labels. Arabic returns to the unprefixed URL (RTL from the locale layout).
6. Sign in as farm `owner` and open the URL → platform forbidden.

---

## 5. Assumptions and gaps

- The backlog asked for `/platform/farms`. The live landing from PA-001 is `/admin/farm-settings`; changing the path would break post-login redirects.
- French is not a product locale here. Arabic RTL and English LTR are supported.
- Animal counts stay `0` until an animal table exists (PA-006).
- Create wizard is PA-008. Edit / suspend / reactivate are PA-009.

## 6. Definition of done

- [x] Summary cards and farm table load from APIs
- [x] Search debounce, filters, sort, pagination
- [x] URL query restored on refresh
- [x] Loading / empty / retryable error
- [x] Details action works; write actions disabled
- [x] Codes as keys/values; labels localized
- [x] Arabic and English copy
- [x] Source-lock tests
