# Next.js app (`apps/web`) — files and entry points

This is the HerdCommand **web dashboard**. It runs on **port 3000**.

It does **not** store users. It starts Keycloak login, keeps a session cookie, and later can call Spring Boot with the access token.

---

## 1. How to start (the real entry)

From the repo root:

```powershell
pnpm.cmd --filter @herdcommand/web dev
```

or `pnpm.cmd dev:web`.

That runs `next dev --port 3000` (`apps/web/package.json` → `scripts.dev`).

**Config Next loads first**

| File | Role |
|---|---|
| `apps/web/package.json` | App name, scripts, dependencies (Next 15, Auth.js, next-intl). |
| `apps/web/next.config.mjs` | Next config. Wraps next-intl plugin (`./src/i18n/request.ts`). Transpiles `@herdcommand/tokens`. |
| `apps/web/tsconfig.json` | TypeScript. `@/*` → `./src/*`. |
| `apps/web/.env.local` | Secrets and Keycloak URLs (not in git). Copy from `.env.example`. |
| `apps/web/.eslintrc.json` | Lint. |

There is **no** `src/index.tsx`. Next.js App Router uses the `src/app` folder plus **middleware**.

---

## 2. Request entry points (what runs first)

Every browser request hits **one of these first**, then a page or a route.

```
Browser
   │
   ├─ Pages (/ , /login, /me, /en/...)
   │     → middleware.ts
   │     → app/layout.tsx
   │     → app/[locale]/layout.tsx
   │     → app/[locale]/.../page.tsx
   │
   └─ Auth APIs (/api/auth/...)
         → NOT middleware (matcher skips `api`)
         → app/api/auth/.../route.ts
```

### Entry A — `src/middleware.ts` (pages only)

**This is the first code for HTML pages.** Matcher skips `api`, `_next`, files with a dot.

What it does:

1. Runs Auth.js `auth()` so the session JWT can refresh and **Set-Cookie** (pages themselves cannot always save cookies).
2. If the path is `/me`, `/admin`, or `/portal` (including `/ar/…` and `/en/…`) and there is no session → redirect to `/login`.
3. Runs **next-intl**: add locale prefix if needed, set `HERDCOMMAND_LOCALE`.

### Entry B — `src/app/api/auth/[...nextauth]/route.ts`

**This is the first code for login/callback/session.** Catch-all `GET`/`POST` for:

- `/api/auth/signin`
- `/api/auth/callback/keycloak`
- `/api/auth/session`
- `/api/auth/csrf`
- `/api/auth/signout`

Handlers come from `src/auth.ts`.

### Entry C — `src/app/api/auth/federated-logout/route.ts`

**GET only.** Used when the session is already bad (expired refresh). Clears the cookie, then redirects the browser to Keycloak logout.

### Entry D — pages under `src/app/[locale]/`

After middleware + layouts, Next renders the matching `page.tsx`.

| URL (Arabic default, no prefix) | File |
|---|---|
| `/` | `src/app/[locale]/(site)/page.tsx` (signed-in users redirect to their portal) |
| `/login` | `src/app/[locale]/(site)/login/page.tsx` |
| `/portal` | `src/app/[locale]/(site)/portal/page.tsx` |
| `/me` | `src/app/[locale]/(site)/me/page.tsx` |
| `/design-system` | `src/app/[locale]/(site)/design-system/page.tsx` |
| `/admin` | `src/app/[locale]/admin/page.tsx` → animal settings, or farm settings if `PLATFORM_ADMIN` only |
| `/admin/animal-settings` | `src/app/[locale]/admin/animal-settings/page.tsx` |
| `/admin/farm-settings` | `src/app/[locale]/admin/farm-settings/page.tsx` (PA-001 landing + PA-003 catalog) |
| `/signed-in` | `src/app/[locale]/signed-in/page.tsx` (post-login hop) |
| `/en`, `/en/login`, `/en/admin`, … | Same files, `locale = en` |

Breed admin screen (who can open it, file-by-file, click-test): [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md). Status tab: [us-ac-005-status-administration-ui.md](./us-ac-005-status-administration-ui.md). Groups tab: [us-ac-007-farm-group-administration-ui.md](./us-ac-007-farm-group-administration-ui.md). Farm-management landing (PA-001): [us-pa-001-platform-admin-security-foundation.md](./us-pa-001-platform-admin-security-foundation.md).

---

## 3. Layouts (shell around every page)

| File | Entry? | What it is |
|---|---|---|
| `src/app/layout.tsx` | Root layout | Required `<html>` / `<body>`. Fonts, `lang`/`dir` from `getLocale()` (`rtl` for `ar`). Token CSS + `globals.css`. |
| `src/app/[locale]/layout.tsx` | Locale layout | next-intl messages + `AuthSessionProvider`. No second `<html>`/`<body>`. |
| `src/app/globals.css` | — | Page layout, header, login, alerts. Uses `--hc-*` from tokens. |

---

## 4. Auth (heart of login)

| File | What it is |
|---|---|
| `src/auth.ts` | **Auth.js setup (the OIDC client).** Exports `handlers`, `auth`, `signIn`, `signOut`. Keycloak provider, PKCE, JWT session 30 min, `jwt` callback (save tokens + refresh), `session` callback (expose access/id token to the app). |
| `src/lib/refresh-access-token.ts` | Talks to Keycloak token URL with `grant_type=refresh_token`. Uses Keycloak `expires_in`. Dedupes parallel refresh. Helpers: `accessTokenNeedsRefresh` (15s early), `isAuthSessionError`. |
| `src/lib/logout.ts` | Server action for the **Sign out button**. `signOut` then redirect to Keycloak end-session URL. |
| `src/lib/keycloak-logout-url.ts` | Builds that Keycloak logout URL (`client_id`, `post_logout_redirect_uri`, `id_token_hint`). |
| `src/components/SignInButton.tsx` | Client button. Calls `signIn('keycloak')` so the **browser** goes to Keycloak. |
| `src/components/AuthSessionProvider.tsx` | `SessionProvider`, refetch every 20s so refresh can run without a full reload. |

**Call graph**

- Button Sign in → `SignInButton` → Auth.js → Keycloak → callback route → `auth.ts` jwt (first time, `account` set) → cookie → home.
- Later page load → middleware `auth()` and/or `auth()` in page → `auth.ts` jwt (check expiry) → maybe `refresh-access-token.ts`.
- Sign out → `logout.ts` → cookie gone → Keycloak logout → `/login`.

---

## 5. Pages (screens)

| File | What the user sees | What it calls |
|---|---|---|
| `src/app/[locale]/(site)/page.tsx` | Signed-in users redirect to `/admin` or `/portal`. Guests → `/login`. | `auth()`, `postLoginHref` |
| `src/app/[locale]/(site)/login/page.tsx` | “Continue to Keycloak” card. No password fields. | `auth()`, `SignInButton` |
| `src/app/[locale]/(site)/portal/page.tsx` | Farm workspace after login for non-admins. | `auth()`, `logoutAction` |
| `src/app/[locale]/(site)/me/page.tsx` | Account: calls Spring `GET /api/v1/me` with Bearer token. | `auth()`, `fetch` API |
| `src/app/[locale]/(site)/design-system/page.tsx` | Button / Input / Card / Badge samples. | No auth required |
| `src/app/[locale]/admin/animal-settings/page.tsx` | Breed, status, and group administration (US-AC-003 / 005 / 007). | `requireAdmin`, breed + status + farm/group APIs |
| `src/app/[locale]/admin/farm-settings/page.tsx` | Platform farm-management landing. Feature cards from the catalog API (PA-003). No farm list yet. | `requirePlatformAdmin`, `/api/v1/platform`, `/api/v1/platform/features` |

---

## 6. UI components

| File | What it is |
|---|---|
| `src/components/AppHeader.tsx` | Top bar: brand, Home (portal or admin), Design system, Account, language, Sign in/out. Uses `auth()`. |
| `src/components/admin/AdminShell.tsx` | Dark admin sidebar. System settings and Farms are real links when the user has those roles. |
| `src/components/admin/FarmSettings.tsx` | Farm-management landing: placeholder stats and coming-soon cards. |
| `src/components/admin/AnimalSettings.tsx` | Animal-settings chrome: stats and Breeds / Statuses / Groups tabs. |
| `src/components/admin/BreedManagement.tsx` | Breed table, filters, create/edit modal, activate/deactivate. |
| `src/components/admin/StatusManagement.tsx` | Status table, color picker, live preview, edit / activate (no create or delete). |
| `src/components/admin/GroupManagement.tsx` | Farm group table, create/edit modal, activate/deactivate. Uses the first farm. |
| `src/components/LanguageSwitcher.tsx` | Switches `ar` / `en` via next-intl router (keeps the same page). |
| `src/components/Button.tsx` + `Button.module.css` | Shared button. |
| `src/components/Input.tsx` + `Input.module.css` | Shared field (design system). |
| `src/components/Card.tsx` + `Card.module.css` | Card wrapper. |
| `src/components/Badge.tsx` + `Badge.module.css` | Status chip. |

---

## 7. i18n (Arabic / English)

| File | What it is |
|---|---|
| `src/i18n/routing.ts` | Locales, default `ar`, cookie name, no auto-detect from browser. |
| `src/i18n/request.ts` | **Hooked from `next.config.mjs`.** Loads `messages/{locale}.json` for each request. |
| `src/i18n/navigation.ts` | `Link`, `redirect`, `useRouter` that keep the locale in the URL. **Use these**, not raw `next/link`, for in-app links. |
| `src/messages/ar.json` | Arabic copy. |
| `src/messages/en.json` | English copy. |
| `src/lib/locale.test.mjs` | Small unit test for locale helpers. |

---

## 8. Config and generated files (do not treat as app logic)

| File | What it is |
|---|---|
| `next-env.d.ts` | Next.js TypeScript references. Auto-generated. |
| `.next/` | Build cache. Do not edit. |
| `.env.example` | Template for `.env.local`. |

---

## 9. Path alias

In code, `@/auth` means `src/auth.ts`. Same for `@/components/...`, `@/lib/...`, `@/i18n/...`.

---

## 10. Mental model (rebuild order)

1. `package.json` + `next.config.mjs` + `.env.local`
2. `middleware.ts` + `i18n/*` + `messages/*`
3. `app/layout.tsx` + `app/[locale]/layout.tsx` + `globals.css`
4. `auth.ts` + `app/api/auth/[...nextauth]/route.ts`
5. Login page + `SignInButton`
6. Home / me / logout / refresh helpers
7. Components and design-system page

**Single sentence:** the **process** entry is `next dev`; the **HTTP** entry is `middleware.ts` for pages and `/api/auth/[...nextauth]` for login.
