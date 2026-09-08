import { NextResponse } from 'next/server';
import { signOut } from '@/auth';
import { buildKeycloakLogoutUrl } from '@/lib/keycloak-logout-url';

export async function GET() {
  await signOut({ redirect: false });
  return NextResponse.redirect(buildKeycloakLogoutUrl());
}
