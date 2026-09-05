import { NextResponse } from 'next/server';
import createMiddleware from 'next-intl/middleware';
import { auth } from '@/auth';
import { routing } from './i18n/routing';

const intlMiddleware = createMiddleware(routing);

const protectedMatchers = [
  /^\/me(?:\/|$)/,
  /^\/ar\/me(?:\/|$)/,
  /^\/en\/me(?:\/|$)/,
  /^\/admin(?:\/|$)/,
  /^\/ar\/admin(?:\/|$)/,
  /^\/en\/admin(?:\/|$)/,
  /^\/portal(?:\/|$)/,
  /^\/ar\/portal(?:\/|$)/,
  /^\/en\/portal(?:\/|$)/,
];

export default auth((request) => {
  const { pathname } = request.nextUrl;
  const isProtected = protectedMatchers.some((pattern) => pattern.test(pathname));
  if (isProtected && !request.auth) {
    const login = new URL('/login', request.url);
    login.searchParams.set('callbackUrl', pathname);
    return NextResponse.redirect(login);
  }
  const response = intlMiddleware(request);
  response.headers.set('x-pathname', pathname);
  return response;
});

export const config = {
  matcher: ['/((?!api|_next|_vercel|kc-login-look|.*\\..*).*)'],
};
