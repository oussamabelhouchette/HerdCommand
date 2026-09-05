'use client';

import { SessionProvider } from 'next-auth/react';
import type { ReactNode } from 'react';

export function AuthSessionProvider({ children }: { children: ReactNode }) {
  return (
    <SessionProvider refetchInterval={120} refetchOnWindowFocus={false}>
      {children}
    </SessionProvider>
  );
}
