import { normalizeMembership } from './portals';

const ADMIN_ROLES = new Set(['administrator', 'administrators', 'owner']);

export function isAdminRole(roles: string[] | undefined | null): boolean {
  if (!roles?.length) {
    return false;
  }
  return roles.some((role) => ADMIN_ROLES.has(normalizeMembership(role)));
}
