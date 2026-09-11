# UI architecture (apps/web)

This is the standard every farm and admin screen must follow. The goal is a UI that is **fast**, **predictable**, and **obvious to the next developer**.

Read this before adding a page, a `'use client'` file, or a fetch from the browser.

---

## 1. Default: Server Components

Next.js App Router renders **Server Components** unless a file starts with `'use client'`.

| Layer | Lives in | May use | Must not |
|---|---|---|---|
| Route | `app/**/page.tsx`, `layout.tsx` | `await` data, compose children | Hooks, click handlers, `'use client'` |
| Screen (RSC) | `components/**` without `'use client'` | Fetch, i18n server APIs, pass **serializable** props | Access tokens as props into client trees |
| Island | `components/**` with `'use client'` **from the first commit** | `useState`, debounce, dialogs, `useSession` | Server-only APIs (`cookies`, `headers`, `next/headers`) |
| Data | `src/lib/*.ts` (no React directive) | `apiFetch`, pure helpers | `'use client'` / `'use server'` mixed with UI |

**Mental model**

```
page.tsx          → thin: locale, gate, one screen
OwnerAnimals.tsx  → RSC: lock card, list, GET filters, server actions
OwnerGroups.tsx   → RSC: lock card, list, GET filters, server actions
src/lib/animals.ts → API + pure helpers only
src/lib/animal-actions.ts → save / archive
src/lib/groups.ts → API + parse/href helpers
src/lib/group-actions.ts → save / activate
```

Admin screens (breeds, farms) hydrate a client island from the page. **Farm `[farmId]` main content does not** — that extra client module makes webpack throw `undefined.call`.


---

## 2. Hard rules (do not break)

1. **Never flip a file** between server and client. If a module needs `'use client'`, create a **new** file. Fast Refresh cannot recover from `Cannot read properties of undefined (reading 'call')`.
2. **`page.tsx` never imports a client module.** It imports one RSC screen. Farm `[farmId]` content RSCs also must not import client modules. Admin pages may render a client island.
3. **Do not add icons or helpers to `'use client'` barrels that RSC files import.** Shared SVGs live in `components/ui/Icons.tsx` (no directive). Client files may import that module. RSC files must **not** import `AdminIcons.tsx`.
4. **Do not pass access tokens as props** into client components. Client code reads `useSession().data?.accessToken`. Server code reads `getSession()` or `requireCurrentFarm` (both cached).
5. **Farm identity is exact.** `findOwnerFarm` / `requireCurrentFarm` match the URL id. Never fall back to `farms[0]` on a `[farmId]` route. The only farm redirect is `/farm` → `/farm/{firstId}`.
6. **Redirects belong in one place per flow.** Farm index redirects; `[farmId]` pages do not. Server actions that close a farm dialog must `redirect` to a **locale-prefixed** path (`/ar/farm/...`). Middleware does not run on the action response, so `/farm/...` never matches `[locale]` and `useLocale` in farm chrome throws. Admin save stays on the page (client fetch) and does not hit this.
7. **Copy lives in `messages/ar.json` and `messages/en.json`.** No demo animal names, IDs, or `window.confirm()`.
8. **If you delete `apps/web/.next`, stop `next dev` first.** Then start it again and hard-refresh. Never delete the cache while Next is running.

---

## 3. How to build a screen (checklist)

**Route file** (`app/[locale]/farm/[farmId]/…/page.tsx`)

- `setRequestLocale(locale)`
- `requireCurrentFarm` (cached; layout already gated)
- `return <Screen farm={current.farm} … />`
- No `'use client'`, no `redirect`, no island import

**RSC screen**

- If the feature is locked, render the lock card here (no island JS).
- `Promise.all` the lookups + list.
- Filters: GET form. Add/edit/archive: `'use server'` actions + URL query (`compose`, `edit`, `archive`).
- Use `<a href={getPathname(...)}>` in farm content, not `Link` from `@/i18n/navigation` (that is a client component).

**Admin client island** (not farm `[farmId]` content)

- Hydrate from server props; skip the first refetch (`useRef` guard).
- Debounce search. Reset page to `0` when filters change.
- Ignore stale responses (`requestSeq`).
- Talk to Spring with `apiFetch` + `useSession` token.
- Keep dialogs in React state. Sync list query with `history.replaceState`.

---

## 4. Farm portal layout

| File | Owns |
|---|---|
| `farm/layout.tsx` | Auth gate, `FarmShell`, i18n messages for the client tree |
| `farm/page.tsx` | Redirect to the first farm, or empty/error notice |
| `farm/[farmId]/layout.tsx` | Exact farm or `notFound()` / load error |
| `farm/[farmId]/page.tsx` | Dashboard RSC |
| `farm/[farmId]/animals/page.tsx` | Animals RSC |
| `farm/[farmId]/groups/page.tsx` | Groups RSC |

`loadFarmPortal` / `requireCurrentFarm` are `React.cache` — calling them from layout and page is cheap, not a double network call.

---

## 5. Performance norms

- **HTML first:** RSC fetches the list. Farm animals and groups have no extra island JS.
- **JS only in farm chrome and admin tables.**
- **Prefetch** farm nav links (`<Link prefetch>` in `FarmNav`).
- **Session:** `AuthSessionProvider` already wraps farm/admin. Do not nest another provider.

---

## 6. Shared UI kit (`components/ui`)

Farm and admin are **one app**. Do not copy a new table or dialog into `farm/` or `admin/`.

| Component | Use for |
|---|---|
| `PageHeader` | Title, subtitle, actions |
| `FilterBar` / `FilterSearch` / `FilterSelect` | Search + filter row (`as="form"` or `as="div"`, `tone="card"` or `"plain"`) |
| `DataTable` / `DataTableFoot` | List card, empty/error, pagination slot (`tone="card"` or `"plain"`) |
| `Dialog` / `DialogFoot` / `FormFields` | Modal, confirm, form grid (`size="wide"` for wizards) |
| `StatGrid` / `StatCard` | Summary counts (`href` for a clickable card) |
| `ActionButton` / `ActionLink` | Shared buttons and button-looking links |
| `Callout` | Locked-module / notice card |
| `Surface` | White content panel |
| `Note` | Helper strip under a list |
| `Icons` | SVGs (no `'use client'`) |
| `Button` / `Input` / `Card` / `Badge` | Existing primitives in `components/` |

These files have **no** `'use client'`. **Farm `[farmId]` content imports them.** Admin client islands must **not** — Next can omit that CSS on `/admin/*` when the same modules also feed farm RSC. Admin layout CSS lives on the island (`FarmSettings.module.css`, `BreedManagement.module.css`). Dialogs use `AdminDialog` (`'use client'`, own CSS). Icons stay in `components/ui/Icons.tsx`.

## 7. CSS and i18n

- Colocate CSS modules with the screen (`OwnerAnimals.module.css`). Use logical properties (`padding-inline-start`, `inset-inline-start`).
- Pagination class is `.pageBtn` (never `.page` — webpack CSS name clash).
- Shared kit CSS locals must be unique (`.statCard`, `.tableCard`, `.dialogPanel`). Never reuse `.card`, `.panel`, `.actions`, `.form`, or `.field` — those names already exist on admin screens and webpack can mix the rules.
- Links that look like buttons must set `text-decoration: none` in that module.
- Use `@/i18n/navigation` (`Link`, `redirect`, `useRouter`, `usePathname`), not raw `next/link`, for in-app routes.

---

## 8. Adding a new farm page (copy this)

1. New `page.tsx` — copy dashboard: `requireCurrentFarm` → one RSC.
2. New `OwnerXyz.tsx` — **no** `'use client'`, and **do not** import a client island. Fetch, lock, GET filters, server actions.
3. New `src/lib/xyz.ts` — paths, types, `apiFetch` helpers, pagination math.
4. New `src/lib/xyz-actions.ts` — `'use server'` mutations.
5. Messages under a dedicated namespace; pass it from `farm/layout.tsx` if layout client chrome needs them.
6. Architecture tests: page and screen have no `'use client'`.

---

## 9. Webpack `undefined.call`

Symptom: `webpack.js` `Cannot read properties of undefined (reading 'call')`.

Cause: a module’s server/client kind changed, or `.next` was deleted while `next dev` ran.

Fix: stop Next → delete `apps/web/.next` → start Next → hard refresh. Do not keep editing the same file hoping Fast Refresh will heal it.
