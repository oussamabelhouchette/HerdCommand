'use client';

import { signIn } from 'next-auth/react';
import { Button } from './Button';

export function SignInButton({ label, callbackUrl }: { label: string; callbackUrl: string }) {
  return (
    <Button type="button" onClick={() => signIn('keycloak', { callbackUrl })}>
      {label}
    </Button>
  );
}
