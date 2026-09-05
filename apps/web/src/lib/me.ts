import { apiBaseUrl } from './api';

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

export async function loadMe(accessToken?: string): Promise<MeLoadResult> {
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
}
