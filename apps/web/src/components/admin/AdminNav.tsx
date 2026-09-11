'use client';

import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import { Link, usePathname } from '@/i18n/navigation';
import { FarmIcon, SlidersIcon } from '@/components/ui/Icons';
import styles from './AdminShell.module.css';

type Props = {
  showAnimalSettings: boolean;
  showFarmSettings: boolean;
};

function isCurrent(pathname: string, href: string) {
  return pathname === href || pathname.endsWith(href);
}

export function AdminNav({ showAnimalSettings, showFarmSettings }: Props) {
  const t = useTranslations('admin');
  const pathname = usePathname();
  const [pending, setPending] = useState<string | null>(null);

  useEffect(() => {
    setPending(null);
  }, [pathname]);

  const current = pending ?? pathname;

  return (
    <nav className={styles.nav}>
      {showFarmSettings ? (
        <Link
          href="/admin/farm-settings"
          prefetch
          onClick={() => setPending('/admin/farm-settings')}
          className={`${styles.navLink} ${isCurrent(current, '/admin/farm-settings') ? styles.navLinkActive : ''}`}
          aria-current={isCurrent(current, '/admin/farm-settings') ? 'page' : undefined}
        >
          <FarmIcon />
          {t('farms')}
        </Link>
      ) : null}
      {showAnimalSettings ? (
        <Link
          href="/admin/animal-settings"
          prefetch
          onClick={() => setPending('/admin/animal-settings')}
          className={`${styles.navLink} ${isCurrent(current, '/admin/animal-settings') ? styles.navLinkActive : ''}`}
          aria-current={isCurrent(current, '/admin/animal-settings') ? 'page' : undefined}
        >
          <SlidersIcon />
          {t('settings')}
        </Link>
      ) : null}
    </nav>
  );
}
