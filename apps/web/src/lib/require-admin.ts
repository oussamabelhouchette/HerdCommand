import { cache } from 'react';
import { getSession } from '@/lib/session';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { isMeIdentity, loadMe, type MeIdentity } from '@/lib/me';
import { isAdminRole } from '@/lib/roles';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';
import type { Session } from 'next-auth';

function identityFromSession(session: Session): MeIdentity {
  return {
    subject: session.user?.id ?? session.user?.email ?? 'unknown',
    username: session.user?.name ?? session.user?.email ?? 'unknown',
    email: session.user?.email ?? undefined,
    roles: session.roles ?? [],
  };
}

export const requireAdmin = cache(async (
  locale: string,
): Promise<
  | { session: Session; me: MeIdentity; allowed: true }
  | { session: Session; me: MeIdentity | null; allowed: false }
> => {
  const session = await getSession();

  if (!session?.user) {
    redirect({ href: '/login', locale });
    throw new Error('redirect');
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
    throw new Error('redirect');
  }

  if (isAdminRole(session.roles)) {
    return { session, me: identityFromSession(session), allowed: true };
  }

  const me = await loadMe(session.accessToken);
  if (me && 'unauthorized' in me) {
    redirect({ href: '/login', locale });
    throw new Error('redirect');
  }

  if (!isMeIdentity(me) || !isAdminRole(me.roles)) {
    return { session, me: isMeIdentity(me) ? me : null, allowed: false };
  }

  return { session, me, allowed: true };
});
