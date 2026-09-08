import { cache } from 'react';
import { cookies } from 'next/headers';
import { auth } from '@/auth';
import { hasAuthJsSessionCookie } from '@/lib/session-cookie';

export const getSession = cache(async () => {
  const jar = await cookies();
  if (!hasAuthJsSessionCookie(jar.getAll().map((cookie) => cookie.name))) {
    return null;
  }
  return auth();
});
