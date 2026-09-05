'use server';

import { redirect } from 'next/navigation';
import { signOut } from '@/auth';
import { buildKeycloakLogoutUrl } from '@/lib/keycloak-logout-url';

export async function logoutAction(formData?: FormData) {
  const idToken = formData?.get('idToken');
  const locale = formData?.get('locale');
  await signOut({ redirect: false });
  redirect(
    buildKeycloakLogoutUrl(
      typeof idToken === 'string' && idToken ? idToken : undefined,
      typeof locale === 'string' && locale ? locale : undefined,
    ),
  );
}
