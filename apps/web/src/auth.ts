import NextAuth from 'next-auth';
import Keycloak from 'next-auth/providers/keycloak';
import type { Session } from 'next-auth';
import { refreshKeycloakAccessToken, accessTokenExpiresAtSeconds, accessTokenNeedsRefresh } from '@/lib/refresh-access-token';
import { membershipsFromAccessToken } from '@/lib/realm-roles';
import { resolveAuthJsRedirect } from '@/lib/post-login-redirect';

declare module 'next-auth' {
  interface Session {
    accessToken?: string;
    idToken?: string;
    error?: string;
    roles?: string[];
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    accessToken?: string;
    idToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    error?: string;
    roles?: string[];
  }
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  trustHost: true,
  providers: [
    Keycloak({
      clientId: process.env.AUTH_KEYCLOAK_ID,
      clientSecret: process.env.AUTH_KEYCLOAK_SECRET || undefined,
      issuer: process.env.AUTH_KEYCLOAK_ISSUER,
      checks: ['pkce', 'state'],
      client: {
        token_endpoint_auth_method: 'none',
      },
    }),
  ],
  session: { strategy: 'jwt', maxAge: 30 * 60 },
  pages: {
    signIn: '/login',
  },
  callbacks: {
    async jwt({ token, account }) {
      if (account) {
        token.accessToken = account.access_token;
        token.idToken = account.id_token;
        token.refreshToken = account.refresh_token;
        token.expiresAt = accessTokenExpiresAtSeconds(account);
        token.roles = membershipsFromAccessToken(account.access_token);
        token.error = undefined;
        return token;
      }

      const needsRefresh = accessTokenNeedsRefresh(token.expiresAt);

      if (!needsRefresh) {
        if (!token.roles?.length && token.accessToken) {
          token.roles = membershipsFromAccessToken(token.accessToken);
        }
        return token;
      }

      if (!token.refreshToken) {
        token.error = 'SessionExpired';
        token.accessToken = undefined;
        token.roles = undefined;
        return token;
      }

      const refreshed = await refreshKeycloakAccessToken(token.refreshToken);
      if ('error' in refreshed) {
        token.error = refreshed.error;
        token.accessToken = undefined;
        token.roles = undefined;
        return token;
      }

      token.accessToken = refreshed.accessToken;
      token.idToken = refreshed.idToken ?? token.idToken;
      token.refreshToken = refreshed.refreshToken;
      token.expiresAt = refreshed.expiresAt;
      token.roles = membershipsFromAccessToken(refreshed.accessToken);
      token.error = undefined;
      return token;
    },
    async session({
      session,
      token,
    }: {
      session: Session;
      token: { accessToken?: string; idToken?: string; error?: string; roles?: string[] };
    }) {
      session.accessToken = token.accessToken;
      session.idToken = token.idToken;
      session.error = token.error;
      session.roles = token.roles;
      return session;
    },
    async redirect({ url, baseUrl }) {
      return resolveAuthJsRedirect(url, baseUrl);
    },
  },
});
