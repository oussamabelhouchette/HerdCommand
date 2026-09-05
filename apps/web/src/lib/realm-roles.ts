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

function stringList(value: unknown): string[] {
  if (!Array.isArray(value)) {
    return [];
  }
  return value.filter((item): item is string => typeof item === 'string' && item.trim().length > 0);
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
  return stringList((realmAccess as RealmAccess).roles);
}

export function groupsFromAccessToken(accessToken?: string | null): string[] {
  const payload = decodeJwtPayload(accessToken);
  if (!payload) {
    return [];
  }
  return stringList(payload.groups);
}

export function membershipsFromAccessToken(accessToken?: string | null): string[] {
  return [...new Set([...realmRolesFromAccessToken(accessToken), ...groupsFromAccessToken(accessToken)])];
}
