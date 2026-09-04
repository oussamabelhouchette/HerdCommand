type RefreshedTokens = {
  accessToken: string;
  idToken?: string;
  refreshToken: string;
  expiresAt: number;
};

type RefreshFailure = {
  error: 'RefreshAccessTokenError';
};

const refreshInflight = new Map<string, Promise<RefreshedTokens | RefreshFailure>>();
const refreshCache = new Map<string, { result: RefreshedTokens; storedAt: number }>();
const REFRESH_CACHE_MS = 60_000;

export async function refreshKeycloakAccessToken(refreshToken: string): Promise<RefreshedTokens | RefreshFailure> {
  const cached = refreshCache.get(refreshToken);
  if (cached && Date.now() - cached.storedAt < REFRESH_CACHE_MS) {
    console.info('[HerdCommand] token refresh reused (same process, Keycloak already rotated this token)');
    return cached.result;
  }

  const pending = refreshInflight.get(refreshToken);
  if (pending) {
    return pending;
  }

  const work = doRefreshKeycloakAccessToken(refreshToken).then((result) => {
    if (!('error' in result)) {
      refreshCache.set(refreshToken, { result, storedAt: Date.now() });
      refreshCache.set(result.refreshToken, { result, storedAt: Date.now() });
    }
    refreshInflight.delete(refreshToken);
    return result;
  });
  refreshInflight.set(refreshToken, work);
  return work;
}

async function doRefreshKeycloakAccessToken(refreshToken: string): Promise<RefreshedTokens | RefreshFailure> {
  const issuer = process.env.AUTH_KEYCLOAK_ISSUER?.replace(/\/$/, '');
  const clientId = process.env.AUTH_KEYCLOAK_ID;
  if (!issuer || !clientId || !refreshToken) {
    return { error: 'RefreshAccessTokenError' };
  }

  const body = new URLSearchParams({
    grant_type: 'refresh_token',
    client_id: clientId,
    refresh_token: refreshToken,
  });

  const response = await fetch(`${issuer}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
    cache: 'no-store',
  });

  const payload = (await response.json()) as {
    access_token?: string;
    id_token?: string;
    refresh_token?: string;
    expires_in?: number;
    error?: string;
  };

  if (!response.ok || !payload.access_token) {
    console.warn('[HerdCommand] token refresh failed', payload.error ?? response.status);
    return { error: 'RefreshAccessTokenError' };
  }

  console.info(
    '[HerdCommand] token refresh OK, new access token lasts',
    payload.expires_in ?? 60,
    'seconds',
  );

  return {
    accessToken: payload.access_token,
    idToken: payload.id_token,
    refreshToken: payload.refresh_token ?? refreshToken,
    expiresAt: Math.floor(Date.now() / 1000) + (payload.expires_in ?? 60),
  };
}

export function accessTokenExpiresAtSeconds(account: {
  expires_at?: number;
  expires_in?: number;
}) {
  if (account.expires_at && account.expires_at > 1_000_000_000_000) {
    return Math.floor(account.expires_at / 1000);
  }
  if (account.expires_at && account.expires_at > 1_000_000_000) {
    return account.expires_at;
  }
  return Math.floor(Date.now() / 1000) + (Number(account.expires_in) || 60);
}

export function accessTokenNeedsRefresh(expiresAt?: number) {
  if (!expiresAt) {
    return true;
  }
  const expiresAtMs = expiresAt > 1_000_000_000_000 ? expiresAt : expiresAt * 1000;
  return Date.now() >= expiresAtMs - 15_000;
}

export function isAuthSessionError(error?: string) {
  return error === 'SessionExpired' || error === 'RefreshAccessTokenError';
}
