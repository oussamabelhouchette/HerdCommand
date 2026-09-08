import { getTranslations, setRequestLocale } from 'next-intl/server';
import { ApiRequestError } from '@/lib/api';
import { loadOwnerFarms } from '@/lib/owner-farms';
import { requireFarmOwner } from '@/lib/require-admin';
import { redirect } from '@/i18n/navigation';
import styles from '@/components/farm/FarmWorkspace.module.css';

type Props = { params: Promise<{ locale: string }> };

export default async function FarmIndexPage({ params }: Props) {
  const { locale } = await params;
  setRequestLocale(locale);
  const gate = await requireFarmOwner(locale);
  const t = await getTranslations('farm');

  if (!gate.allowed || !gate.session.accessToken) {
    return null;
  }

  const { farms, error } = await loadOwnerFarms(gate.session.accessToken, locale);
  if (error) {
    const message = error instanceof ApiRequestError ? error.message : t('loadError');
    return <p className={styles.error}>{message}</p>;
  }
  if (farms[0]) {
    redirect({ href: `/farm/${farms[0].id}`, locale });
  }

  return (
    <section>
      <div className={styles.header}>
        <div className={styles.title}>
          <h1>{t('emptyTitle')}</h1>
          <p>{t('emptyHint')}</p>
        </div>
      </div>
    </section>
  );
}
