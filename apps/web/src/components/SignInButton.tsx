'use client';

import { signIn } from 'next-auth/react';
import { safeSameOriginPath } from '@/lib/post-login-redirect';
import { Button } from './Button';

export function SignInButton({ label, continuePath }: { label: string; continuePath: string }) {
  return (
    <Button
      type="button"
      onClick={() => {
        const intended = safeSameOriginPath(
          new URLSearchParams(window.location.search).get('callbackUrl'),
        );
        signIn('keycloak', { callbackUrl: `${continuePath}?next=${encodeURIComponent(intended)}` });
      }}
    >
      {label}
    </Button>
  );
}
