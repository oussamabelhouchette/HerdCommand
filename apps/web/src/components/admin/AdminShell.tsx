import { getTranslations } from 'next-intl/server';
import { logoutAction } from '@/lib/logout';
import type { MeIdentity } from '@/lib/me';
import { AdminLocaleToggle } from './AdminLocaleToggle';
import { AdminNav } from './AdminNav';
import { BellIcon, BrandMarkIcon } from './AdminIcons';
import styles from './AdminShell.module.css';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  me: MeIdentity;
  idToken?: string;
  locale?: string;
  showAnimalSettings?: boolean;
  showFarmSettings?: boolean;
};

export async function AdminShell({
  children,
  me,
  idToken,
  locale,
  showAnimalSettings = true,
  showFarmSettings = false,
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
        <AdminNav showAnimalSettings={showAnimalSettings} showFarmSettings={showFarmSettings} />
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
            {idToken ? <input type="hidden" name="idToken" value={idToken} /> : null}
            {locale ? <input type="hidden" name="locale" value={locale} /> : null}
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
