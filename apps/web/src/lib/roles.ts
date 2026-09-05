const ADMIN_ROLES = new Set(['administrator', 'owner']);


export function isAdminRole(roles: string[] | undefined | null): boolean {
  if (!roles?.length) {
    return false;
  }
  return roles.some((role) => ADMIN_ROLES.has(role.trim().toLowerCase()));
}
