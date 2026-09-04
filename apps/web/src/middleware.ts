import { NextResponse } from 'next/server';
import createMiddleware from 'next-intl/middleware';
import { auth } from '@/auth';
import { routing } from './i18n/routing';

const intlMiddleware = createMiddleware(routing);

const protectedMatchers = [/^\/me(?:\/|$)/, /^\/ar\/me(?:\/|$)/, /^\/en\/me(?:\/|$)/];

export default auth((request) => {
  const { pathname } = request.nextUrl;
  const isProtected = protectedMatchers.some((pattern) => pattern.test(pathname));
  if (isProtected && !request.auth) {
    const login = new URL('/login', request.url);
    login.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(login);
  }
  return intlMiddleware(request);
});

export const config = {
  matcher: ['/((?!api|_next|_vercel|kc-login-look|.*\\..*).*)'],
};
