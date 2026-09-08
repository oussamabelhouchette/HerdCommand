'use client';

import { useEffect } from 'react';

export function RedirectIfSignedIn() {
  useEffect(() => {
    let cancelled = false;

    fetch('/api/auth/session')
      .then((response) => response.json())
      .then((session: { user?: unknown; error?: string }) => {
        if (cancelled || !session?.user || session.error) {
          return;
        }
        window.location.replace(window.location.pathname.startsWith('/en') ? '/en' : '/');
      })
      .catch(() => undefined);

    return () => {
      cancelled = true;
    };
  }, []);

  return null;
}
