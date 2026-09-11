# PA-004 — Keycloak owner lookup and invitation

This document explains **every file added or changed** for identifying or inviting a farm owner by email. Authentication stays in Keycloak. Ownership stays in HerdCommand.

There is **no farm create API** here (PA-005). Lookup does **not** create Keycloak users. Provisioning exists as a server service for the next story.

Related: [us-pa-001-platform-admin-security-foundation.md](./us-pa-001-platform-admin-security-foundation.md), [us-pa-003-dynamic-feature-catalog.md](./us-pa-003-dynamic-feature-catalog.md), [authentication.md](./authentication.md).

---

## 1. What this story delivered

| Concern | Decision |
|---|---|
| Lookup API | `POST /api/v1/platform/identity/lookup` with `{ email }`. `PLATFORM_ADMIN` only. |
| Found | `{ found: true, keycloakUserId, email, displayName, enabled }`. No credentials, roles, or extra Keycloak profile. |
| Missing | HTTP 200 `{ found: false, invitationRequired: true }`. Not a server error. No user is created. |
| Disabled | Lookup returns `enabled: false`. `IdentityProvisioningService.resolveOwner` throws `400 IDENTITY_DISABLED`. |
| Keycloak client | Server-side `herdcommand-admin` service account. Secret from `KEYCLOAK_ADMIN_CLIENT_SECRET` only. |
| React | Debounced owner email on farm-settings. Calls Spring, never `/admin/realms`. |
| Roles | No farm-specific Keycloak roles are assigned. |
| Logging | Outcome only (`found`, `enabled`). Tokens and client secret are never logged. |

### When farm creation (later) calls `resolveOwner`

| Keycloak user | Result |
|---|---|
| One enabled match | Use that user id. Membership will be `ACTIVE`. Password untouched. |
| One disabled match | `IDENTITY_DISABLED`. No user created. |
| No match | Create user (`enabled=true`, `emailVerified=false`, no password). Required actions `VERIFY_EMAIL`, `UPDATE_PASSWORD`. Send execute-actions email. Membership will be `INVITED`. If the email send fails, invitation stays retryable (`invitationEmailSent=false`). |
| Several exact matches | `409 IDENTITY_AMBIGUOUS`. |

Compensation `deleteCreatedIdentity` exists for an identity **this operation created**. PA-005 will call it if the database write fails. Lookup never deletes anyone.

---

## 2. How the pieces connect

```
React OwnerLookup (debounce 400ms)
    POST /api/v1/platform/identity/lookup
        PLATFORM_ADMIN
        IdentityLookupService
            normalize email
            IdentityDirectory.findByExactEmail
                Keycloak GET /admin/realms/{realm}/users?email=&exact=true
        0 rows ──► invitationRequired
        1 row  ──► found + enabled flag
        2+     ──► 409 IDENTITY_AMBIGUOUS
        down   ──► 503 IDENTITY_PROVIDER_UNAVAILABLE
```

---

## 3. File-by-file

#### Backend

- `domain/identity/*` — email helper, lookup/provision services, `IdentityDirectory`
- `identity/keycloak/KeycloakIdentityDirectory` — token + users + execute-actions-email
- `api/platform/identity/IdentityController`
- Error codes / `messages_en.properties` / `messages_ar.properties`
- `application.yml` Keycloak admin settings (secret from env)

#### Config you must do once locally

Create confidential client **`herdcommand-admin`** (also listed in `infra/keycloak/realm/herdcommand-realm.json` for new imports). Enable service account. Assign **realm-management** client roles `view-users`, `query-users`, `manage-users`. Turn **Full scope allowed** on so those roles appear in the client-credentials token. Copy the client secret into `.env` as `KEYCLOAK_ADMIN_CLIENT_SECRET`. Restart the API.

Keycloak SMTP must be configured if you want invitation emails to leave the box. PA-005 will send them at create time.

#### Web

- `apps/web/src/lib/identity.ts`
- `OwnerLookup.tsx` on the farm-settings page
- `ar.json` / `en.json` owner copy
- `.env.example` admin client variables (empty secret)

---

## 4. How to test

```powershell
mvn -f apps/api/pom.xml test "-Dtest=EmailAddressesTest,IdentityServicesTest,IdentityLookupApiTest,KeycloakAdminPropertiesTest" "-Daether.connector.https.securityMode=insecure"
npm --prefix apps/web test
```

Restart the API after setting `KEYCLOAK_ADMIN_CLIENT_SECRET`. As a platform admin, type an existing user email on `/admin/farm-settings`. You should see the display name. An unknown email shows the invitation hint. A disabled user shows the disabled message.

```powershell
curl -s http://localhost:8080/api/v1/platform/identity/lookup -H "Authorization: Bearer PASTE" -H "Content-Type: application/json" -d "{\"email\":\"Owner@Example.TN\"}"
```

---

## 5. Assumptions and gaps

- Lookup does not create memberships or farms.
- Invitation email is sent only from `resolveOwner` / `resendInvitation`, which farm create will call.
- After the owner finishes Keycloak verify/password, flipping `INVITED` → `ACTIVE` waits for first login handling (later story).
- The live local realm is not rewritten automatically. Add the service-account client in the admin console if this realm was imported before PA-004.
- No TanStack Query. Debounce matches the breed search pattern.

---

## 6. Definition of done

- [x] `POST /api/v1/platform/identity/lookup` for platform admins
- [x] Found / missing / disabled / unavailable covered
- [x] Email normalized
- [x] Service-account client; secret not in git or React
- [x] Provision abstraction for existing, disabled, and invite flows
- [x] Debounced owner field with safe states
- [x] Automated tests above
