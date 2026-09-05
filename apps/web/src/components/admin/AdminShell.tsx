import { getTranslations } from 'next-intl/server';
import { Link } from '@/i18n/navigation';
import { logoutAction } from '@/lib/logout';
import type { MeIdentity } from '@/lib/me';
import { AdminLocaleToggle } from './AdminLocaleToggle';
import {
  AnimalIcon,
  BellIcon,
  BrandMarkIcon,
  ChartIcon,
  FarmIcon,
  FolderIcon,
  GridIcon,
  SlidersIcon,
  TasksIcon,
} from './AdminIcons';
import styles from './AdminShell.module.css';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  me: MeIdentity;
  showAnimalSettings?: boolean;
  showFarmSettings?: boolean;
  activeHref?: string;
};

export async function AdminShell({
  children,
  me,
  showAnimalSettings = true,
  showFarmSettings = false,
  activeHref = '',
}: Props) {
  const t = await getTranslations();
  const initial = (me.username || me.email || '?').trim().charAt(0).toUpperCase();
  const platform = showFarmSettings && !showAnimalSettings;

  return (
    <div className={styles.shell}>
      <aside className={styles.side}>
        <div className={styles.brand}>
          <span className={styles.brandIcon}>
            <BrandMarkIcon />
          </span>
          {t('app.name')}
        </div>
        <nav className={styles.nav} aria-label={t('app.name')}>
          <span className={styles.navItem}>
            <GridIcon />
            {t('admin.dashboard')}
          </span>
          {showFarmSettings ? (
            <Link
              href="/admin/farm-settings"
              className={`${styles.navLink} ${activeHref.includes('farm-settings') ? styles.navLinkActive : ''}`}
            >
              <FarmIcon />
              {t('admin.farms')}
            </Link>
          ) : null}
          <span className={styles.navItem}>
            <AnimalIcon />
            {t('admin.animals')}
          </span>
          <span className={styles.navItem}>
            <FolderIcon />
            {t('admin.groups')}
          </span>
          <span className={styles.navItem}>
            <TasksIcon />
            {t('admin.tasks')}
          </span>
          <span className={styles.navItem}>
            <ChartIcon />
            {t('admin.reports')}
          </span>
          {showAnimalSettings ? (
            <Link
              href="/admin/animal-settings"
              className={`${styles.navLink} ${activeHref.includes('animal-settings') ? styles.navLinkActive : ''}`}
            >
              <SlidersIcon />
              {t('admin.settings')}
            </Link>
          ) : null}
        </nav>
        <div className={styles.sideFooter}>
          <div className={styles.account}>
            <span className={styles.avatar}>{initial}</span>
            <div>
              <div className={styles.accountName}>{me.username || me.email}</div>
              <div className={styles.role}>{platform ? t('admin.rolePlatform') : t('admin.roleAdmin')}</div>
            </div>
            <BellIcon className={styles.bell} />
          </div>
          <AdminLocaleToggle />
          <form action={logoutAction}>
            <button type="submit" className={styles.signOut}>
              {t('nav.signOut')}
            </button>
          </form>
        </div>
      </aside>
      <main className={styles.main}>{children}</main>
    </div>
  );
}
