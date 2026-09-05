import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
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

function isProtectedPath(pathname: string) {
  return protectedMatchers.some((pattern) => pattern.test(pathname));
}

function withPathname(request: NextRequest, response: NextResponse) {
  response.headers.set('x-pathname', request.nextUrl.pathname);
  return response;
}

const protect = auth((request) => {
  if (!request.auth) {
    const login = new URL('/login', request.url);
    login.searchParams.set('callbackUrl', request.nextUrl.pathname);
    return NextResponse.redirect(login);
  }
  return withPathname(request, intlMiddleware(request));
});

export default function middleware(request: NextRequest) {
  if (!isProtectedPath(request.nextUrl.pathname)) {
    return withPathname(request, intlMiddleware(request));
  }
  return protect(request);
}

export const config = {
  matcher: ['/((?!api|_next|_vercel|kc-login-look|.*\\..*).*)'],
};
