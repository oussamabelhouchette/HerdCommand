import { NextResponse } from 'next/server';
import type { NextFetchEvent, NextRequest } from 'next/server';
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
  /^\/farm(?:\/|$)/,
  /^\/ar\/farm(?:\/|$)/,
  /^\/en\/farm(?:\/|$)/,
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

function isRedirect(response: Response) {
  return response.status >= 300 && response.status < 400;
}

const protect = auth((request) => {
  if (!request.auth) {
    const login = new URL('/login', request.url);
    login.searchParams.set('callbackUrl', request.nextUrl.pathname);
    return NextResponse.redirect(login);
  }
  return NextResponse.next();
});

function copySetCookies(from: Response, to: NextResponse) {
  const cookies = typeof from.headers.getSetCookie === 'function' ? from.headers.getSetCookie() : [];
  for (const cookie of cookies) {
    to.headers.append('set-cookie', cookie);
  }
  return to;
}

export default async function middleware(request: NextRequest, event: NextFetchEvent) {
  const intlResponse = withPathname(request, intlMiddleware(request));

  if (!isProtectedPath(request.nextUrl.pathname)) {
    return intlResponse;
  }

  // next-auth wraps the response and drops next-intl's locale cookie, which
  // makes /farm 307 to /farm forever. Let locale redirects finish first.
  if (isRedirect(intlResponse)) {
    return intlResponse;
  }

  const authResponse = await (
    protect as (req: NextRequest, ev: NextFetchEvent) => Promise<Response> | Response
  )(request, event);

  if (isRedirect(authResponse)) {
    return authResponse;
  }

  return copySetCookies(authResponse, intlResponse);
}

export const config = {
  matcher: ['/((?!api|_next|_vercel|kc-login-look|.*\\..*).*)'],
};
