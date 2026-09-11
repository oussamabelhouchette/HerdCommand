import { getTranslations } from 'next-intl/server';
import { logoutAction } from '@/lib/logout';
import type { MeIdentity } from '@/lib/me';
import type { OwnerFarm } from '@/lib/owner-farms';
import { AdminLocaleToggle } from '@/components/admin/AdminLocaleToggle';
import { BellIcon, BrandMarkIcon } from '@/components/ui/Icons';
import { FarmNav } from './FarmNav';
import { FarmSwitcher } from './FarmSwitcher';
import styles from '../admin/AdminShell.module.css';
import type { ReactNode } from 'react';

type Props = {
  children: ReactNode;
  me: MeIdentity;
  farms: OwnerFarm[];
  idToken?: string;
  locale?: string;
};

export async function FarmShell({ children, me, farms, idToken, locale }: Props) {
  const t = await getTranslations();
  const initial = (me.username || me.email || '?').trim().charAt(0).toUpperCase();

  return (
    <div className={styles.shell}>
      <aside className={styles.side}>
        <div className={styles.brand}>
          <span className={styles.brandIcon}>
            <BrandMarkIcon />
          </span>
          {t('app.name')}
        </div>
        <FarmSwitcher farms={farms} />
        <FarmNav farms={farms} />
        <div className={styles.sideFooter}>
          <div className={styles.account}>
            <span className={styles.avatar}>{initial}</span>
            <div>
              <div className={styles.accountName}>{me.username || me.email}</div>
              <div className={styles.role}>{t('farm.roleOwner')}</div>
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
