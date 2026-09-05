import { auth } from '@/auth';
import { isAuthSessionError } from '@/lib/refresh-access-token';
import { isMeIdentity, loadMe, type MeIdentity } from '@/lib/me';
import { isAdminRole } from '@/lib/roles';
import { redirect } from '@/i18n/navigation';
import { redirect as redirectToRoute } from 'next/navigation';
import type { Session } from 'next-auth';

export async function requireAdmin(locale: string): Promise<
  | { session: Session; me: MeIdentity; allowed: true }
  | { session: Session; me: MeIdentity | null; allowed: false }
> {
  const session = await auth();

  if (!session?.user) {
    redirect({ href: '/login', locale });
    throw new Error('redirect');
  }

  if (isAuthSessionError(session.error)) {
    redirectToRoute('/api/auth/federated-logout');
    throw new Error('redirect');
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
}
