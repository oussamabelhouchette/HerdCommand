import { cache } from 'react';
import { getSession } from '@/lib/session';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { isMeIdentity, loadMe, type MeIdentity } from '@/lib/me';
import { isAdminRole, isAdminShellRole, isFarmOwnerRole, isPlatformAdminRole } from '@/lib/roles';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';
import type { Session } from 'next-auth';

export type AdminGate =
  | { session: Session; me: MeIdentity; allowed: true }
  | { session: Session; me: MeIdentity | null; allowed: false };

function identityFromSession(session: Session): MeIdentity {
  return {
    subject: session.user?.id ?? session.user?.email ?? 'unknown',
    username: session.user?.name ?? session.user?.email ?? 'unknown',
    email: session.user?.email ?? undefined,
    roles: session.roles ?? [],
  };
}

function createRoleGate(isAllowed: (roles: string[] | undefined | null) => boolean) {
  return cache(async (locale: string): Promise<AdminGate> => {
    const session = await getSession();

    if (!session?.user) {
      redirect({ href: '/login', locale });
      throw new Error('redirect');
    }

    if (isAuthSessionError(session.error)) {
      redirectToRoute('/api/auth/federated-logout');
      throw new Error('redirect');
    }

    if (isAllowed(session.roles)) {
      return { session, me: identityFromSession(session), allowed: true };
    }

    const me = await loadMe(session.accessToken);
    if (me && 'unauthorized' in me) {
      redirect({ href: '/login', locale });
      throw new Error('redirect');
    }

    if (!isMeIdentity(me) || !isAllowed(me.roles)) {
      return { session, me: isMeIdentity(me) ? me : null, allowed: false };
    }

    return { session, me, allowed: true };
  });
}

export const requireAdmin = createRoleGate(isAdminRole);
export const requireAdminShell = createRoleGate(isAdminShellRole);
export const requirePlatformAdmin = createRoleGate(isPlatformAdminRole);
export const requireFarmOwner = createRoleGate(isFarmOwnerRole);
