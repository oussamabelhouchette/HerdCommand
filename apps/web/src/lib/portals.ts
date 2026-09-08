/**
 * Keycloak group or realm-role name → app portal path.
 * Add rows here when a new group should land on a different portal.
 * Names are matched case-insensitively; a leading slash is ignored (/administrator).
 */
export const PORTAL_BY_MEMBERSHIP: Record<string, string> = {
  platform_admin: '/admin/farm-settings',
  administrator: '/admin/animal-settings',
  administrators: '/admin/animal-settings',
  owner: '/admin/animal-settings',
  farm_owner: '/farm',
};

export const DEFAULT_PORTAL = '/portal';

export function normalizeMembership(name: string): string {
  const parts = name.trim().toLowerCase().replace(/^\/+/, '').split('/').filter(Boolean);
  return parts[parts.length - 1] ?? '';
}

export function portalHref(memberships: string[] | undefined | null): string {
  if (!memberships?.length) {
    return DEFAULT_PORTAL;
  }
  for (const raw of memberships) {
    const key = normalizeMembership(raw).replace(/-/g, '_');
    const path = PORTAL_BY_MEMBERSHIP[key];
    if (path) {
      return path;
    }
  }
  return DEFAULT_PORTAL;
}

export function isAdminPortal(path: string): boolean {
  return path === '/admin' || path.startsWith('/admin/');
}

export function isFarmPortal(path: string): boolean {
  return path === '/farm' || path.startsWith('/farm/');
}

export function collectMemberships(...lists: Array<string[] | undefined | null>): string[] {
  return [...new Set(lists.flatMap((list) => list ?? []))];
}
