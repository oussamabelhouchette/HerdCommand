# US-AC-007 — Farm group administration UI

This document explains the Groups tab on `/admin/animal-settings`. The API is US-AC-006. There is **no** farm switcher, assignment drawer, or TanStack Query.

Related: [us-ac-006-farm-group-api.md](./us-ac-006-farm-group-api.md), [us-ac-003-breed-administration-ui.md](./us-ac-003-breed-administration-ui.md), [implementation-guide.md](./implementation-guide.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Route | Same `/admin/animal-settings`. Groups is a **tab**. |
| Farm | Server loads `GET /api/v1/farms` and uses the **first** farm (`HARRI`). No switcher. |
| Create / edit | Modal like breeds. Code uppercase; disabled on edit; PUT does not send `code`. |
| Soft status | Deactivate confirm; activate one click. No delete. |
| Animal count | Column shows `0`. |
| Who sees the screen | Unchanged: `owner` / `administrator`. Those roles already have `GROUP_MANAGE`. |

### Deferred

- Farm switcher and farm-aware query keys
- Assignment drawer / bulk select
- `GROUP_NOT_EMPTY` move/unassign UI
- TanStack Query
- Hiding write buttons for `GROUP_VIEW`-only users (they cannot open `/admin`)

---

## 2. How the pieces connect

```
animal-settings/page.tsx
    listFarms → first farmId
    listGroups(farmId)
    ▼
AnimalSettings tabs
    ▼
GroupManagement
    GET/POST/PUT/PATCH  /api/v1/farms/{farmId}/animal-groups
    401 ──► federated logout
    409 GROUP_CODE_ALREADY_EXISTS ──► under code
```

---

## 3. File-by-file

#### `apps/web/src/lib/groups.ts`

`listFarms`, `listGroups`, `createGroup`, `updateGroup`, `updateGroupActive`. Every group URL includes `farmId`. No assign helper. `GROUP_TYPE_CODES` matches the API enum.

#### `apps/web/src/components/admin/GroupManagement.tsx`

Search (300 ms), active filter, add button, table, create/edit modal (names, code, optional capacity, description), deactivate confirm. Reloads after save. Group type is hidden; the client always sends `OTHER` until animal stories need types.

#### `apps/web/src/components/admin/GroupManagement.module.css`

Textarea for description.

#### `apps/web/src/components/admin/AnimalSettings.tsx` and `animal-settings/page.tsx`

Groups tab enabled. Stat card uses the real group total.

#### Messages

`animalSettings.addGroup`, `group*`, `groupTypes.*` in `en.json` / `ar.json`.

#### `apps/web/src/lib/groups.test.mjs`

Locks `farmId` in paths, no DELETE/assign helper, create + unique-code handling, code not submitted on update.

---

## 4. How to test

```powershell
pnpm.cmd --filter @herdcommand/web test
```

Click-test (API must have V4):

1. Sign in as administrator → `/admin/animal-settings`.
2. Click **Groups**. Add a group (Arabic name, English name, code `DAMS-1`). There is no type field. Toast + row.
3. Add again with `dams-1` → duplicate-code message.
4. Edit: code disabled; change English name; save; code unchanged.
5. Deactivate → confirm → Inactive. Activate again.
6. Animal count stays `0`.
7. Breeds and Statuses tabs still work.

---

## 5. Definition of done

- [x] Groups tab uses the first accessible farm
- [x] Create / edit / deactivate like breeds
- [x] `farmId` on every client path
- [x] No assignment UI; counts are 0
- [x] Arabic and English copy
- [x] Source-lock tests
