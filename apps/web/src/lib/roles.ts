import { normalizeMembership } from './portals';

const ADMIN_ROLES = new Set(['administrator', 'administrators', 'owner']);

export function isAdminRole(roles: string[] | undefined | null): boolean {
  if (!roles?.length) {
    return false;
  }
  return roles.some((role) => ADMIN_ROLES.has(normalizeMembership(role)));
}

export function normalizePlatformRole(role: string): string {
  return normalizeMembership(role).replace(/-/g, '_');
}

export function isPlatformAdminRole(roles: string[] | undefined | null): boolean {
  if (!roles?.length) {
    return false;
  }
  return roles.some((role) => normalizePlatformRole(role) === 'platform_admin');
}

export function isAdminShellRole(roles: string[] | undefined | null): boolean {
  return isAdminRole(roles) || isPlatformAdminRole(roles);
}

export function isFarmOwnerRole(roles: string[] | undefined | null): boolean {
  if (!roles?.length) {
    return false;
  }
  return roles.some((role) => normalizePlatformRole(role) === 'farm_owner');
}
