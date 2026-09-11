import NextAuth from 'next-auth';
import Keycloak from 'next-auth/providers/keycloak';
import { refreshKeycloakAccessToken, accessTokenExpiresAtSeconds, accessTokenNeedsRefresh } from '@/lib/refresh-access-token';
import { membershipsFromAccessToken } from '@/lib/realm-roles';
import { resolveAuthJsRedirect } from '@/lib/post-login-redirect';
import { runtimeEnv } from '@/lib/runtime-env';

declare module 'next-auth' {
  interface Session {
    accessToken?: string;
    idToken?: string;
    error?: string;
    roles?: string[];
  }
}

type AuthToken = {
  accessToken?: string;
  idToken?: string;
  refreshToken?: string;
  expiresAt?: number;
  error?: string;
  roles?: string[];
};

function keycloakAuthOptions() {
  const publicIssuer = runtimeEnv('AUTH_KEYCLOAK_ISSUER')?.replace(/\/$/, '');
  const internalIssuer = (runtimeEnv('AUTH_KEYCLOAK_INTERNAL_ISSUER') || publicIssuer)?.replace(/\/$/, '');
  const keycloakSecret = runtimeEnv('AUTH_KEYCLOAK_SECRET');
  return {
    clientId: runtimeEnv('AUTH_KEYCLOAK_ID'),
    clientSecret: keycloakSecret,
    issuer: publicIssuer,
    // Auth.js discovers from `issuer` unless these URLs are set. Issuer is the
    // browser URL (localhost); token/userinfo must be reachable from Docker.
    authorization: publicIssuer ? `${publicIssuer}/protocol/openid-connect/auth` : undefined,
    token: internalIssuer ? `${internalIssuer}/protocol/openid-connect/token` : undefined,
    userinfo: internalIssuer ? `${internalIssuer}/protocol/openid-connect/userinfo` : undefined,
    checks: ['pkce', 'state'] as Array<'pkce' | 'state'>,
    client: {
      token_endpoint_auth_method: (keycloakSecret ? 'client_secret_post' : 'none') as
        | 'client_secret_post'
        | 'none',
    },
  };
}

export const { handlers, auth, signIn, signOut } = NextAuth({
  trustHost: true,
  providers: [Keycloak(keycloakAuthOptions())],
  session: { strategy: 'jwt', maxAge: 30 * 60 },
  pages: {
    signIn: '/login',
  },
  callbacks: {
    async jwt({ token, account }) {
      const authToken = token as typeof token & AuthToken;
      if (account) {
        authToken.accessToken = account.access_token;
        authToken.idToken = account.id_token;
        authToken.refreshToken = account.refresh_token;
        authToken.expiresAt = accessTokenExpiresAtSeconds(account);
        authToken.roles = membershipsFromAccessToken(account.access_token);
        authToken.error = undefined;
        return authToken;
      }

      const needsRefresh = accessTokenNeedsRefresh(authToken.expiresAt);

      if (!needsRefresh) {
        if (!authToken.roles?.length && authToken.accessToken) {
          authToken.roles = membershipsFromAccessToken(authToken.accessToken);
        }
        return authToken;
      }

      if (!authToken.refreshToken) {
        authToken.error = 'SessionExpired';
        authToken.accessToken = undefined;
        authToken.roles = undefined;
        return authToken;
      }

      const refreshed = await refreshKeycloakAccessToken(authToken.refreshToken);
      if ('error' in refreshed) {
        authToken.error = refreshed.error;
        authToken.accessToken = undefined;
        authToken.roles = undefined;
        return authToken;
      }

      authToken.accessToken = refreshed.accessToken;
      authToken.idToken = refreshed.idToken ?? authToken.idToken;
      authToken.refreshToken = refreshed.refreshToken;
      authToken.expiresAt = refreshed.expiresAt;
      authToken.roles = membershipsFromAccessToken(refreshed.accessToken);
      authToken.error = undefined;
      return authToken;
    },
    async session({ session, token }) {
      const authToken = token as AuthToken;
      session.accessToken = authToken.accessToken;
      session.idToken = authToken.idToken;
      session.error = authToken.error;
      session.roles = authToken.roles;
      return session;
    },
    async redirect({ url, baseUrl }) {
      return resolveAuthJsRedirect(url, baseUrl);
    },
  },
});
