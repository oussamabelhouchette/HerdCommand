type RealmAccess = {
  roles?: unknown;
};

export function decodeJwtPayload(token: string | undefined | null): Record<string, unknown> | null {
  if (!token) {
    return null;
  }
  const parts = token.split('.');
  if (parts.length < 2) {
    return null;
  }
  try {
    const json = Buffer.from(parts[1], 'base64url').toString('utf8');
    const payload = JSON.parse(json) as unknown;
    if (!payload || typeof payload !== 'object' || Array.isArray(payload)) {
      return null;
    }
    return payload as Record<string, unknown>;
  } catch {
    return null;
  }
}

export function realmRolesFromAccessToken(accessToken?: string | null): string[] {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) {
    return [];
  }
  const realmAccess = payload.realm_access;
  if (!realmAccess || typeof realmAccess !== 'object' || Array.isArray(realmAccess)) {
    return [];
  }
  const roles = (realmAccess as RealmAccess).roles;
  if (!Array.isArray(roles)) {
    return [];
  }
  return roles.filter((role): role is string => typeof role === 'string' && role.trim().length > 0);
}
