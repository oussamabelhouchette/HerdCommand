import { cache } from 'react';
import { apiBaseUrl } from './api';
import { collectMemberships } from './portals';

export type MeIdentity = {
  subject: string;
  username: string;
  email?: string;
  roles: string[];
};

export type MeLoadResult = MeIdentity | { unauthorized: true } | { error: true } | null;

export function isMeIdentity(me: MeLoadResult): me is MeIdentity {
  return me !== null && 'subject' in me;
}

export const loadMe = cache(async (accessToken?: string): Promise<MeLoadResult> => {
  if (!accessToken) {
    return null;
  }
  const response = await fetch(`${apiBaseUrl()}/api/v1/me`, {
    headers: { Authorization: `Bearer ${accessToken}` },
    cache: 'no-store',
  });
  if (response.status === 401) {
    return { unauthorized: true };
  }
  if (!response.ok) {
    return { error: true };
  }
  return response.json() as Promise<MeIdentity>;
});

/** Use token roles when present so pages do not wait on /api/v1/me. */
export async function membershipsFromSession(session?: {
  roles?: string[] | null;
  accessToken?: string;
} | null): Promise<string[]> {
  const roles = session?.roles ?? [];
  if (roles.length > 0 || !session?.accessToken) {
    return roles;
  }
  const me = await loadMe(session.accessToken);
  return isMeIdentity(me) ? collectMemberships(roles, me.roles) : roles;
}
