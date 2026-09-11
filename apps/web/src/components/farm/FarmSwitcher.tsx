'use client';

import { useTranslations } from 'next-intl';
import { usePathname, useRouter } from '@/i18n/navigation';
import { farmHref, farmIdFromPath, findOwnerFarm, type OwnerFarm } from '@/lib/owner-farms';
import styles from '../admin/AdminShell.module.css';

type Props = {
  farms: OwnerFarm[];
};

export function FarmSwitcher({ farms }: Props) {
  const t = useTranslations('farm');
  const pathname = usePathname();
  const router = useRouter();
  const pathId = farmIdFromPath(pathname);
  const selectedId = findOwnerFarm(farms, pathId)?.id ?? farms[0]?.id;

  if (farms.length === 0) {
    return null;
  }

  if (farms.length === 1) {
    return <div className={styles.farmName}>{farms[0].name}</div>;
  }

  return (
    <div className={styles.farmSwitch}>
      <label className={styles.farmSwitchLabel} htmlFor="farm-switcher">
        {t('switchFarm')}
      </label>
      <select
        id="farm-switcher"
        className={styles.farmSelect}
        value={selectedId}
        onChange={(event) => router.replace(farmHref(event.target.value, pathname))}
      >
        {farms.map((farm) => (
          <option key={farm.id} value={farm.id}>
            {farm.name}
          </option>
        ))}
      </select>
    </div>
  );
}
