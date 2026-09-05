import { getTranslations } from 'next-intl/server';
import { CheckIcon, ClockIcon, FarmIcon, LayersIcon, PlusIcon } from './AdminIcons';
import styles from './FarmSettings.module.css';

type Props = {
  apiReady: boolean;
  loadError?: string;
};

export async function FarmSettings({ apiReady, loadError }: Props) {
  const t = await getTranslations('farmSettings');
  const pending = t('statPendingValue');

  return (
    <>
      <header className={styles.header}>
        <div className={styles.title}>
          <h1>{t('title')}</h1>
          <p>{t('subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button type="button" className={`${styles.btn} ${styles.btnPrimary}`} disabled>
            <PlusIcon />
            {t('addFarm')}
          </button>
        </div>
      </header>

      <section className={styles.stats}>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <FarmIcon />
          </div>
          <div>
            <strong>{pending}</strong>
            <span>{t('statFarms')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <CheckIcon />
          </div>
          <div>
            <strong>{pending}</strong>
            <span>{t('statActive')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <LayersIcon />
          </div>
          <div>
            <strong>{pending}</strong>
            <span>{t('statAnimals')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <ClockIcon />
          </div>
          <div>
            <strong>{pending}</strong>
            <span>{t('statPending')}</span>
          </div>
        </div>
      </section>

      <section className={styles.panel}>
        {loadError ? (
          <p className={`${styles.status} ${styles.statusError}`}>{loadError}</p>
        ) : apiReady ? (
          <p className={styles.status}>{t('apiReady')}</p>
        ) : null}
        <p className={styles.note}>{t('note')}</p>
        <ul className={styles.soon}>
          <li>
            <span className={styles.soonTitle}>{t('soonTitle')}</span>
            {t('soonList')}
          </li>
          <li>
            <span className={styles.soonTitle}>{t('soonTitle')}</span>
            {t('soonCreate')}
          </li>
          <li>
            <span className={styles.soonTitle}>{t('soonTitle')}</span>
            {t('soonEdit')}
          </li>
        </ul>
      </section>
    </>
  );
}
