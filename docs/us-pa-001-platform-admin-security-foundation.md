# PA-001 — Platform-admin security foundation

This document explains **every file added or changed** for the first farm-management / platform-admin story: what it does, why it exists, and how to test it.

There is **no farm list, create wizard, or membership table** here. Those are PA-002 and later. This story locks `/api/v1/platform/**` and `/admin/farm-settings` to the Keycloak **`PLATFORM_ADMIN`** role, and adds a landing page in the same admin shell as animal settings.

Related: [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md) (shared `ApiError`, JWT resource server), [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md) (admin shell pattern), [nextjs.md](./nextjs.md), [authentication.md](./authentication.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Role | Keycloak realm or client role **`PLATFORM_ADMIN`**. Also accepted as `platform_admin` / `platform-admin`. |
| Who is **not** a platform admin | Farm `owner`, `administrator`, `manager`, and other farm roles. They keep animal-settings access when they already had it. |
| API prefix | `/api/v1/platform/**` requires `hasAuthority('PLATFORM_ADMIN')`. |
| Probe endpoints | `GET /api/v1/platform` (foundation metadata) and `GET /api/v1/platform/farms` (empty `farms` array). No farm records. |
| Errors | Same US-AC-001 envelope: `code`, `message`, `fieldErrors`, `timestamp`, `path`. `401` without a token, `403` without the role. |
| React route | `/admin/farm-settings` (Arabic default). English: `/en/admin/farm-settings`. |
| Who sees the screen | Token / session contains `PLATFORM_ADMIN`. |
| Who sees the nav | Farm-management link is hidden unless `isPlatformAdminRole`. Farm users never see it. |
| Access denied | Authenticated non-platform-admins who open the route see platform-forbidden copy. |
| Keycloak credentials | None in React. No admin-cli, no service-account secret. |
| Flyway / farm schema | Unchanged. PA-002 owns membership tables. |

### Screens

| URL (Arabic, no prefix) | English | What you see |
|---|---|---|
| `/admin/farm-settings` | `/en/admin/farm-settings` | Farm-management landing (stats as placeholders, coming-soon cards) |
| `/admin` as `PLATFORM_ADMIN` only | same | Redirect to farm settings |
| `/admin` as owner / administrator | same | Still animal settings |
| `/admin/farm-settings` as owner (no platform role) | same | Access-denied copy inside the admin shell |
| `/admin` as a farm user | same | Existing forbidden page + link to `/portal` |

### Who can do what

| Realm / client role | Lands after login | Animal settings | Farm-management landing | `/api/v1/platform/**` |
|---|---|---|---|---|
| `PLATFORM_ADMIN` | `/admin/farm-settings` | no | yes | yes |
| `owner`, `administrator` | `/admin/animal-settings` | yes | no (denied if they open the URL) | 403 |
| `manager`, `veterinarian`, `worker`, `accountant` | `/portal` | no | no | 403 |
| No token | `/login` | — | — | 401 |

---

## 2. How the pieces connect

```
Browser  /admin/farm-settings
    │
    ▼
middleware.ts
    no session ──► /login?callbackUrl=/admin/farm-settings
    ▼
[locale]/admin/layout.tsx
    requireAdminShell(locale)
        no user            ──► /login
        not owner/admin
          and not PLATFORM_ADMIN ──► forbidden (link → /portal)
        allowed            ──► AdminShell (Farms nav only if PLATFORM_ADMIN)
    ▼
farm-settings/page.tsx
    requirePlatformAdmin(locale)
        owner/admin without PLATFORM_ADMIN ──► platform-forbidden copy
        allowed ──► GET /api/v1/platform + GET /api/v1/platform/farms
    ▼
Spring
    JwtAuthoritiesConverter
        realm_access.roles + resource_access.*.roles
        RolePermissionMapper: platform_admin → PLATFORM_ADMIN
        owner / manager do **not** get PLATFORM_ADMIN
    ▼
    @PreAuthorize("hasAuthority('PLATFORM_ADMIN')") on /api/v1/platform/**
        no token ──► 401 UNAUTHORIZED
        farm owner ──► 403 FORBIDDEN (no farm payload)
```

---

## 3. File-by-file

Paths are relative to the repo root unless noted.

### 3.1 Backend security

#### `apps/api/src/main/java/com/herdcommand/api/security/Permission.java` *(changed)*

**Why:** Platform APIs must use the same permission enum as animal configuration, not a parallel security model.

**What changed:** added `PLATFORM_ADMIN`.

**How to test:** `JwtAuthoritiesConverterTest` and `PlatformAdminSecurityTest`.

#### `apps/api/src/main/java/com/herdcommand/api/security/RolePermissionMapper.java` *(changed)*

**Why:** `owner` / `administrator` / `manager` used `EnumSet.allOf(Permission.class)`. Adding `PLATFORM_ADMIN` to the enum would have given farm owners every farm.

**What it does:** animal configuration permissions stay explicit. Only the normalized role `platform_admin` maps to `PLATFORM_ADMIN`. Hyphens become underscores (`platform-admin` matches).

**How to test:** converter test `ownerDoesNotReceivePlatformAdmin` and `managerRoleReceivesConfigurationPermissions` (asserts no `PLATFORM_ADMIN`).

#### `apps/api/src/main/java/com/herdcommand/api/security/JwtRoleExtractor.java` *(new)*

**Why:** Platform admin may be a **realm** role or a **client** role. `/me` and the JWT converter must read the same claims.

**What it does:** unions `realm_access.roles` with every `resource_access.<client>.roles`.

**How to test:** `ApiContractTest.meIncludesPlatformAdminClientRole`.

#### `apps/api/src/main/java/com/herdcommand/api/config/JwtAuthoritiesConverter.java` *(changed)*

**Why:** Authorities must include `PLATFORM_ADMIN` when the token has that realm or client role.

**What it does:** feeds extracted roles through `RolePermissionMapper` and the existing `permissions` claim parser.

**How to test:** `JwtAuthoritiesConverterTest` realm + client cases.

#### `apps/api/src/main/java/com/herdcommand/api/config/SecurityConfig.java` *(changed)*

**Why:** `/api/v1/platform/**` must be authenticated at the filter chain; method security then requires the role.

**What changed:** `requestMatchers("/api/v1/platform/**").authenticated()`.

**How to test:** unauthenticated `GET /api/v1/platform/farms` → 401 with the standard envelope.

#### `apps/api/src/main/java/com/herdcommand/api/api/me/MeController.java` *(changed)*

**Why:** The React gate reads `/api/v1/me` roles when the session JWT is thin. A client-only `PLATFORM_ADMIN` must appear there.

**What it does:** `roles` = `JwtRoleExtractor.realmAndClientRoles(jwt)`.

#### `apps/api/src/main/java/com/herdcommand/api/api/platform/PlatformAdminController.java` *(new)*

**Why:** PA-001 needs a real `/api/v1/platform/**` surface for 200/401/403 tests. The farm **list** is PA-006.

**What it does:**

| Method | Path | Body |
|---|---|---|
| `GET` | `/api/v1/platform` | `{ defaultLocale, supportedLocales, permissions: ["PLATFORM_ADMIN"] }` |
| `GET` | `/api/v1/platform/farms` | `{ accessible: true, farms: [] }` |

Class-level `@PreAuthorize("hasAuthority('PLATFORM_ADMIN')")`.

**How to test:** section 5. A farm-owner token must not see a `farms` array of real records (the 403 body is `ApiError` only).

#### `PlatformFoundationResponse.java` / `PlatformFarmsProbeResponse.java` *(new)*

**Why:** Controllers return records, not JPA entities.

---

### 3.2 Tests (API)

#### `apps/api/src/test/java/com/herdcommand/api/security/JwtAuthoritiesConverterTest.java` *(changed)*

Adds `PLATFORM_ADMIN` realm role, client role, and “owner is not platform admin”.

#### `apps/api/src/test/java/com/herdcommand/api/platform/PlatformAdminSecurityTest.java` *(new)*

| Case | Expect |
|---|---|
| No token on `/api/v1/platform/farms` | 401 + `UNAUTHORIZED` envelope |
| JWT with realm role `owner` | 403, no `farms` field |
| Authenticated, no permission | 403 on `/api/v1/platform` |
| `PLATFORM_ADMIN` permission | 200 foundation + empty farms probe |

#### `apps/api/src/test/java/com/herdcommand/api/ApiContractTest.java` *(changed)*

`/me` includes a `PLATFORM_ADMIN` **client** role.

---

### 3.3 Keycloak

#### `infra/keycloak/realm/herdcommand-realm.json` *(changed)*

**Why:** The story asks to configure the role in the realm export.

**What changed:** realm role `PLATFORM_ADMIN`. No service-account secret, no extra user password.

**How to use:** new local imports get the role. An already-running realm needs the role created once in the admin console. See [authentication.md](./authentication.md) §3.5.

---

### 3.4 React route guard and landing

#### `apps/web/src/lib/roles.ts` *(changed)*

**Why:** Animal admin (`owner` / `administrator`) must stay separate from platform admin.

**What it does:** `isPlatformAdminRole` (normalized `platform_admin`), `isAdminShellRole` (either).

**How to test:** `roles.test.mjs` — `PLATFORM_ADMIN` is true; `owner` / `manager` are false.

#### `apps/web/src/lib/require-admin.ts` *(changed)*

**Why:** One session/loadMe path for three gates.

**What it does:** `requireAdmin` (animal UI), `requirePlatformAdmin` (farm landing), `requireAdminShell` (sidebar chrome).

#### `apps/web/src/lib/require-platform-admin.ts` *(new)*

**Why:** The story asked for a dedicated platform-admin route guard. Re-exports `requirePlatformAdmin`.

Frontend checks are **not** the security boundary. The API still returns 401/403.

#### `apps/web/src/lib/portals.ts` *(changed)*

`platform_admin` → `/admin/farm-settings`. Hyphens normalize to underscores so `PLATFORM-ADMIN` matches.

#### `apps/web/src/lib/realm-roles.ts` *(changed)*

Reads `resource_access.*.roles` so a client-scoped `PLATFORM_ADMIN` reaches `session.roles`.

#### `apps/web/src/lib/platform.ts` *(new)*

**Why:** Keep HTTP paths out of the page. Calls only `/api/v1/platform` and `/api/v1/platform/farms`. No Keycloak admin URL.

#### `apps/web/src/app/[locale]/admin/layout.tsx` *(changed)*

Uses `requireAdminShell`. Sidebar shows **Farms** only when `isPlatformAdminRole`. Animal settings stay for `owner` / `administrator`.

#### `apps/web/src/app/[locale]/admin/page.tsx` *(changed)*

Platform-admin-only users go to `/admin/farm-settings`. Everyone else who can open `/admin` still goes to animal settings.

#### `apps/web/src/app/[locale]/admin/farm-settings/page.tsx` *(new)*

**Why:** First farm-management landing, same idea as `/admin/animal-settings`.

**What it does:** `requirePlatformAdmin`, then probes the two platform endpoints. On denial, access-denied copy. On API down, the landing still renders with `loadError`.

#### `apps/web/src/components/admin/FarmSettings.tsx` + `FarmSettings.module.css` *(new)*

**Why:** Match the animal-settings chrome (header, stats, panel) and the HTML mock title, without inventing farm rows.

**What it does:** Arabic-first copy, disabled “Add farm”, placeholder stats (`—`), coming-soon cards. No hardcoded farms, plans, or feature flags.

#### `apps/web/src/components/admin/AdminShell.tsx` *(changed)*

Farms nav link + active state. Platform-only users see “Platform administrator” in the footer.

#### `apps/web/src/components/admin/AdminIcons.tsx` *(changed)*

`FarmIcon` for the sidebar and landing stats.

#### `apps/web/src/components/AppHeader.tsx` *(changed)*

Public header shows Farm management **only** for `PLATFORM_ADMIN`. Ordinary farm users do not see it.

#### `apps/web/src/messages/ar.json` and `en.json` *(changed)*

`admin.farms`, `admin.platformForbidden*`, `farmSettings.*`, `nav.farmSettings`.

---

### 3.5 Web tests

| File | Locks |
|---|---|
| `roles.test.mjs` | `PLATFORM_ADMIN` yes; `owner` / `manager` no |
| `portals.test.mjs` | `platform_admin` → `/admin/farm-settings` |
| `post-login-redirect.test.mjs` | platform admin landing; manager cannot callback into farm-settings |
| `realm-roles.test.mjs` | client-role `PLATFORM_ADMIN` |
| `platform.test.mjs` | client talks only to `/api/v1/platform`, no Keycloak admin |

`apps/web/package.json` `scripts.test` includes `platform.test.mjs`.

---

## 4. What this story did **not** change

- No new Flyway version. The existing `farm` table from US-AC-006 is still the animal-group tenant, not the PA-002 membership model.
- No farm create/edit/suspend APIs (PA-005 / PA-006 / PA-009).
- No Keycloak Admin API, invitations, or service-account credentials (PA-004).
- No feature catalog (PA-003).
- No farm table or wizard on the React page (PA-007 / PA-008).
- French locale was **not** added. The app stays Arabic-first + English, same as US-AC-001.

---

## 5. How to run and test

### 5.1 Automated

From the repo root:

```powershell
pnpm.cmd --filter @herdcommand/web test
mvn -f apps/api/pom.xml test "-Daether.connector.https.securityMode=insecure"
```

Expect the new platform security tests and the role/portal/route-guard tests to pass. Existing animal-config tests must still pass (`manager` still has breed permissions, not `PLATFORM_ADMIN`).

### 5.2 HTTP (API)

Need the API on **8080** and a Keycloak access token.

```powershell
# no token
curl.exe -i http://localhost:8080/api/v1/platform/farms
# expect 401, code UNAUTHORIZED

# farm owner / manager token
curl.exe -i -H "Authorization: Bearer <owner-or-manager-token>" http://localhost:8080/api/v1/platform/farms
# expect 403, code FORBIDDEN, no farm list

# PLATFORM_ADMIN token
curl.exe -i -H "Authorization: Bearer <platform-admin-token>" http://localhost:8080/api/v1/platform
curl.exe -i -H "Authorization: Bearer <platform-admin-token>" http://localhost:8080/api/v1/platform/farms
# expect 200; farms is []
```

### 5.3 Click-test (the landing)

| Process | Port |
|---|---|
| Keycloak | 9090 |
| API | 8080 |
| Web | 3000 |

Assign realm role **`PLATFORM_ADMIN`** to a Keycloak user (see [authentication.md](./authentication.md) §3.5).

1. Sign in as that user. You must land on `/admin/farm-settings`. The sidebar **Farms** link is visible. Animal settings is hidden unless they also have `owner` / `administrator`.
2. English: `/en/login` → `/en/admin/farm-settings`. Title and buttons switch language. Arabic returns to the unprefixed URL.
3. Sign in as `manager` / farm `owner`. You land on `/portal` or animal settings. Opening `/admin/farm-settings` shows access denied (or the existing admin forbidden page). The public header has no Farm management link.
4. With the API down, reload farm settings as `PLATFORM_ADMIN`. The shell and landing still open; you see the load-error string, not a Next crash.

### 5.4 What you cannot click-test yet

- Farm list, search, filters
- Add farm wizard
- Edit / suspend / reactivate
- Feature catalog and audit history

---

## 6. Definition of done (checklist)

- [x] `PLATFORM_ADMIN` mapped from Keycloak realm or client role using the existing JWT converter
- [x] `/api/v1/platform/**` requires that authority
- [x] 401 without a token, 403 for a farm owner, 200 for a platform admin
- [x] Standard `ApiError` envelope reused (no new error JSON)
- [x] `/admin/farm-settings` landing exists (Arabic default, `/en/...` for English)
- [x] Route guard + access-denied page for authenticated non-platform-admins
- [x] Farm-management nav hidden from ordinary farm users
- [x] No Keycloak administrator credentials in React
- [x] JWT mapping unit tests + controller 200/401/403 tests + React route-guard tests
