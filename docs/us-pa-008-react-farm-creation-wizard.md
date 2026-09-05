# PA-008 — React farm creation wizard

This document explains **every file added or changed** for the four-step farm creation wizard. The create API is PA-005. There is **no** edit/suspend (PA-009).

Related: [us-pa-005-atomic-farm-creation-api.md](./us-pa-005-atomic-farm-creation-api.md), [us-pa-007-react-all-farms-administration-page.md](./us-pa-007-react-all-farms-administration-page.md), [us-pa-004-keycloak-owner-lookup-and-invitation.md](./us-pa-004-keycloak-owner-lookup-and-invitation.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Entry | **Add a farm** on `/admin/farm-settings` opens a modal wizard. |
| Steps | 1 farm identity · 2 owner lookup · 3 plan + catalog features · 4 review. |
| Farm code | Never collected. Server generates `FARM-TN-####`. |
| Names | Arabic and French required. English optional (API copies French when omitted). |
| Governorate | Allow-listed `TN-xx` codes. |
| Status | Shown as setup only. API still rejects any other `initialStatus`. |
| Owner | Same Keycloak lookup as PA-004. Missing user → invitation copy. Disabled user cannot continue. |
| Features | Catalog from props. Coming-soon stays disabled. Defaults are enableable codes. |
| Limits | Caps displayed from the server catalog. The wizard does not send client max values. |
| Submit | One `POST /api/v1/platform/farms` with `Idempotency-Key`. Same key reused on retry of the same body. |
| Success | Wizard closes, list/summary reload, details dialog opens on the new farm. |
| Validation | Client step checks, then backend `fieldErrors` jump to the matching step. |
| Form library | None. No React Hook Form / Zod / TanStack Query. |

---

## 2. How the pieces connect

```
Add farm
    ▼
FarmCreateWizard
    lookupIdentity          (step 2)
    createPlatformFarm      (step 4)
      Idempotency-Key
      POST /api/v1/platform/farms
    401 ──► federated logout
    ▼
FarmSettings reload + details
```

---

## 3. File-by-file

#### `apps/web/src/lib/farms.ts` *(changed)*

`createPlatformFarm`, `newIdempotencyKey`, governorate/plan allow-lists, `wizardStepForField`.

#### `apps/web/src/lib/api.ts` *(changed)*

Optional extra headers so the idempotency key can be sent.

#### `apps/web/src/components/admin/FarmCreateWizard.tsx` *(new)*

Four-step modal. Preserves values between steps.

#### `apps/web/src/components/admin/FarmSettings.tsx` *(changed)*

Add button enabled. Hosts the wizard. Shows the created farm.

#### Messages / CSS

`farmSettings.wizard.*` and `governorate.*`. Wizard layout, stepper, toast.

#### `apps/web/src/lib/farm-wizard.test.mjs` *(new)*

Source-lock: steps, lookup states, catalog, idempotency reuse, no farm-code field.

---

## 4. How to test

```powershell
pnpm.cmd --filter @herdcommand/web test
```

Click-test (API must have PA-005 + Keycloak admin client):

1. Sign in as `PLATFORM_ADMIN` → Add a farm.
2. Enter Arabic/French names and a governorate. Continue.
3. Existing owner email → found. Unknown email → invitation copy. Continue.
4. Trial + Animal management checked; coming-soon disabled. Review and create.
5. New row appears. Details dialog opens. Refresh keeps the farm.
6. Double-click Create: the button stays disabled while saving.

---

## 5. Assumptions and gaps

- French is a **farm name** field, not a product locale.
- Phone is validated as E.164 and not stored (PA-005).
- Plan caps are display-only; the server still enforces them.
- Edit / suspend remain disabled (PA-009).

## 6. Definition of done

- [x] Four-step wizard from Add a farm
- [x] No farm-code input
- [x] Owner lookup + invitation copy
- [x] Dynamic catalog; coming-soon disabled
- [x] One idempotency key per payload; retries reuse it
- [x] Field errors return to the affected step
- [x] Success refreshes the list and opens details
- [x] Source-lock tests
