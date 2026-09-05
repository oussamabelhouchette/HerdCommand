# US-AC-003 — Breed administration UI

This document explains **every file added or changed** for the animal-breed administration screen: what it does, why it exists, and how to test it.

The **API** is already US-AC-002. This story is the React screen that calls that API. There is still **no DELETE**. Statuses and farm groups stay placeholders.

Related: [us-ac-002-breed-management-api.md](./us-ac-002-breed-management-api.md), [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md), [nextjs.md](./nextjs.md), [authentication.md](./authentication.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Route | `/admin` → `/admin/animal-settings` (Arabic default). English: `/en/admin/animal-settings`. |
| Who sees the screen | Keycloak **`owner`** or **`administrator`** (also group path `/administrator` and alias `administrators`). |
| Who does **not** | `manager` can still call the breed API (`BREED_MANAGE`) but gets the forbidden page here. `veterinarian`, `worker`, `accountant` same. |
| Data | Browser calls `/api/v1/admin/animal-breeds` with the session Bearer token and `Accept-Language`. |
| Create / edit | Modal. Code is typed uppercase; disabled when editing (immutable). |
| Soft status | Deactivate asks for confirm; activate is one click. No delete button. |
| Placeholders | Statuses tab, Groups tab, changelog button, and the two extra stat cards. Counts stay `0`. |
| After login | `administrator` / `owner` → `/admin`. Everyone else → `/portal`. |
| API / Flyway | Unchanged. No new backend files. |

### Screens

| URL (Arabic, no prefix) | English | What you see |
|---|---|---|
| `/admin` | `/en/admin` | Immediate redirect to animal settings |
| `/admin/animal-settings` | `/en/admin/animal-settings` | Breed table + create/edit/status |
| `/portal` | `/en/portal` | Farm workspace (non-admin landing) |
| `/admin` as a farm user | same | Forbidden copy + link back to `/portal` |

### Who can do what

| Realm role / group | Lands after login | Sees this UI | Breed API (US-AC-002) |
|---|---|---|---|
| `owner`, `administrator` | `/admin` | yes | list + write |
| `manager` | `/portal` | no (forbidden if they open `/admin`) | list + write |
| `veterinarian`, `worker`, `accountant` | `/portal` | no | list only |

The Spring API still authorizes from **realm roles**, not groups. A Keycloak **group** named `administrator` is enough to open this UI if the access token has a `groups` claim. Breed writes still need the `administrator` or `owner` **role** (or `manager` via API only).

---

## 2. How the pieces connect

```
Browser  /admin/animal-settings
    │
    ▼
middleware.ts
    no session ──► /login?callbackUrl=/admin/animal-settings
    ▼
[locale]/admin/layout.tsx
    requireAdmin(locale)
        no user            ──► /login
        session expired    ──► /api/auth/federated-logout
        not owner/admin    ──► forbidden page (link → /portal)
        allowed            ──► AdminShell + children
    ▼
animal-settings/page.tsx
    listBreeds (server, first paint)
    ▼
BreedManagement (client)
    GET    /api/v1/admin/animal-breeds
    POST   /api/v1/admin/animal-breeds
    PUT    /api/v1/admin/animal-breeds/{id}
    PATCH  /api/v1/admin/animal-breeds/{id}/status
         401 ──► federated logout
         409 BREED_CODE_ALREADY_EXISTS ──► field error on code
         400 fieldErrors ──► under the matching input
    ▼
Spring US-AC-002  (existing)
```

After Keycloak login (generic callback `/`, `/login`, `/signed-in`):

```
signed-in/page.tsx
    session.roles  = realm_access.roles + groups claim
    + GET /api/v1/me roles (when the token works)
    ▼
postLoginHref(memberships)
    administrator / owner ──► /admin  ──► /admin/animal-settings
    anyone else           ──► /portal
```

---

## 3. File-by-file

Paths are relative to the repo root unless noted.

### 3.1 Admin routes

#### `apps/web/src/app/[locale]/admin/layout.tsx` *(new)*

**Why:** Every `/admin/*` page must share one sidebar and one access gate. The public site header (`AppHeader`) is the wrong chrome for this screen.

**What it does:**

1. `requireAdmin(locale)`.
2. If not allowed → forbidden heading, hint, link to `/portal`.
3. If allowed → `AdminShell` around the page.

This layout is **outside** `[locale]/(site)/`, so admin pages do not get the marketing header.

**How to test:** sign in as `manager` (or any non-admin) and open `/admin`. You must see the forbidden page, not the breed table.

#### `apps/web/src/app/[locale]/admin/page.tsx` *(new)*

**Why:** `/admin` is the portal landing. There is no separate dashboard yet.

**What it does:** redirects to `/admin/animal-settings` with the current locale.

**How to test:** after an administrator login, the address bar should settle on `/admin/animal-settings` (or `/en/admin/animal-settings`).

#### `apps/web/src/app/[locale]/admin/animal-settings/page.tsx` *(new)*

**Why:** First paint should already have breed rows. The client component must not flash an empty table while it waits for `useSession`.

**What it does:**

- Calls `requireAdmin` again (layout already gated; this needs the access token).
- `listBreeds` twice in parallel: default list, and `active=true&size=1` so `total` is the “Active breeds” stat.
- On fetch failure, still renders the client with `initialError` (the API may be down).
- Passes `initialPage`, `initialActiveCount`, `initialError` into `BreedManagement`.

**How to test:** start web + API, sign in as administrator, open `/admin/animal-settings`. The table should show existing breeds without a second loading hop. Stop the API and reload: the page still opens and shows the load-error string.

---

### 3.2 Admin chrome

#### `apps/web/src/components/admin/AdminShell.tsx` *(new)*

**Why:** The HTML mock (`herdcommand-admin-config`) uses a dark brand sidebar. Only **System settings** is a real link.

**What it does:**

- Fixed sidebar: brand, nav labels (dashboard / animals / groups / tasks / reports are **not** links), settings → `/admin/animal-settings`.
- Footer: avatar initial from `/api/v1/me` username, “System administrator” label, locale toggle, Sign out.

**How to test:** the settings item is the only clickable nav row. Sign out must return you to Keycloak logout then `/login`.

#### `apps/web/src/components/admin/AdminShell.module.css` *(new)*

**Why:** Admin chrome is not the public `hc-header` layout. Sidebar width is `255px`; `padding-inline-start` keeps RTL correct.

**What it does:** sidebar, nav, avatar, locale buttons, sign-out. Uses `--hc-color-brand-strong` and `--hc-color-accent` from the token package.

#### `apps/web/src/components/admin/AdminLocaleToggle.tsx` *(new)*

**Why:** The public `LanguageSwitcher` lives in `AppHeader`, which this layout does not render.

**What it does:** `ar` / `en` buttons. `router.replace(pathname, { locale })` so you stay on animal settings when switching language.

**How to test:** on `/admin/animal-settings` click English → `/en/admin/animal-settings`. Species labels and buttons must switch. Arabic returns to the unprefixed URL.

#### `apps/web/src/components/admin/AdminIcons.tsx` *(new)*

**Why:** The mock uses small outline icons. Inline SVGs avoid a new icon package.

**What it does:** shared 24×24 stroke icons for the sidebar and the breed toolbar (search, plus, pen, ban, check, …).

---

### 3.3 Breed screen

#### `apps/web/src/components/admin/BreedManagement.tsx` *(new)*

**Why:** All interactive breed behaviour lives in one client component so the server page stays a data loader.

**What it does:**

| UI | Behaviour |
|---|---|
| Search | 300 ms debounce, resets to page 0, calls `listBreeds` with `search` |
| Species / status filters | Immediate reload, page 0 |
| Add breed | Modal: Arabic name, English name, code, species, display order |
| Edit | Same modal; code input **disabled**; PUT does not send `code` |
| Deactivate | Confirm modal, then `PATCH { "active": false }` |
| Activate | Immediate `PATCH { "active": true }` |
| Pagination | Shown when `total > size` (size 20) |
| Changelog | Disabled button |
| Statuses / Groups tabs | Disabled, title “Coming soon” |
| Toast | ~2.2 s after save / activate / deactivate |
| 401 | `window.location` → `/api/auth/federated-logout` |
| 409 `BREED_CODE_ALREADY_EXISTS` | Message + `fieldErrors.code` under the code field |

`useSession()` supplies the Bearer token for later fetches (filters, save). First rows come from the server props.

**How to test:** section 5 (click path). Automated coverage for this component is not a Playwright suite yet; the web unit tests lock the **who-sees-admin** rule (`roles.test.mjs`).

#### `apps/web/src/components/admin/BreedManagement.module.css` *(new)*

**Why:** Match the mock: stats row, tab bar, table, modal overlay. Isolated from `globals.css` so the public site is unchanged.

**What it does:** header, three stat cards, tabs, search/filter toolbar, table, pager, note, modal, toast. Tokens (`--hc-color-*`, `--hc-space-*`, `--hc-radius-*`) for brand colours.

---

### 3.4 API client

#### `apps/web/src/lib/api.ts` *(new)*

**Why:** Every admin call needs the same base URL, Bearer header, `Accept-Language`, query builder, and US-AC-001 error envelope.

**What it does:**

- `apiBaseUrl()` → `NEXT_PUBLIC_API_BASE_URL` or `http://localhost:8080`
- `apiFetch<T>` — JSON body, `cache: 'no-store'`, throws `ApiRequestError` with `{ code, message, fieldErrors }`
- `fieldErrorsMap` — `{ nameAr: "…", code: "…" }` for the modal

**How to test:** stop the API and reload animal settings — `ApiRequestError` / fallback `loadError`. Duplicate a code in the modal — `fieldErrorsMap` fills the code hint from `BREED_CODE_ALREADY_EXISTS`.

#### `apps/web/src/lib/breeds.ts` *(new)*

**Why:** Keep HTTP paths out of the React component.

**What it does:**

| Export | Maps to |
|---|---|
| `listBreeds` | `GET /api/v1/admin/animal-breeds` |
| `createBreed` | `POST` |
| `updateBreed` | `PUT` (names, species, display order — no `code`) |
| `updateBreedStatus` | `PATCH …/status` |
| `SPECIES_CODES` | `SHEEP`, `GOAT`, `CATTLE`, `CAMEL` (same enum as the API) |
| `emptyBreedPage` | Empty first paint when the server fetch failed |

Default list: `page=0`, `size=20`, `sort=displayOrder,asc`.

**How to test:** browser Network tab while using the screen. There is no `DELETE` helper.

---

### 3.5 Who may open `/admin`

#### `apps/web/src/lib/require-admin.ts` *(new)*

**Why:** Layout and the animal-settings page must not each invent a gate.

**What it does:**

1. No session → `/login`.
2. Refresh/session error → federated logout.
3. `GET /api/v1/me` 401 → `/login`.
4. Allowed if `isAdminRole(session.roles)` **or** `isAdminRole(me.roles)`.
5. If `/me` failed but the token already has `administrator` / `owner`, still allow and synthesise a `MeIdentity` from the session (sidebar still has a name).

**How to test:** administrator with a working API → shell + table. `manager` → forbidden. Signed-out visit to `/admin` → `/login`.

#### `apps/web/src/lib/roles.ts` *(changed)*

**Why:** The API treats `manager` as a breed writer. The **screen** must not. Product rule: only system owner / administrator see configuration chrome.

**What it does:** `isAdminRole` is true for `owner`, `administrator`, `administrators`. Names are normalised through `normalizeMembership` so `/administrator` matches.

**How to test:** `apps/web/src/lib/roles.test.mjs` asserts the source contains `owner` and `administrator` and does **not** contain `manager`.

#### `apps/web/src/lib/me.ts` *(existing, used)*

**Why:** `/api/v1/me` is the API’s view of realm roles. Used as a second vote when the session JWT is missing roles.

Unchanged contract: `{ subject, username, email?, roles }`.

---

### 3.6 After-login portals (so the screen is reachable)

These files are not the breed table. They are how an administrator actually lands on `/admin` instead of staying on `/`.

#### `apps/web/src/lib/portals.ts` *(new)*

**Why:** One map from Keycloak group/role → path. Later groups add one row.

```ts
administrator  → /admin
administrators → /admin
owner          → /admin
(default)      → /portal
```

`normalizeMembership` lowercases, strips a leading `/`, and uses the last path segment (`/farm/administrator` → `administrator`).

**How to test:** `portals.test.mjs`, `post-login-redirect.test.mjs`.

#### `apps/web/src/lib/post-login-redirect.ts` *(changed)*

**Why:** Generic callbacks (`/`, `/login`, `/signed-in`, `/portal`) must resolve to the user’s portal. A farm user must not be sent to `/admin` via `callbackUrl`; an admin must not be left on `/portal`.

**What it does:** `postLoginHref(memberships, callbackUrl)` — specific paths like `/me` or `/admin/animal-settings` are kept when they belong to that user.

#### `apps/web/src/lib/realm-roles.ts` *(changed)*

**Why:** Groups are not `realm_access.roles`. Without this, a user who is only in group `administrator` would look like a farm user.

**What it does:** `groupsFromAccessToken` reads the `groups` claim; `membershipsFromAccessToken` unions roles + groups. `auth.ts` stores that union on `session.roles`.

#### `apps/web/src/auth.ts` *(changed)*

**Why:** The signed-in page must not decode the JWT itself on every hop.

**What it does:** on first login, refresh, and “roles empty but token present”, sets `token.roles = membershipsFromAccessToken(...)`. Session copies `roles`.

#### `apps/web/src/app/[locale]/signed-in/page.tsx` *(changed)*

**Why:** Auth.js cannot know “this user is an administrator” until the session exists. This page is the first place that can choose `/admin` vs `/portal`.

**What it does:** merge session roles with `/api/v1/me`, then `redirect(postLoginHref(...))`.

#### `apps/web/src/app/[locale]/(site)/page.tsx` *(changed)*

**Why:** `/` used to show a welcome card. Signed-in users now leave immediately for their portal.

#### `apps/web/src/app/[locale]/(site)/login/page.tsx` *(changed)*

**Why:** Already-signed-in visits to `/login` must go to the same portal, not stay on the Sign in card.

#### `apps/web/src/app/[locale]/(site)/portal/page.tsx` *(new)*

**Why:** Non-admin landing. If an administrator opens `/portal`, they are sent to `/admin`.

#### `apps/web/src/middleware.ts` *(changed)*

**Why:** `/admin` and `/portal` are not public.

**What it does:** same cookie gate as `/me`. No session → `/login?callbackUrl=<path>`.

#### `apps/web/src/components/AppHeader.tsx` *(changed)*

**Why:** “Home” for a signed-in farm user is `/portal`; for an admin it is `/admin`. Guests go to `/login`. The animal-settings nav link only shows for admins.

---

### 3.7 Copy and tests

#### `apps/web/src/messages/ar.json` and `apps/web/src/messages/en.json` *(changed)*

**Why:** The screen is Arabic-first. API field errors already follow `Accept-Language`; chrome and empty states must too.

**Keys added:**

| Prefix | Use |
|---|---|
| `admin.*` | Sidebar labels, forbidden page, “back to farm portal” |
| `animalSettings.*` | Title, table, filters, modal, species names, toasts |
| `nav.animalSettings`, `nav.portal` | Public header (admin link) |
| `portal.title`, `portal.body` | Farm portal card |

**How to test:** toggle `العربية` / `English` on the admin sidebar. Forbidden page as `manager` must be Arabic on `/admin` and English on `/en/admin`.

#### `apps/web/src/lib/roles.test.mjs` *(new)*

Locks the “manager is not admin UI” rule by reading `roles.ts`.

#### `apps/web/src/lib/portals.test.mjs` *(new)*

Locks the `administrator → /admin` and default `/portal` map.

#### `apps/web/src/lib/post-login-redirect.test.mjs` *(changed)*

Generic login paths → portal of the membership list. Cross-portal callbacks are rewritten.

#### `apps/web/src/lib/realm-roles.test.mjs` *(changed)*

Reads a fake JWT `groups` claim and unions it with `realm_access.roles`.

#### `apps/web/package.json` *(changed)*

`scripts.test` includes the new `*.test.mjs` files.

---

## 4. What this story did **not** change

- No new Flyway version. `animal_breed` is still `V2`.
- No new Spring controller, permission, or species table.
- No DELETE in the UI or the API.
- Status configuration and farm groups: tabs only.
- `manager` did **not** gain this screen (API access unchanged).

---

## 5. How to run and test

### 5.1 Automated (web)

From the repo root:

```powershell
pnpm.cmd --filter @herdcommand/web test
```

Expect the locale, roles, portals, post-login redirect, and realm-roles tests to pass.

The API suite from US-AC-002 is still the contract for HTTP:

```powershell
mvn -f apps/api/pom.xml test "-Daether.connector.https.securityMode=insecure"
```

### 5.2 Click-test (the screen)

Need three processes:

| Process | Command | Port |
|---|---|---|
| Keycloak | `.\bin\kc.bat start-dev --http-port=9090` from the Keycloak install | 9090 |
| API | `mvn -f apps/api/pom.xml spring-boot:run "-Daether.connector.https.securityMode=insecure"` | 8080 |
| Web | `pnpm.cmd --filter @herdcommand/web dev` | 3000 |

`NEXT_PUBLIC_API_BASE_URL` must be `http://localhost:8080` (`apps/web/.env.example`).

Use a Keycloak user with realm role **`administrator`** or **`owner`**. A user who is only in group `administrator` needs a **Group Membership** protocol mapper on client `herdcommand` with token claim name `groups`.

1. **Login lands on the screen**

   Open `http://localhost:3000/login`, sign in as administrator. You must end on `/admin/animal-settings`, not `/` or `/login`.

2. **English URL**

   Open `http://localhost:3000/en/login`, sign in. You must end on `/en/admin/animal-settings`.

3. **Farm user does not see the screen**

   Sign in as `manager` / `worker`. You must land on `/portal`. Opening `/admin` shows the forbidden page and “Back to farm portal”.

4. **List + search**

   With at least one breed in the DB (create below, or reuse US-AC-002 curls), type part of the Arabic or English name. After ~300 ms the table filters. Species and Active/Inactive filters must combine with search.

5. **Create**

   Add breed. Example:

   - Arabic name: `نجدي`
   - English name: `Najdi`
   - Code: `najdi` (the input uppercases to `NAJDI`)
   - Species: Sheep
   - Display order: `0`

   Save. Toast “Changes saved”. Row appears. Code in the table is `NAJDI`.

6. **Duplicate code**

   Add again with code `najdi`. Expect the API message for `BREED_CODE_ALREADY_EXISTS` and an error under the code field. No second row.

7. **Edit — code locked**

   Edit Najdi. The code field is disabled. Change the English name and species. Save. Code stays `NAJDI`.

8. **Deactivate / activate**

   Deactivate → confirm dialog mentioning the code → row shows Inactive, toast. Activate → Active again. The row is still in the table (not deleted). Postgres:

   ```sql
   SELECT code, active FROM animal_breed WHERE code = 'NAJDI';
   ```

9. **401 while working**

   Stop Keycloak or expire the session, then change a filter. The browser should go to federated logout, not stay on a broken table.

10. **API down**

    Stop Spring Boot, reload `/admin/animal-settings` as administrator. You still get the admin shell and a load error, not a Next crash.

### 5.3 What you cannot click-test yet

- Changelog
- Statuses tab
- Groups tab
- Species as a configurable catalog (still the four-value enum)
- A `manager` using this UI (by design)

---

## 6. Definition of done (checklist)

- [x] `/admin` and `/admin/animal-settings` exist under the locale layout (ar default, `/en/...` for English)
- [x] Screen is limited to `owner` / `administrator` (not `manager`)
- [x] Signed-in administrator is redirected to `/admin`; others to `/portal`
- [x] Table talks to US-AC-002 (`GET`/`POST`/`PUT`/`PATCH` only)
- [x] Create + edit modal; code immutable on edit; duplicate code shown from `ApiError`
- [x] Deactivate confirm; activate; no DELETE
- [x] Statuses / groups / changelog are visible placeholders only
- [x] Arabic and English copy for the shell and the breed form
- [x] Unit tests for admin-role membership and post-login portal choice
