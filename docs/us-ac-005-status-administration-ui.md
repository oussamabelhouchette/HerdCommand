# US-AC-005 — Status administration UI

This document explains **every file added or changed** for the animal-status administration screen: what it does, why it exists, and how to test it.

The **API** is already US-AC-004. This story is the React Status tab that calls that API. There is still **no POST** and **no DELETE**. There is still **no** animal-list filter UI; after save the tab reloads its own list. Farm groups stay a placeholder.

Related: [us-ac-004-status-configuration-api.md](./us-ac-004-status-configuration-api.md), [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md), [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Route | Same `/admin/animal-settings`. Statuses is a **tab**, not a new URL. |
| Who sees the screen | Unchanged: Keycloak **`owner`** or **`administrator`**. `manager` can still call the status API but gets the forbidden page here. |
| Data | Browser calls `/api/v1/admin/animal-statuses` with the session Bearer token and `Accept-Language`. |
| Create | **None.** The API has no POST. The tab has no Add button. |
| Edit | Modal. Technical code is read-only and is **not** in the PUT body. |
| Color | Picker is the five allowlisted tokens only: `success`, `purple`, `danger`, `warning`, `neutral`. |
| Preview | Badge in the modal updates from the current label + token **before** save. |
| Soft status | Deactivate asks for confirm except **`ACTIVE`**, which is disabled (API would return 409 `STATUS_REQUIRED`). Activate is one click. No delete. |
| Filter flag | `visibleInFilter` checkbox in the modal; column in the table. No animal-filter screen yet to consume it. |
| Warning | Note under the table: labels and colors do not change what the technical code means. |
| After save | Client reloads the status list (and the API evicts `animalStatusActive`). |
| API / Flyway | Unchanged. No new backend files. |

### Screens

| URL (Arabic, no prefix) | English | What you see |
|---|---|---|
| `/admin/animal-settings` | `/en/admin/animal-settings` | Breeds tab (US-AC-003) **and** a working Statuses tab |
| Statuses tab | same URL | Seeded `ACTIVE`, `PREGNANT`, `SICK`, `ISOLATED` + lock on protected codes |

### Who can do what

Same gate as US-AC-003. Writes still need `STATUS_CONFIG_MANAGE` on the API (`owner`, `administrator`, `manager`). The UI is only owner / administrator.

---

## 2. How the pieces connect

```
Browser  /admin/animal-settings
    │
    ▼
animal-settings/page.tsx
    listBreeds + listStatuses (server, first paint)
    ▼
AnimalSettings (client)
    tabs: Breeds | Statuses | Groups (still disabled)
    ▼
StatusManagement
    GET    /api/v1/admin/animal-statuses
    PUT    /api/v1/admin/animal-statuses/{code}
    PATCH  /api/v1/admin/animal-statuses/{code}/status
         401 ──► federated logout
         409 STATUS_REQUIRED ──► form error (ACTIVE)
         400 INVALID_COLOR_TOKEN / fieldErrors ──► under the matching control
    ▼
Spring US-AC-004  (existing)
```

Both tabs stay mounted (`hidden` on the inactive one) so switching tabs does not throw away search or unsaved-filter state.

---

## 3. File-by-file

Paths are relative to the repo root unless noted.

### 3.1 Page and chrome

#### `apps/web/src/app/[locale]/admin/animal-settings/page.tsx` *(changed)*

**Why:** First paint should already have breed rows **and** status rows. The Statuses tab must not flash empty while `useSession` arrives.

**What it does:**

- Still calls `requireAdmin`.
- Loads breeds (list + active count) and statuses in separate try/catch so one API failure does not blank the other tab.
- Renders `AnimalSettings` instead of `BreedManagement` directly.

**How to test:** open `/admin/animal-settings` as administrator. The “Defined statuses” stat should be `4` after V3 seed. Stop the API and reload: the shell still opens; each tab can show its own load error.

#### `apps/web/src/components/admin/AnimalSettings.tsx` *(new)*

**Why:** Header, stats, and tabs belong to the page, not to the breed table. The Statuses tab was a disabled placeholder inside `BreedManagement`.

**What it does:**

- Title, changelog (still disabled), three stat cards (breed active count, **real** status total, groups still `0`).
- Tabs: Breeds and Statuses are clickable; Groups stays “Coming soon”.
- Keeps both panels mounted.

**How to test:** click Statuses — the four seeded rows appear. Click Breeds — the breed table is still there with the same search text.

#### `apps/web/src/components/admin/BreedManagement.tsx` *(changed)*

**Why:** The page chrome moved up. The breed table must still work when it is only the tab body.

**What it does:** `hideChrome` skips header / stats / tabs. `onStatsChange` keeps the page stat card and tab count in sync after create / deactivate.

Standalone use (chrome on) is unchanged.

#### `apps/web/src/components/admin/BreedManagement.module.css` *(changed)*

**Why:** Statuses is a real tab now.

**What it does:** enabled tabs use `cursor: pointer`; disabled Groups tab stays muted. `.embedded` is the breed panel when chrome is hidden.

#### `apps/web/src/components/admin/AdminIcons.tsx` *(changed)*

**Why:** Protected codes need a lock mark next to the technical code.

**What it does:** adds `LockIcon`.

---

### 3.2 Status tab

#### `apps/web/src/components/admin/StatusManagement.tsx` *(new)*

**Why:** All interactive status behaviour lives in one client component, same pattern as breeds.

**What it does:**

| UI | Behaviour |
|---|---|
| Search | 300 ms debounce, page 0, `listStatuses` with `search` |
| Active / inactive filter | Immediate reload |
| Code column | Technical code + lock when `systemProtected` |
| Badge column | Label for the current locale + color token class |
| Filter column | Visible / hidden from `visibleInFilter` |
| Edit | Modal; code input `readOnly` + `disabled`; PUT uses path code only |
| Color picker | One button per `COLOR_TOKENS` value (`role="radio"`) |
| Live preview | `data-testid="status-preview"`; label + token from **form state**, not the saved row |
| `visibleInFilter` / `active` / display order | In the modal |
| `ACTIVE` | Deactivate button and active checkbox are disabled |
| Deactivate (other codes) | Confirm, then `PATCH { "active": false }` |
| Activate | Immediate `PATCH { "active": true }` |
| No Add, no delete | No POST helper is called |
| Toast | ~2.2 s after save / activate / deactivate |
| 401 | `window.location` → `/api/auth/federated-logout` |
| 409 `STATUS_REQUIRED` | Message in the modal |

**How to test:** section 5. Automated coverage is source-lock tests in `statuses.test.mjs` (tokens, no POST/DELETE, read-only code, preview wiring, `ACTIVE` guard).

#### `apps/web/src/components/admin/StatusManagement.module.css` *(new)*

**Why:** Badge colors must be tokens, not free hex from the API. The picker and preview share the same five classes.

**What it does:** `.success` / `.purple` / `.danger` / `.warning` / `.neutral` backgrounds, lock mark, swatches, preview row.

---

### 3.3 API client

#### `apps/web/src/lib/statuses.ts` *(new)*

**Why:** Keep HTTP paths out of the React component.

**What it does:**

| Export | Maps to |
|---|---|
| `listStatuses` | `GET /api/v1/admin/animal-statuses` |
| `updateStatus` | `PUT` (labels, `colorToken`, order, `visibleInFilter`, `active` — **no** `code`) |
| `updateStatusActive` | `PATCH …/status` |
| `COLOR_TOKENS` | The same five tokens as the API |
| `REQUIRED_STATUS_CODE` | `ACTIVE` |
| `isRequiredStatus` / `isAllowedColorToken` / `statusLabel` | UI helpers |
| `emptyStatusPage` | Empty first paint when the server fetch failed |

Default list: `page=0`, `size=20`, `sort=displayOrder,asc`.

**How to test:** browser Network tab while using the Statuses tab. There is no `createStatus` and no `DELETE`.

---

### 3.4 Copy and tests

#### `apps/web/src/messages/ar.json` and `apps/web/src/messages/en.json` *(changed)*

**Why:** Status chrome must be Arabic-first like the breed form.

**Keys added:** `statusLoadError`, `statusSearchPlaceholder`, `statusNote`, `statusEditTitle`, `colorLabel`, `preview*`, `protectedCode`, `cannotDeactivateActive`, `deactivateStatusTitle`, `confirmDeactivateStatus`, `statusDeactivated` / `statusActivated`, `visibleInFilter`, `filterVisible` / `filterHidden`, `colLabel` / `colColor` / `colFilter`, `colorTokens.*`.

**How to test:** sidebar locale toggle on the Statuses tab. The note, modal, and color names must switch. API field errors still follow `Accept-Language`.

#### `apps/web/src/lib/statuses.test.mjs` *(new)*

Locks the allowlist, the absence of create/delete helpers, read-only code in the modal, live-preview wiring, and the `ACTIVE` deactivate disable.

#### `apps/web/package.json` *(changed)*

`scripts.test` includes `statuses.test.mjs`.

---

## 4. What this story did **not** change

- No new Flyway version. Statuses are still `V3`.
- No new Spring controller or permission.
- No POST or DELETE in the UI or the API.
- No animal registration / filter screen (so “hide from filters” is stored and shown, not consumed yet).
- Groups tab and changelog stay placeholders.
- `manager` did **not** gain this screen.

---

## 5. How to run and test

### 5.1 Automated (web)

From the repo root:

```powershell
pnpm.cmd --filter @herdcommand/web test
```

Expect the existing locale / roles / portals tests **plus** `statuses.test.mjs` to pass.

The API suite from US-AC-004 is still the contract for HTTP:

```powershell
mvn -f apps/api/pom.xml test "-Daether.connector.https.securityMode=insecure"
```

### 5.2 Click-test (the screen)

Need three processes (same as US-AC-003). Restart the API after V3 if it was started before US-AC-004.

Use a Keycloak user with realm role **`administrator`** or **`owner`**.

1. **Tab is real**

   Open `/admin/animal-settings`. Click **Statuses**. You must see four rows (`ACTIVE`, `PREGNANT`, `SICK`, `ISOLATED`), not “Coming soon”. The Defined statuses stat is `4`.

2. **Lock + code**

   Each code shows a lock. Edit any row. The technical-code field is disabled. Save after changing only the English label. Network: `PUT /api/v1/admin/animal-statuses/PREGNANT` body has **no** `code` field.

3. **Live preview**

   In the edit modal, change the Arabic label and click **Purple**. The preview badge updates immediately. Cancel. The table badge is unchanged.

4. **Color allowlist only**

   The picker has five tokens. There is no hex input and no “custom color”.

5. **Hide from filters**

   Edit `ISOLATED`, uncheck “Show in animal filters”, save. The In filters column shows Hidden. There is no animal filter dropdown yet to prove consumption; the API row is what later stories will read.

6. **Deactivate / ACTIVE**

   `ACTIVE` deactivate control is disabled. `SICK` deactivate → confirm → Inactive, row still present. Activate again.

7. **Warning**

   The note under the table says labels do not change business behavior.

8. **Breeds still work**

   Switch back to Breeds. Search, add, and edit still work. No delete on either tab.

9. **401 / API down**

   Same as US-AC-003: expired session → federated logout; API down → load error, not a crash.

### 5.3 What you cannot click-test yet

- Changelog
- Groups tab
- An animal filter that hides statuses with `visibleInFilter=false`
- Creating or deleting a status (by design)
- A `manager` using this UI (by design)

---

## 6. Definition of done (checklist)

- [x] Statuses tab on `/admin/animal-settings` loads US-AC-004
- [x] Technical code shown with a protected/lock indicator; read-only in the modal; not submitted
- [x] Color picker is the allowlisted tokens only
- [x] Live badge preview updates from label + token without save
- [x] `visibleInFilter`, `active`, and display order are editable
- [x] No delete; no create; `ACTIVE` cannot be deactivated in the UI
- [x] Warning that labels do not change business behavior
- [x] List reloads after save
- [x] Arabic and English copy
- [x] Source-lock tests for tokens, read-only code, preview, and no POST/DELETE
