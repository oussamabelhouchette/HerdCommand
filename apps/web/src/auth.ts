import NextAuth from 'next-auth';
import Keycloak from 'next-auth/providers/keycloak';
import type { Session } from 'next-auth';
import { refreshKeycloakAccessToken, accessTokenExpiresAtSeconds, accessTokenNeedsRefresh } from '@/lib/refresh-access-token';

declare module 'next-auth' {
  interface Session {
    accessToken?: string;
    idToken?: string;
    error?: string;
  }
}

declare module 'next-auth/jwt' {
  interface JWT {
    accessToken?: string;
    idToken?: string;
    refreshToken?: string;
    expiresAt?: number;
    error?: string;
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
        token.error = undefined;
        console.info(
          '[HerdCommand] signed in, access token expires in',
          Math.max(0, (token.expiresAt ?? 0) - Math.floor(Date.now() / 1000)),
          'seconds',
        );
        return token;
      }

      const remaining = token.expiresAt ? token.expiresAt - Math.floor(Date.now() / 1000) : 0;
      const needsRefresh = accessTokenNeedsRefresh(token.expiresAt);
      console.info('[HerdCommand] jwt check, access token remaining', remaining, 's, refresh=', needsRefresh);

      if (!needsRefresh) {
        return token;
      }

      console.info('[HerdCommand] access token near expiry, calling Keycloak refresh');

      if (!token.refreshToken) {
        token.error = 'SessionExpired';
        token.accessToken = undefined;
        return token;
      }

      const refreshed = await refreshKeycloakAccessToken(token.refreshToken);
      if ('error' in refreshed) {
        token.error = refreshed.error;
        token.accessToken = undefined;
        return token;
      }

      token.accessToken = refreshed.accessToken;
      token.idToken = refreshed.idToken ?? token.idToken;
      token.refreshToken = refreshed.refreshToken;
      token.expiresAt = refreshed.expiresAt;
      token.error = undefined;
      return token;
    },
    async session({
      session,
      token,
    }: {
      session: Session;
      token: { accessToken?: string; idToken?: string; error?: string };
    }) {
      session.accessToken = token.accessToken;
      session.idToken = token.idToken;
      session.error = token.error;
      return session;
    },
  },
});
