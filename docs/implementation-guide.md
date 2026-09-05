# HerdCommand — implementation guide

This document is the map of **what is implemented today**, **why each piece exists**, and **how the parts work together**. Use it to rebuild the same system by hand.

Related files: [authentication.md](./authentication.md) (OIDC details), [nextjs.md](./nextjs.md) (every Next.js file and entry point).

Figma login is **not** pixel-perfect yet. Leave that. The theme is a first branded version of Keycloak’s login page.

---

## 1. What we are building (product)

HerdCommand is an Arabic-first livestock farm platform:

| Surface | Role |
|---|---|
| **Web** (`apps/web`) | Dashboard. User clicks Sign in here. |
| **Keycloak** (your machine, port **9090**) | Username, password, tokens, SSO. |
| **Postgres (`keycloakDB`)** | Keycloak’s user store only. The Next.js app never talks to it. |
| **API** (`apps/api`) | Spring Boot. Later: farm data. Today: `/api/v1/me` behind a JWT. |
| **Mobile** (`apps/mobile`) | Expo stub with OIDC PKCE. Not required for web login. |

**Rule that never changes:** the password is typed only on Keycloak. HerdCommand never collects the password.

---

## 2. Picture of how it fits together

```
Browser
  │
  ├─ http://localhost:3000     Next.js (pages, cookies, Auth.js)
  │         │
  │         ├─ OIDC (code + PKCE, refresh, logout)
  │         ▼
  ├─ http://localhost:9090     Keycloak realm herdcommand
  │         │
  │         └─ JDBC ──────────► Postgres keycloakDB
  │
  └─ http://localhost:8080     Spring Boot (optional)
            ▲
            └── Bearer access_token (same Keycloak issuer)
```

**Who does what**

| Piece | Job | Does **not** do |
|---|---|---|
| Next.js `/login` | Button “go authenticate”. Arabic UI, locale. | Password form. |
| Auth.js (`/api/auth/*`) | Start login, handle callback, store tokens in a cookie, refresh, clear cookie. | User database. |
| Keycloak | Prove who the user is. Issue access / id / refresh tokens. Theme for the password page. | Farm business data. |
| Session cookie | Keep the user “logged into HerdCommand” for up to 30 minutes. | Replace Keycloak SSO by itself. |
| Spring Boot | Trust the **access token**. Return `/me`. | Login UI. |
| Keycloak theme | Make the password page look like HerdCommand. | Change the OIDC protocol. |

---

## 3. What is implemented vs not

**Done (you can run it)**

1. Monorepo: web, mobile, API, design tokens.
2. Arabic default (`ar`), English (`en`), RTL/LTR.
3. Web login / logout / session via Keycloak (public client `herdcommand`, PKCE).
4. Access-token refresh from Keycloak’s `expires_in` (not hardcoded to 1 minute).
5. Keycloak login theme in the repo, copied to `C:\Users\Lenovo\Documents\keycloak-26.7.2\themes\herdcommand`.
6. Spring Boot JWT resource server + `GET /api/v1/me` (needs API running + Bearer token).
7. **US-AC-001 configuration foundation** (Flyway, JPA audit fields, admin permissions, standard `ApiError`, `Accept-Language` ar/en). File-by-file notes: [us-ac-001-configuration-foundation.md](./us-ac-001-configuration-foundation.md).
8. **US-AC-002 breed management API** (Flyway V2 `animal_breed`, list/create/update/status, no DELETE, no React screens). File-by-file notes: [us-ac-002-breed-management-api.md](./us-ac-002-breed-management-api.md).
9. **US-AC-003 breed administration UI** (`/admin/animal-settings`, owner/administrator only, farm users land on `/portal`). File-by-file notes: [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md).
10. **US-AC-004 status configuration API** (Flyway V3 `animal_status_definition`, seed ACTIVE/PREGNANT/SICK/ISOLATED, no POST/DELETE, no React screens). File-by-file notes: [us-ac-004-status-configuration-api.md](./us-ac-004-status-configuration-api.md).
11. **US-AC-005 status administration UI** (Statuses tab on `/admin/animal-settings`, color-token picker, live badge preview, no create/delete). File-by-file notes: [us-ac-005-status-administration-ui.md](./us-ac-005-status-administration-ui.md).
12. **US-AC-006 farm group API** (Flyway V4 `farm` + `animal_group`, seed HARRI, no animals/assignment). File-by-file notes: [us-ac-006-farm-group-api.md](./us-ac-006-farm-group-api.md).
13. **US-AC-007 farm group administration UI** (Groups tab, create/edit/deactivate, first seeded farm). File-by-file notes: [us-ac-007-farm-group-administration-ui.md](./us-ac-007-farm-group-administration-ui.md).
14. **PA-001 platform-admin security foundation** (`PLATFORM_ADMIN` JWT mapping, `/api/v1/platform/**`, `/admin/farm-settings` landing, farm owners get 403). File-by-file notes: [us-pa-001-platform-admin-security-foundation.md](./us-pa-001-platform-admin-security-foundation.md).
15. **PA-002 farm and membership database foundation** (Flyway V6 tenant columns, `farm_membership`, `farm_subscription`, generated `FARM-TN-####` codes, owner-required activation, optimistic lock). File-by-file notes: [us-pa-002-farm-and-membership-database-foundation.md](./us-pa-002-farm-and-membership-database-foundation.md).
16. **PA-003 dynamic feature catalog** (Flyway V7 `feature_catalog` + `farm_feature`, `ANIMAL_MANAGEMENT`, `GET /api/v1/platform/features`, coming-soon not enableable). File-by-file notes: [us-pa-003-dynamic-feature-catalog.md](./us-pa-003-dynamic-feature-catalog.md).
17. Mobile OIDC helper (client id `herdcommand-mobile`) — stub.

**Not done (later product)**

Animals, assignment, Keycloak owner lookup (PA-004), farm create/list/wizard (PA-005+), health, Figma-perfect login, AWS, production HTTPS, real `AUTH_SECRET`.

---

## 4. Rebuild by hand (order)

Do this in order. Each step has a **why**.

### Step A — Keep Keycloak as the identity server

**Why:** One place for users and passwords. Web, mobile, and API all trust the same issuer.

Your Keycloak is already installed (not Docker in this repo):

- URL: `http://localhost:9090`
- Realm: `herdcommand`
- Web client: `herdcommand` (public, **no secret**)
- Issuer: `http://localhost:9090/realms/herdcommand`

**Must be true on the client `herdcommand`:**

| Setting | Value | Why |
|---|---|---|
| Standard flow | On | Authorization Code login. |
| Client authentication | Off | Public browser app. |
| PKCE | S256 | Stops stolen authorization codes. |
| Valid redirect URIs | `http://localhost:3000/api/auth/callback/keycloak` and `http://localhost:3000/*` | Keycloak will only send the user back here. |
| Web origins | `http://localhost:3000` | Browser CORS for token/session calls from the app origin. |
| Valid post-logout URIs | `http://localhost:3000/login` and `http://localhost:3000/*` | After logout, user returns to the app. |

Access token lifespan is set **in Keycloak** (you used ~5 minutes). The app reads `expires_in`. It does not hardcode 1 minute.

### Step B — Repo layout

**Why:** Web, API, and mobile share tokens and conventions without becoming one giant app.

```
HerdCommand/
  apps/web          Next.js 15 + Auth.js + next-intl
  apps/api          Spring Boot 3
  apps/mobile       Expo
  packages/tokens   Colors, spacing, type (CSS variables)
  infra/keycloak    Theme + sample realm JSON (your live realm is already on 9090)
  docker-compose.yml   Optional Postgres for **future app data** (not Keycloak)
  docs/             This guide
```

`pnpm-workspace.yaml` includes web, mobile, tokens. The API is Maven, not pnpm.

### Step C — Design tokens

**Why:** Same greens/sand as the product, including the Keycloak CSS.

File: `packages/tokens/src/tokens.css`  
Keycloak copy of colors: `infra/keycloak/theme/herdcommand/login/resources/css/login.css`

### Step D — Web app (Next.js)

**Why:** This is the product UI. Auth.js is the OIDC **client** (BFF): the browser talks to Next.js; Next.js talks to Keycloak for tokens.

Create `apps/web/.env.local` (never commit secrets):

```
AUTH_SECRET=<32+ random characters>
AUTH_TRUST_HOST=true
AUTH_KEYCLOAK_ID=herdcommand
AUTH_KEYCLOAK_SECRET=
AUTH_KEYCLOAK_ISSUER=http://localhost:9090/realms/herdcommand
NEXTAUTH_URL=http://localhost:3000
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

Empty `AUTH_KEYCLOAK_SECRET` is required: the client is public. Auth.js uses `token_endpoint_auth_method: none`.

Start (Windows if `pnpm` is blocked):

```powershell
pnpm.cmd dev:web
```

Open `http://localhost:3000` (Arabic default).

### Step E — Keycloak theme (optional branding)

**Why:** The password page is Keycloak’s HTML. CSS/FTL cannot live in Next.js.

1. Source of truth in git: `infra/keycloak/theme/herdcommand`
2. **No build.** Copy the folder to `...\keycloak-26.7.2\themes\herdcommand`
3. Realm settings → Themes → Login = `herdcommand`
4. Restart Keycloak so it reloads files

### Step F — Spring Boot (when you need APIs)

**Why:** The UI is not the security boundary. The API checks the **access token**.

```powershell
mvn -f apps/api/pom.xml spring-boot:run
```

Issuer must match Keycloak. `/api/v1/me` needs header `Authorization: Bearer <access_token>`.

---

## 5. Functionality, file by file

### 5.1 Locales (Arabic first)

| File | Function |
|---|---|
| `apps/web/src/i18n/routing.ts` | Locales `ar`, `en`. Default `ar`. Cookie `HERDCOMMAND_LOCALE`. No browser auto-detect. |
| `apps/web/src/middleware.ts` | next-intl + Auth.js. Protects `/me`. |
| `apps/web/src/messages/ar.json` / `en.json` | UI strings. |
| `apps/web/src/app/[locale]/layout.tsx` | `lang` + `dir` (rtl/ltr), fonts, session provider. |

**Together:** `/` is Arabic. `/en/...` is English. Keycloak locale is separate; the theme also supports `ar` and `en`.

### 5.2 Login

| File | Function |
|---|---|
| `apps/web/src/app/[locale]/login/page.tsx` | If already signed in, go home. Else show Sign in. |
| `apps/web/src/components/SignInButton.tsx` | Client `signIn('keycloak')` so the **browser** navigates to Keycloak (not an RSC fetch, which caused CORS noise). |
| `apps/web/src/auth.ts` | Keycloak provider, PKCE, JWT session. |
| `apps/web/src/app/api/auth/[...nextauth]/route.ts` | Auth.js HTTP API: signin, callback, session, csrf. |

**Flow**

1. User clicks Sign in on `:3000`.
2. Browser goes to Keycloak `/protocol/openid-connect/auth` with `code_challenge` (PKCE).
3. User types password on the **themed** Keycloak page.
4. Keycloak redirects to `http://localhost:3000/api/auth/callback/keycloak?code=...`.
5. Auth.js POSTs to Keycloak token endpoint with `code` + `code_verifier` (no client secret).
6. Auth.js puts `access_token`, `id_token`, `refresh_token`, `expiresAt` into an encrypted cookie (`authjs.session-token`, sometimes split `.0` / `.1`).
7. User lands on home. `auth()` reads the cookie.

### 5.3 Session and refresh

| File | Function |
|---|---|
| `apps/web/src/auth.ts` `jwt` callback | On every session read: if access token is within **15 seconds** of expiry, refresh. |
| `apps/web/src/lib/refresh-access-token.ts` | `POST .../token` with `grant_type=refresh_token`. Uses Keycloak `expires_in`. Dedupes parallel calls (Keycloak **rotates** refresh tokens). |
| `apps/web/src/middleware.ts` | `auth()` wrapper can **Set-Cookie** on navigations (RSC `auth()` cannot persist cookies). |
| `apps/web/src/components/AuthSessionProvider.tsx` | Every **20 seconds** hits `/api/auth/session` so refresh can run while the user sits on a page. |

**Why refresh exists:** Access tokens are short (Keycloak). The session cookie is 30 minutes. Without refresh, API calls would 401 while the user still looks “logged in”.

**Why not 1 minute in code:** `expiresAt` comes from Keycloak. Change Access Token Lifespan in the realm; the logs will show a new remaining time (for example `299 s`).

**If refresh fails** (`invalid_grant`, session ended in admin console): `session.error = RefreshAccessTokenError` → pages redirect to federated logout.

### 5.4 Logout

Two ways, same idea: **delete the app cookie** and **end Keycloak SSO**.

| File | Function |
|---|---|
| `apps/web/src/lib/logout.ts` | Server action from the Sign out **button**. `signOut` then redirect to Keycloak logout URL. |
| `apps/web/src/app/api/auth/federated-logout/route.ts` | GET used when the session is already broken (cannot call `signOut` during RSC render). |
| `apps/web/src/lib/keycloak-logout-url.ts` | Builds `/protocol/openid-connect/logout?client_id&post_logout_redirect_uri&id_token_hint`. |

If you only cleared the Next.js cookie, the next Sign in would **auto-login** (Keycloak SSO cookie still valid).

### 5.5 Account page and API

| File | Function |
|---|---|
| `apps/web/src/app/[locale]/me/page.tsx` | Calls `GET {API}/api/v1/me` with `session.accessToken`. |
| `apps/api/.../MeController.java` | Reads subject, username, email, realm roles from the JWT. |
| `apps/api/.../SecurityConfig.java` | Stateless. JWT issuer = Keycloak. CORS allows `:3000`. |

**Together:** Web is logged in via cookie. API is logged in via **Bearer**. Same Keycloak access token.

If the API is down, `/me` shows an error. Login still works.

### 5.6 Keycloak theme

| Path | Function |
|---|---|
| `login/theme.properties` | Parent `keycloak.v2` (Keycloak 26). Our CSS on top of PatternFly. |
| `login/template.ftl` | Layout: language pills, logo mark, card, footer. |
| `login/resources/css/login.css` | Figma-inspired colors (not exact Figma). |
| `login/messages/messages_ar.properties` | Arabic strings on the Keycloak page. |
| `login/footer.ftl` | Copyright line. |

Google / Nafath / register were **not** implemented: they are not configured on the realm.

### 5.7 Mobile (stub)

`apps/mobile/src/auth.ts` — Expo Auth Session + PKCE, client `herdcommand-mobile`. Tokens in SecureStore. A physical phone cannot reach `localhost:9090` without a tunnel.

---

## 6. Cookies and tokens (what lives where)

| Name | Where | Lifetime | Contains |
|---|---|---|---|
| `authjs.session-token` | Browser, HttpOnly | 30 min (`session.maxAge`) | Encrypted JWT: user + access + id + refresh + `expiresAt` |
| `HERDCOMMAND_LOCALE` | Browser | 1 year | `ar` or `en` |
| Keycloak SSO cookie | Browser, Keycloak domain `:9090` | Realm SSO settings | Keycloak login session |
| Access token | Inside session JWT | Keycloak Access Token Lifespan | Sent to Spring as Bearer |
| Refresh token | Inside session JWT | Keycloak refresh lifespan | Used only by the Next.js **server** to get new access tokens |

The browser **Network** tab does **not** show refresh. Watch the **Next.js terminal** for `[HerdCommand] token refresh`.

---

## 7. How to run the implemented stack

1. Start **Keycloak** (your usual command), port **9090**, theme `herdcommand` if you want branding.
2. `pnpm.cmd install` then `pnpm.cmd dev:web`.
3. Optional: Spring Boot on **8080**.
4. Optional: `pnpm.cmd dev:mobile`.

Do **not** start a second Keycloak. `docker-compose.yml` Postgres is for later app data, not for this login.

---

## 8. Typical problems (and which layer)

| Symptom | Layer | What to check |
|---|---|---|
| `Invalid parameter: redirect_uri` | Keycloak client | Redirect URI list. |
| `Invalid parameter: post_logout_redirect_uri` | Keycloak client | Post-logout URI list. |
| Login works, `/me` 401 | API or token | API running? Issuer URL identical? Bearer sent? |
| Theme still default | Keycloak | Theme name on realm + restart after copy. |
| `invalid_grant` on refresh | Keycloak session | Session ended, or refresh token already used (rotation) without cookie update. |
| Hydration / Grammarly on `<body>` | Browser extension | Not the app. |
| CORS on Keycloak **authorize** URL | Next trying to fetch Keycloak as RSC | Use client `signIn` (already in `SignInButton`). Login still continues via full navigation. |

---

## 9. Suggested order when you continue by hand

1. Keep login/refresh/logout as they are.
2. Turn on Spring Boot and prove `/me` with a real token.
3. Add the first farm APIs the same way: Bearer JWT, OpenAPI, error envelope already sketched in the API.
4. Revisit Keycloak CSS against Figma when you want pixel match.
5. Finish mobile OIDC against a reachable Keycloak URL.

---

## 10. Source files (quick index)

| Concern | Start here |
|---|---|
| OIDC client | `apps/web/src/auth.ts` |
| Refresh | `apps/web/src/lib/refresh-access-token.ts` |
| Logout URL | `apps/web/src/lib/keycloak-logout-url.ts` |
| Sign in button | `apps/web/src/components/SignInButton.tsx` |
| Middleware | `apps/web/src/middleware.ts` |
| Env example | `apps/web/.env.example` |
| API JWT + method security | `apps/api/src/main/java/com/herdcommand/api/config/SecurityConfig.java` |
| API me | `apps/api/src/main/java/com/herdcommand/api/api/me/MeController.java` |
| API configuration foundation | `docs/us-ac-001-configuration-foundation.md` |
| API breed management | `docs/us-ac-002-breed-management-api.md` |
| Breed administration UI | `docs/us-ac-003-breed-administration-ui.md` |
| API status configuration | `docs/us-ac-004-status-configuration-api.md` |
| Status administration UI | `docs/us-ac-005-status-administration-ui.md` |
| API farm groups | `docs/us-ac-006-farm-group-api.md` |
| Farm group administration UI | `docs/us-ac-007-farm-group-administration-ui.md` |
| Platform-admin security + farm landing | `docs/us-pa-001-platform-admin-security-foundation.md` |
| Farm + membership database foundation | `docs/us-pa-002-farm-and-membership-database-foundation.md` |
| Dynamic feature catalog | `docs/us-pa-003-dynamic-feature-catalog.md` |
| Theme | `infra/keycloak/theme/herdcommand/` |
| Tokens | `packages/tokens/src/tokens.css` |
