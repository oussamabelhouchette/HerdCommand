import { apiFetch } from './api';

export type IdentityLookup = {
  found: boolean;
  invitationRequired: boolean;
  keycloakUserId?: string | null;
  email: string;
  displayName?: string | null;
  enabled?: boolean | null;
};

export type OwnerLookupStatus = 'idle' | 'invalid' | 'loading' | 'found' | 'invitation' | 'disabled' | 'error';

export function normalizeEmail(raw: string): string {
  return raw.trim().toLowerCase();
}

export function isValidEmail(raw: string): boolean {
  return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(normalizeEmail(raw));
}

export function lookupIdentity(accessToken: string, locale: string, email: string) {
  return apiFetch<IdentityLookup>('/api/v1/platform/identity/lookup', {
    accessToken,
    locale,
    method: 'POST',
    body: { email: normalizeEmail(email) },
  });
}

export function ownerLookupStatus(result: IdentityLookup | null, options: { loading?: boolean; error?: boolean; email?: string } = {}): OwnerLookupStatus {
  if (options.loading) {
    return 'loading';
  }
  if (options.error) {
    return 'error';
  }
  const email = options.email ?? '';
  if (!email.trim()) {
    return 'idle';
  }
  if (!isValidEmail(email)) {
    return 'invalid';
  }
  if (!result) {
    return 'idle';
  }
  if (result.found && result.enabled === false) {
    return 'disabled';
  }
  if (result.found) {
    return 'found';
  }
  if (result.invitationRequired) {
    return 'invitation';
  }
  return 'idle';
}
