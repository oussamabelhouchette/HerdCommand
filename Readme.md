# HerdCommand

Arabic-first livestock farm platform. R0 Foundation: design system, bilingual RTL/LTR, engineering baseline, and Keycloak authentication.

## Layout

- `apps/web` — Next.js dashboard (Arabic default, English LTR)
- `apps/mobile` — Expo / React Native field shell (OIDC PKCE stub)
- `apps/api` — Spring Boot REST API (OpenAPI, JWT resource server)
- `packages/tokens` — color, spacing, type, and motion tokens
- `infra/keycloak` — realm import and branded login theme
- `docker-compose.yml` — PostgreSQL only (Keycloak runs on your machine)

## Prerequisites

- Node.js 20, pnpm 9 (`corepack enable`)
- JDK 17+ (local JDK 18 is fine; Maven compiles to 17)
- Maven 3.9
- Docker

## Local startup

1. Copy environment values:

```bash
cp .env.example .env
```

Use a real `AUTH_SECRET` (32+ characters) in `.env`. For the Next.js app, also copy the Auth.js variables into `apps/web/.env.local` or export them in your shell.

Login and logout architecture, Keycloak client settings, and a full request example: [docs/authentication.md](docs/authentication.md).

**Rebuild by hand (why + how everything fits):** [docs/implementation-guide.md](docs/implementation-guide.md).

**Next.js files and entry points:** [docs/nextjs.md](docs/nextjs.md).

2. Use the Keycloak instance already running at http://localhost:9090
   - Realm: `herdcommand`
   - Client id in `apps/web/.env.local`: `herdcommand`
   - Redirect URI: `http://localhost:3000/api/auth/callback/keycloak`
   - Web origin: `http://localhost:3000`
   - Do not start a second Keycloak on 9090

3. Start the web app (login first):

```bash
pnpm install
pnpm dev:web
```

Open http://localhost:3000 — you will land on the login screen. Sign in continues to your local Keycloak (authorization code + PKCE), then returns to the app.

4. API (later, not required for login):

```bash
mvn -f apps/api/pom.xml spring-boot:run
```

5. Mobile:

```bash
pnpm dev:mobile
```

Sign in uses Expo Auth Session against `herdcommand-mobile` with PKCE. Complete the flow on a device or emulator; localhost Keycloak is not reachable from a physical phone without a tunnel.

## API conventions

- Base path: `/api/v1`
- Errors: `{ code, message, correlationId, timestamp, path, errors[] }`
- Correlation id: `X-Correlation-Id`
- Pagination (future resources): `page`, `size`, `sort` and `PageResponse`
- Identifiers: UUID strings
- Date/time: ISO-8601 UTC in storage; display is locale-specific in clients
- Auth: Bearer JWT from Keycloak; UI is not the security boundary

## Design tokens

Raw palette and semantic CSS variables live in `packages/tokens`. Reconcile hex values with Figma when the file is available.
