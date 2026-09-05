import { getTranslations } from 'next-intl/server';
import { CheckIcon, ClockIcon, FarmIcon, LayersIcon, PlusIcon } from './AdminIcons';
import type { CatalogFeature } from '@/lib/features';
import { selectedFeatureCodes } from '@/lib/features';
import styles from './FarmSettings.module.css';

type Props = {
  apiReady: boolean;
  loadError?: string;
  features: CatalogFeature[];
};

export async function FarmSettings({ apiReady, loadError, features }: Props) {
  const t = await getTranslations('farmSettings');
  const pending = t('statPendingValue');
  const selectedCodes = selectedFeatureCodes(features);

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
        <h2 className={styles.featuresTitle}>{t('featuresTitle')}</h2>
        <p className={styles.featuresHint}>{t('featuresHint')}</p>
        {features.length === 0 && !loadError ? (
          <p className={styles.note}>{t('featuresEmpty')}</p>
        ) : (
          <ul className={styles.features} data-selected-codes={selectedCodes.join(',')}>
            {features.map((feature) => {
              const disabled = !feature.enableable;
              return (
                <li
                  key={feature.id}
                  className={disabled ? styles.featureDisabled : undefined}
                  data-feature-id={feature.id}
                  data-feature-code={feature.code}
                  data-enableable={String(feature.enableable)}
                  aria-disabled={disabled}
                >
                  <label className={styles.featureRow}>
                    <input
                      type="checkbox"
                      checked={feature.enableable}
                      disabled={disabled}
                      readOnly
                      value={feature.code}
                    />
                    <span>
                      <strong>{feature.name}</strong>
                      {feature.description ? <em>{feature.description}</em> : null}
                    </span>
                    <span className={styles.featureStatus}>
                      {disabled ? t('featureComingSoon') : t('featureAvailable')}
                    </span>
                  </label>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </>
  );
}
