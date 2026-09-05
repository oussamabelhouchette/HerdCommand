import { AppHeader } from '@/components/AppHeader';
import type { ReactNode } from 'react';

export default function SiteLayout({ children }: { children: ReactNode }) {
  return (
    <div className="hc-shell">
      <AppHeader />
      <main>{children}</main>
    </div>
  );
}
