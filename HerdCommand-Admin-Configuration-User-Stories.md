# HerdCommand Admin Configuration — Implementation Backlog

This backlog implements dynamic Breed (`السلالة`), Animal Status (`الحالة`), and Farm Group (`المجموعة`) configuration using Spring Boot and React.

## Agreed design decisions

1. Breed is administrator-managed master data.
2. Animal status uses an immutable technical code. Administrators may change labels, badge color, display order, visibility, and active state, but not the code.
3. Groups are operational records owned by one farm. They are not global lookup values.
4. React stores and sends IDs/codes, never Arabic or English display labels.
5. Referenced records are deactivated rather than physically deleted.
6. Arabic is the default language; English is also supported.
7. All endpoints are versioned under `/api/v1`.
8. Dates are ISO-8601 UTC. IDs are UUIDs unless the existing project has another standard.
9. Every write endpoint requires authorization and server-side validation.

## Recommended execution order

| Order | Story | Depends on |
|---:|---|---|
| 1 | US-AC-001 Configuration foundation | None |
| 2 | US-AC-002 Breed API | US-AC-001 |
| 3 | US-AC-003 Breed administration UI | US-AC-002 |
| 4 | US-AC-004 Status configuration API | US-AC-001 |
| 5 | US-AC-005 Status administration UI | US-AC-004 |
| 6 | US-AC-006 Farm group API | US-AC-001 |
| 7 | US-AC-007 Farm group administration UI | US-AC-006 |
| 8 | US-AC-008 Dynamic animal filter API | US-AC-002, 004, 006 |
| 9 | US-AC-009 Dynamic animal list filters | US-AC-008 |
| 10 | US-AC-010 Audit history | US-AC-002, 004, 006 |

---

# US-AC-001 — Configuration foundation

## User story

As a platform owner, I want a secure and consistent foundation for animal configuration so that breeds, statuses, and groups can be implemented without duplicated conventions.

## Scope

- Database migration framework and base audit fields.
- Standard API error response.
- Configuration permissions.
- Localization convention.
- No administration screens in this story.

## Spring Boot tasks

1. Confirm or configure Flyway/Liquibase.
2. Create reusable auditing fields: `createdAt`, `createdBy`, `updatedAt`, `updatedBy`.
3. Enable Spring Data JPA auditing using the authenticated user.
4. Add permissions:
   - `ANIMAL_CONFIG_VIEW`
   - `BREED_MANAGE`
   - `STATUS_CONFIG_MANAGE`
   - `GROUP_VIEW`
   - `GROUP_MANAGE`
5. Configure method security using `@PreAuthorize` or the project’s existing convention.
6. Add a global exception handler for validation, not-found, conflict, and forbidden errors.
7. Read the requested language from `Accept-Language`; support `ar` and `en`, with Arabic fallback.
8. Add OpenAPI documentation if the project already uses Springdoc.

## Standard error response

```json
{
  "code": "BREED_CODE_ALREADY_EXISTS",
  "message": "A breed with this code already exists.",
  "fieldErrors": [
    { "field": "code", "message": "Code must be unique." }
  ],
  "timestamp": "2026-09-04T12:00:00Z",
  "path": "/api/v1/admin/animal-breeds"
}
```

## Acceptance criteria

### Scenario: Reject unauthenticated administration access

**Given** the caller is not authenticated,  
**When** an administration endpoint is requested,  
**Then** the API returns HTTP `401`.

### Scenario: Reject missing permission

**Given** the caller is authenticated but lacks the required permission,  
**When** a protected administration endpoint is requested,  
**Then** the API returns HTTP `403`.

### Scenario: Return consistent validation errors

**Given** a request contains invalid fields,  
**When** it is submitted,  
**Then** the API returns HTTP `400` using the standard error structure.

## Tests

- Security tests for `401` and `403`.
- Global validation-error serialization test.
- JPA auditing test.
- Arabic and English message-resolution test.

## Definition of done

- Migration succeeds on an empty and existing database.
- Security tests pass.
- API errors use one documented format.
- No secrets, hardcoded users, or hardcoded farm IDs are introduced.

---

# US-AC-002 — Breed management API

## User story

As a system administrator, I want to manage animal breeds so that breeds can change without a code deployment.

## Data model

Table: `animal_breed`

| Column | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key |
| `code` | varchar(40) | Unique, uppercase, immutable after creation |
| `name_ar` | varchar(100) | Required |
| `name_en` | varchar(100) | Required |
| `species_code` | varchar(30) | Required; stable code such as `SHEEP` |
| `display_order` | integer | Default `0`, minimum `0` |
| `active` | boolean | Default `true` |
| Audit fields | — | Required |

Recommended index: `(species_code, active, display_order)`.

## API contract

### List breeds

```http
GET /api/v1/admin/animal-breeds?search=&speciesCode=SHEEP&active=true&page=0&size=20&sort=displayOrder,asc
```

### Get one breed

```http
GET /api/v1/admin/animal-breeds/{breedId}
```

### Create breed

```http
POST /api/v1/admin/animal-breeds
Content-Type: application/json
```

```json
{
  "code": "NAJDI",
  "nameAr": "نجدي",
  "nameEn": "Najdi",
  "speciesCode": "SHEEP",
  "displayOrder": 10,
  "active": true
}
```

### Update breed

```http
PUT /api/v1/admin/animal-breeds/{breedId}
```

The update request excludes `code`, or the backend ignores/rejects any attempt to change it.

### Change active state

```http
PATCH /api/v1/admin/animal-breeds/{breedId}/status
```

```json
{ "active": false }
```

## Spring Boot tasks

1. Create entity, repository, DTOs, mapper, service, and controller.
2. Normalize `code` using trim and uppercase before validation.
3. Enforce case-insensitive unique codes at both application and database levels.
4. Implement paginated search by code, Arabic name, or English name.
5. Prevent physical deletion through the public API.
6. Permit deactivation even when referenced, but preserve the label for historical records.
7. Prevent assigning inactive breeds when creating or updating an animal.
8. Return HTTP `409` for duplicate codes.
9. Add permission checks: list/get requires `ANIMAL_CONFIG_VIEW`; writes require `BREED_MANAGE`.

## Acceptance criteria

### Scenario: Create a valid breed

**Given** an authorized administrator,  
**When** a unique code and valid bilingual names are submitted,  
**Then** the breed is created with HTTP `201`,  
**And** the response contains its generated ID and audit fields.

### Scenario: Reject duplicate code

**Given** code `NAJDI` already exists,  
**When** `najdi` is submitted as a new code,  
**Then** the API returns HTTP `409` with code `BREED_CODE_ALREADY_EXISTS`.

### Scenario: Keep code immutable

**Given** an existing breed,  
**When** an update attempts to change its technical code,  
**Then** the API returns HTTP `400`,  
**And** the stored code remains unchanged.

### Scenario: Deactivate a referenced breed

**Given** animals reference a breed,  
**When** the administrator deactivates it,  
**Then** existing animals continue to display the breed,  
**And** it is excluded from new-animal selection endpoints.

## Tests

- Repository uniqueness test.
- Create, update, list, search, paging, and deactivate integration tests.
- Permission tests.
- Concurrent duplicate-create test or database constraint test.
- Test that inactive breeds cannot be newly assigned.

## Out of scope

- React screens.
- Species administration; use existing species codes or a temporary enum until separately configured.

---

# US-AC-003 — Breed administration UI

## User story

As a system administrator, I want a Breed tab in the Admin Portal so that I can search, add, edit, activate, and deactivate breeds.

## React tasks

1. Add route such as `/admin/animal-configuration?tab=breeds`.
2. Implement the Breed tab from the approved HTML concept.
3. Load data with TanStack Query and server-side pagination.
4. Add search, species, and active-state filters.
5. Debounce search by 300–500 ms.
6. Add create/edit modal with React Hook Form and Zod, if used by the project.
7. Fields: Arabic name, English name, code, species, display order, active.
8. Disable the code field when editing.
9. Add activation/deactivation confirmation.
10. Invalidate breed queries after successful mutations.
11. Display loading skeleton, empty state, API error, field errors, and success toast.
12. Hide write actions without `BREED_MANAGE`.
13. Support Arabic RTL and English LTR.

## Acceptance criteria

### Scenario: Load breed records

**Given** the user has view permission,  
**When** the Breed tab opens,  
**Then** records are retrieved from the API, not from a hardcoded React array.

### Scenario: Create a breed

**Given** valid form data,  
**When** the administrator saves,  
**Then** the modal closes, a success message appears, and the table refreshes.

### Scenario: Show API field error

**Given** the code already exists,  
**When** the form is submitted,  
**Then** the duplicate error is shown next to the code field without losing entered values.

### Scenario: Read-only administrator

**Given** the user has view permission but not manage permission,  
**When** the page opens,  
**Then** data is visible but add, edit, and state-change actions are unavailable.

## Tests

- Component test for data, loading, empty, and error states.
- Form validation tests.
- Mutation success and API-error tests using mocked network responses.
- RTL layout check.
- End-to-end test: create → edit → deactivate.

---

# US-AC-004 — Animal status configuration API

## User story

As a system administrator, I want to configure how animal statuses are presented while preserving stable technical codes used by business logic.

## Data model

Table: `animal_status_definition`

| Column | Type | Rules |
|---|---|---|
| `code` | varchar(40) | Primary key; immutable |
| `label_ar` | varchar(100) | Required |
| `label_en` | varchar(100) | Required |
| `color_token` | varchar(30) | From approved allowlist |
| `display_order` | integer | Minimum `0` |
| `visible_in_filter` | boolean | Default `true` |
| `active` | boolean | Default `true` |
| `system_protected` | boolean | Default `true` for seeded statuses |
| Audit fields | — | Required |

Seed at least: `ACTIVE`, `PREGNANT`, `SICK`, and `ISOLATED`. Add other statuses only if required by animal lifecycle rules.

Approved initial color tokens: `success`, `purple`, `danger`, `warning`, `neutral`. Store tokens, not arbitrary CSS or hex values.

## API contract

```http
GET /api/v1/admin/animal-statuses?search=&active=true
GET /api/v1/admin/animal-statuses/{code}
PUT /api/v1/admin/animal-statuses/{code}
PATCH /api/v1/admin/animal-statuses/{code}/status
```

Update request:

```json
{
  "labelAr": "حامل",
  "labelEn": "Pregnant",
  "colorToken": "purple",
  "displayOrder": 20,
  "visibleInFilter": true,
  "active": true
}
```

## Spring Boot tasks

1. Create migration, entity, repository, service, DTOs, mapper, and controller.
2. Seed protected statuses idempotently.
3. Do not expose code modification.
4. Validate `colorToken` against the allowlist.
5. Prevent deactivation if the status is mandatory for a running workflow, based on explicit project rules.
6. Existing animals keep displaying inactive status definitions.
7. Cache active status definitions if appropriate; evict the cache after updates.
8. Require `ANIMAL_CONFIG_VIEW` for reads and `STATUS_CONFIG_MANAGE` for writes.

## Acceptance criteria

### Scenario: Update presentation

**Given** status `PREGNANT` exists,  
**When** its Arabic label and color token are updated,  
**Then** the technical code stays `PREGNANT`,  
**And** subsequent API responses contain the new presentation values.

### Scenario: Reject an unsafe color

**Given** a request uses an arbitrary value such as `url(...)`,  
**When** it is submitted,  
**Then** the API returns HTTP `400` with `INVALID_COLOR_TOKEN`.

### Scenario: Protect technical code

**Given** a protected status,  
**When** an administrator edits it,  
**Then** only permitted display fields are updated.

## Tests

- Idempotent seed-data test.
- Immutable-code test.
- Allowed and rejected color-token tests.
- Localization, ordering, visibility, and authorization tests.
- Cache eviction test if caching is added.

## Out of scope

- Changing animal workflow behavior through the Admin Portal.
- Arbitrary administrator-created statuses unless a future story defines transition rules.

---

# US-AC-005 — Animal status administration UI

## User story

As a system administrator, I want to configure bilingual status labels, badge appearance, order, and filter visibility from the Admin Portal.

## React tasks

1. Implement the Status tab using API data.
2. Display technical code with a lock/protected indicator.
3. Add edit modal; show code as read-only.
4. Provide a controlled badge-color picker using supported tokens.
5. Show a live badge preview.
6. Add `visibleInFilter`, `active`, and display-order controls.
7. Do not provide a delete action for protected statuses.
8. Display a warning explaining that labels do not change business behavior.
9. Refresh cached status and animal-filter queries after a successful update.

## Acceptance criteria

### Scenario: Preview status styling

**Given** the edit modal is open,  
**When** a color token or label changes,  
**Then** the badge preview updates immediately without saving.

### Scenario: Hide status from filters

**Given** `visibleInFilter` is disabled and saved,  
**When** animal filter configuration is refreshed,  
**Then** that status is no longer offered as a filter option.

### Scenario: Protect the code in the UI

**Given** a status is being edited,  
**Then** its technical code is visibly read-only and cannot be submitted as an updated value.

## Tests

- Status list rendering and badge mapping tests.
- Read-only code test.
- Live-preview test.
- Visibility and active toggle mutation tests.
- End-to-end status-edit test.

---

# US-AC-006 — Farm group management API

## User story

As a farm administrator, I want to manage groups belonging to my farm so that animals can be organized without exposing data across farms.

## Data model

Table: `animal_group`

| Column | Type | Rules |
|---|---|---|
| `id` | UUID | Primary key |
| `farm_id` | UUID | Required; immutable |
| `code` | varchar(40) | Unique within a farm |
| `name_ar` | varchar(100) | Required |
| `name_en` | varchar(100) | Required |
| `group_type_code` | varchar(30) | Required |
| `description` | varchar(500) | Optional |
| `capacity` | integer | Optional; positive |
| `active` | boolean | Default `true` |
| Audit fields | — | Required |

Unique constraint: `(farm_id, lower(code))` or equivalent normalized-code constraint.

## API contract

```http
GET  /api/v1/farms/{farmId}/animal-groups?search=&active=true&page=0&size=20
GET  /api/v1/farms/{farmId}/animal-groups/{groupId}
POST /api/v1/farms/{farmId}/animal-groups
PUT  /api/v1/farms/{farmId}/animal-groups/{groupId}
PATCH /api/v1/farms/{farmId}/animal-groups/{groupId}/status
POST /api/v1/farms/{farmId}/animal-groups/{groupId}/animals
```

Bulk assignment request:

```json
{
  "animalIds": ["uuid-1", "uuid-2"]
}
```

## Spring Boot tasks

1. Implement entity, migration, repository, DTOs, mapper, service, and controller.
2. Derive accessible farms from the authenticated user; never trust `farmId` by itself.
3. Enforce group-code uniqueness within the farm.
4. Return `animalCount` in group list responses.
5. Validate that assigned animals and the group belong to the same farm.
6. Decide and document whether one animal can have one current group or multiple groups. Recommended MVP: one current group per animal.
7. Implement bulk assignment transactionally.
8. On deactivation of a non-empty group, return HTTP `409` with the animal count unless `moveToGroupId` or explicit unassignment is provided by the agreed workflow.
9. Require `GROUP_VIEW` for reads and `GROUP_MANAGE` for writes.

## Acceptance criteria

### Scenario: Create a farm-owned group

**Given** the user manages Farm A,  
**When** a valid group is created for Farm A,  
**Then** the API returns HTTP `201`,  
**And** the group is associated with Farm A.

### Scenario: Prevent cross-farm access

**Given** a user can access Farm A but not Farm B,  
**When** the user requests or modifies a Farm B group,  
**Then** the API returns `403` or the project-standard non-disclosing `404`.

### Scenario: Prevent cross-farm assignment

**Given** an animal belongs to Farm A and a group belongs to Farm B,  
**When** assignment is requested,  
**Then** the API rejects it and makes no changes.

### Scenario: Protect a populated group

**Given** a group contains 24 animals,  
**When** simple deactivation is requested,  
**Then** the API returns HTTP `409`, the error code `GROUP_NOT_EMPTY`, and `affectedAnimalCount: 24`.

## Tests

- Farm-isolation tests for every endpoint.
- Unique code per farm test.
- Animal count test.
- Transactional bulk-assignment rollback test.
- Non-empty group deactivation test.
- Permission tests.

---

# US-AC-007 — Farm group administration UI

## User story

As a farm administrator, I want to manage farm groups and see their animal counts so that I can organize the herd safely.

## React tasks

1. Implement the Group tab using the currently selected farm.
2. Add search, active-state, and group-type filters.
3. Add create/edit modal with bilingual names, code, type, capacity, and description.
4. Display animal count for each group.
5. Add a group details or assignment drawer for selecting animals.
6. Support bulk selection and assignment.
7. If deactivation returns `GROUP_NOT_EMPTY`, show the affected count and require the user to move or unassign animals.
8. Never display groups from another farm in dropdowns or cached results.
9. Include `farmId` in TanStack Query keys to prevent cross-farm cache leakage.
10. Hide write controls without `GROUP_MANAGE`.

## Acceptance criteria

### Scenario: Switch farms

**Given** the user can manage more than one farm,  
**When** the active farm changes,  
**Then** the group list and cached options reload for the selected farm.

### Scenario: Assign animals

**Given** eligible animals are selected,  
**When** assignment is confirmed,  
**Then** the API is called once using bulk assignment,  
**And** group counts and animal queries are refreshed.

### Scenario: Deactivate a populated group

**Given** the group contains animals,  
**When** deactivation is attempted,  
**Then** the UI explains why it cannot be immediately deactivated and offers the agreed move/unassign flow.

## Tests

- Farm-aware query key test.
- Create/edit validation tests.
- Bulk-selection and assignment tests.
- Conflict-response UI test.
- Cross-farm UI regression test.

---

# US-AC-008 — Dynamic animal filter-options API

## User story

As a farm user, I want the animals page to receive its filter options dynamically so that configuration changes appear without a deployment.

## API contract

```http
GET /api/v1/farms/{farmId}/animal-filter-options
Accept-Language: ar
```

Example response:

```json
{
  "breeds": [
    { "id": "breed-uuid", "code": "NAJDI", "label": "نجدي" }
  ],
  "statuses": [
    { "code": "ACTIVE", "label": "نشط", "colorToken": "success" }
  ],
  "groups": [
    { "id": "group-uuid", "code": "GRP-001", "label": "أمهات حري" }
  ],
  "genders": [
    { "code": "FEMALE", "label": "نعجة" },
    { "code": "MALE", "label": "كبش" }
  ]
}
```

## Spring Boot tasks

1. Aggregate active breeds, visible active statuses, and active groups accessible for the farm.
2. Sort each collection by configured display order and localized label.
3. Resolve `label` using `Accept-Language` without changing IDs/codes.
4. Keep gender as stable system codes initially; localize labels server-side or through the existing i18n layer.
5. Apply farm authorization.
6. Add cache headers or server-side caching only if consistent with the project; evict after configuration changes.

## Acceptance criteria

### Scenario: Arabic options

**Given** `Accept-Language: ar`,  
**When** filter options are requested,  
**Then** Arabic labels are returned with stable IDs/codes.

### Scenario: English options

**Given** `Accept-Language: en`,  
**When** filter options are requested,  
**Then** English labels are returned for the same IDs/codes.

### Scenario: Exclude unavailable values

**Given** a breed is inactive, a status is hidden from filters, or a group is inactive,  
**When** options are requested,  
**Then** those values are excluded.

## Tests

- Arabic and English response tests.
- Sorting test.
- Active/visible filtering test.
- Farm-isolation test.

---

# US-AC-009 — Dynamic filters on the React animals list

## User story

As a farm user, I want to filter the animals list by dynamically loaded breed, status, group, and gender options.

## Required animal search API behavior

```http
GET /api/v1/farms/{farmId}/animals?search=&breedId=&statusCode=&groupId=&genderCode=&page=0&size=20&sort=createdAt,desc
```

All supplied criteria use `AND`. Empty parameters are omitted by React. The API validates that `groupId` belongs to the requested and accessible farm.

## React tasks

1. Remove hardcoded breed, status, group, and gender arrays from the animals page.
2. Load `/animal-filter-options` with TanStack Query.
3. Store selected values as `breedId`, `statusCode`, `groupId`, and `genderCode`.
4. Add selected values to the animal-query key.
5. Synchronize filters, search, page, and sort with URL query parameters.
6. Debounce search by 300–500 ms.
7. Reset page to zero when any filter changes.
8. Render status badges using the API `colorToken` mapping defined in one safe frontend map.
9. Add loading, error, empty-results, and retry states.
10. Clear invalid URL filter values that are not returned by the options API.
11. Preserve selections during language changes while replacing display labels.

## Acceptance criteria

### Scenario: No hardcoded options

**Given** an administrator creates or updates a configuration record,  
**When** the animals page refreshes its options,  
**Then** the new label/value is displayed without rebuilding React.

### Scenario: Combine filters

**Given** a breed, status, and group are selected,  
**When** the animals request is made,  
**Then** it includes their stable ID/code parameters and returns records matching all criteria.

### Scenario: Preserve filters in URL

**Given** filters are selected,  
**When** the page is refreshed or its URL is shared,  
**Then** valid selections are restored.

### Scenario: Change language

**Given** `NAJDI` is selected in Arabic,  
**When** the language changes to English,  
**Then** the same breed remains selected and its label changes to `Najdi`.

## Tests

- Filter-options loading test.
- Query-parameter generation test.
- Combined-filter test.
- URL restoration test.
- Language-switch preservation test.
- No-options and API-error tests.
- End-to-end test from admin update to animal-filter display.

---

# US-AC-010 — Configuration audit history

## User story

As an auditor or system administrator, I want to view configuration changes so that I can identify what changed, who changed it, and when.

## Scope

Audit create, update, activation, and deactivation for breeds, status definitions, and groups. Animal group membership changes may be included now or separated if volume is high.

## API contract

```http
GET /api/v1/admin/animal-configuration/audit?entityType=BREED&entityId=&userId=&from=&to=&page=0&size=20
```

Response items include:

- Entity type and ID.
- Action.
- Timestamp.
- User ID and display name when available.
- Farm ID when applicable.
- Changed fields with before and after values.

## Tasks

1. Use the project audit mechanism if one exists; do not create a parallel solution unnecessarily.
2. Exclude secrets and sensitive tokens from audit payloads.
3. Record changes transactionally with the configuration write.
4. Implement paginated and permission-protected history endpoint.
5. Connect the “Change History” button in the React Admin Portal.
6. Add entity type, date, and user filters.
7. Render bilingual field labels in React; stored audit field names remain stable.

## Acceptance criteria

### Scenario: Record an update

**Given** an administrator changes a status label,  
**When** the update succeeds,  
**Then** one audit entry records the old label, new label, user, and timestamp.

### Scenario: Roll back a failed operation

**Given** a configuration update fails,  
**When** the transaction is rolled back,  
**Then** no successful-change audit entry is stored.

### Scenario: Restrict history

**Given** the user lacks audit-view permission,  
**When** audit history is requested,  
**Then** the API returns HTTP `403` and the React button is hidden.

## Tests

- Create/update/deactivate audit tests.
- Transaction rollback test.
- Pagination/filter tests.
- Permission tests.
- React history-list tests.

---

# Shared non-functional requirements

## Backend

- Follow the existing package structure and naming conventions.
- Controllers contain no business logic.
- Use DTOs; do not expose JPA entities directly.
- Validate at API and database levels where applicable.
- Avoid N+1 queries, especially when returning group animal counts.
- Paginate administration lists.
- Use optimistic locking (`@Version`) if simultaneous administration is realistic.
- Return stable machine-readable error codes.
- Document endpoints through OpenAPI or the project standard.

## Frontend

- Follow the existing component library and design tokens.
- Use a single API client and existing authentication mechanism.
- Use TanStack Query for server state; do not copy server data into Zustand unless necessary.
- Use React Hook Form/Zod only if compatible with the existing project conventions.
- Support RTL/LTR, keyboard navigation, labels, focus states, and accessible dialogs.
- Do not hardcode business data in UI components.
- Do not use translated labels as option values or React keys.
- Escape API content and map color tokens through a fixed safe map.

## Suggested Cursor instruction for every story

Copy one story at a time and add this instruction:

> Implement only this user story. First inspect the existing repository structure, authentication, error handling, database migrations, API client, UI component library, routing, localization, and test conventions. Reuse existing patterns. Do not implement later stories or introduce a parallel architecture. Before changing code, list the files you plan to modify. After implementation, run relevant tests and report assumptions, migrations, endpoints, and any remaining gaps.

## Overall definition of done

- No breed, status, or group options are hardcoded in the animals page.
- Protected status codes cannot be modified.
- Inactive records remain readable for historical animals but cannot be newly assigned.
- Groups are isolated by farm at database-query, service, and endpoint levels.
- Arabic and English labels work without changing saved IDs/codes.
- Permissions are enforced by the API and reflected in the UI.
- Unit, integration, component, and critical end-to-end tests pass.
- Database migrations and API contracts are documented.
