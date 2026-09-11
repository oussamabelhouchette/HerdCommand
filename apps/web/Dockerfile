# Build from the repo root (web needs packages/tokens):
#   docker build -f apps/web/Dockerfile -t herdcommand-web:test .
#
# Stage 1: install pnpm workspace and run next build.
FROM node:20-bookworm-slim AS build
WORKDIR /repo
RUN corepack enable && corepack prepare pnpm@9.15.4 --activate
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/tokens/package.json ./packages/tokens/package.json
COPY apps/web/package.json ./apps/web/package.json
COPY apps/mobile/package.json ./apps/mobile/package.json
RUN pnpm install --frozen-lockfile --filter @herdcommand/web...
COPY packages/tokens ./packages/tokens
COPY apps/web ./apps/web
ENV AUTH_SECRET=docker-test-secret-change-me-32chars!!
ENV AUTH_TRUST_HOST=true
ENV AUTH_KEYCLOAK_ID=herdcommand
ENV AUTH_KEYCLOAK_SECRET=web-dev-secret
ENV AUTH_KEYCLOAK_ISSUER=http://localhost:9090/realms/herdcommand
ENV AUTH_KEYCLOAK_INTERNAL_ISSUER=http://keycloak:8080/realms/herdcommand
ENV NEXTAUTH_URL=http://localhost:3000
ENV NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ENV API_INTERNAL_URL=http://api:8080
RUN pnpm --filter @herdcommand/web build

# Stage 2: run the standalone Next server (no pnpm, no source).
FROM node:20-bookworm-slim
WORKDIR /repo
ENV NODE_ENV=production
ENV HOSTNAME=0.0.0.0
ENV PORT=3000
ENV AUTH_SECRET=docker-test-secret-change-me-32chars!!
ENV AUTH_TRUST_HOST=true
ENV AUTH_KEYCLOAK_ID=herdcommand
ENV AUTH_KEYCLOAK_SECRET=web-dev-secret
ENV AUTH_KEYCLOAK_ISSUER=http://localhost:9090/realms/herdcommand
ENV AUTH_KEYCLOAK_INTERNAL_ISSUER=http://keycloak:8080/realms/herdcommand
ENV NEXTAUTH_URL=http://localhost:3000
ENV NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
ENV API_INTERNAL_URL=http://api:8080
ENV NODE_OPTIONS=--dns-result-order=ipv4first
COPY --from=build /repo/apps/web/public ./apps/web/public
COPY --from=build /repo/apps/web/.next/standalone ./
COPY --from=build /repo/apps/web/.next/static ./apps/web/.next/static
WORKDIR /repo/apps/web
EXPOSE 3000
CMD ["node", "server.js"]
