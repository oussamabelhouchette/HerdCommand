'use client';

import { useEffect, useState } from 'react';
import { useTranslations } from 'next-intl';
import { Link, usePathname } from '@/i18n/navigation';
import { AnimalIcon, GridIcon } from '@/components/admin/AdminIcons';
import { farmIdFromPath, type OwnerFarm } from '@/lib/owner-farms';
import styles from '../admin/AdminShell.module.css';

type Props = {
  farms: OwnerFarm[];
};

function isDashboard(pathname: string, farmId: string) {
  return pathname === `/farm/${farmId}`;
}

function isAnimals(pathname: string, farmId: string) {
  return pathname === `/farm/${farmId}/animals` || pathname.startsWith(`/farm/${farmId}/animals/`);
}

export function FarmNav({ farms }: Props) {
  const t = useTranslations('farm');
  const pathname = usePathname();
  const [pending, setPending] = useState<string | null>(null);
  const farmId = farmIdFromPath(pathname) ?? farms[0]?.id;

  useEffect(() => {
    setPending(null);
  }, [pathname]);

  if (!farmId) {
    return null;
  }

  const current = pending ?? pathname;

  return (
    <nav className={styles.nav}>
      <Link
        href={`/farm/${farmId}`}
        prefetch
        onClick={() => setPending(`/farm/${farmId}`)}
        className={`${styles.navLink} ${isDashboard(current, farmId) ? styles.navLinkActive : ''}`}
        aria-current={isDashboard(current, farmId) ? 'page' : undefined}
      >
        <GridIcon />
        {t('dashboard')}
      </Link>
      <Link
        href={`/farm/${farmId}/animals`}
        prefetch
        onClick={() => setPending(`/farm/${farmId}/animals`)}
        className={`${styles.navLink} ${isAnimals(current, farmId) ? styles.navLinkActive : ''}`}
        aria-current={isAnimals(current, farmId) ? 'page' : undefined}
      >
        <AnimalIcon />
        {t('animals')}
      </Link>
    </nav>
  );
}
