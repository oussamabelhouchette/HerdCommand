import { NextResponse } from 'next/server';
import { auth, signOut } from '@/auth';
import { buildKeycloakLogoutUrl } from '@/lib/keycloak-logout-url';

export async function GET() {
  const session = await auth();
  await signOut({ redirect: false });
  return NextResponse.redirect(buildKeycloakLogoutUrl(session?.idToken));
}
