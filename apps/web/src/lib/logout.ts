'use server';

import { redirect } from 'next/navigation';
import { auth, signOut } from '@/auth';
import { buildKeycloakLogoutUrl } from '@/lib/keycloak-logout-url';

export async function logoutAction() {
  const session = await auth();
  await signOut({ redirect: false });
  redirect(buildKeycloakLogoutUrl(session?.idToken));
}
