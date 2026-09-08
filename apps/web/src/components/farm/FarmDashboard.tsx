import { getTranslations } from 'next-intl/server';
import { CheckIcon, FarmIcon, LayersIcon, LockIcon } from '@/components/admin/AdminIcons';
import type { OwnerFarm } from '@/lib/owner-farms';
import styles from './FarmWorkspace.module.css';

type Props = {
  farm: OwnerFarm;
};

function statusClass(status: string) {
  if (status === 'ACTIVE') {
    return styles.statusActive;
  }
  if (status === 'SETUP') {
    return styles.statusSetup;
  }
  if (status === 'SUSPENDED') {
    return styles.statusSuspended;
  }
  return styles.statusArchived;
}

export async function FarmDashboard({ farm }: Props) {
  const t = await getTranslations();
  const statusLabel = ['SETUP', 'ACTIVE', 'SUSPENDED', 'ARCHIVED'].includes(farm.status)
    ? t(`farmSettings.status.${farm.status}`)
    : farm.status;
  const planLabel = farm.planCode && ['TRIAL', 'ESSENTIAL', 'PROFESSIONAL'].includes(farm.planCode)
    ? t(`farmSettings.plan.${farm.planCode}`)
    : farm.planCode || t('farm.noPlan');
  const governorateKey = farm.governorateCode ? `farmSettings.governorate.${farm.governorateCode}` : '';
  const governorate = farm.governorateCode
    ? t.has(governorateKey)
      ? t(governorateKey)
      : farm.governorateCode
    : '—';

  return (
    <section>
      <div className={styles.header}>
        <div className={styles.title}>
          <h1>{farm.name}</h1>
          <p>{t('farm.dashboardHint')}</p>
        </div>
      </div>
      <div className={styles.stats}>
        <div className={styles.stat}>
          <span className={styles.statIcon}>
            <FarmIcon />
          </span>
          <div>
            <div className={styles.statLabel}>{t('farm.statCode')}</div>
            <div className={styles.statValue}>{farm.code}</div>
          </div>
        </div>
        <div className={styles.stat}>
          <span className={styles.statIcon}>
            <LayersIcon />
          </span>
          <div>
            <div className={styles.statLabel}>{t('farm.statPlan')}</div>
            <div className={styles.statValue}>{planLabel}</div>
          </div>
        </div>
        <div className={styles.stat}>
          <span className={styles.statIcon}>
            {farm.animalManagementEnabled ? <CheckIcon /> : <LockIcon />}
          </span>
          <div>
            <div className={styles.statLabel}>{t('farm.statAnimals')}</div>
            <div className={styles.statValue}>
              {farm.animalManagementEnabled ? t('farm.supported') : t('farm.notSupported')}
            </div>
          </div>
        </div>
      </div>
      <div className={styles.card}>
        <dl className={styles.details}>
          <div>
            <dt>{t('farmSettings.detailsNameAr')}</dt>
            <dd>{farm.nameAr}</dd>
          </div>
          <div>
            <dt>{t('farmSettings.detailsNameEn')}</dt>
            <dd>{farm.nameEn}</dd>
          </div>
          <div>
            <dt>{t('farm.colStatus')}</dt>
            <dd>
              <span className={`${styles.pill} ${statusClass(farm.status)}`}>{statusLabel}</span>
            </dd>
          </div>
          <div>
            <dt>{t('farmSettings.detailsGovernorate')}</dt>
            <dd>{governorate}</dd>
          </div>
          <div>
            <dt>{t('farmSettings.detailsAddress')}</dt>
            <dd>{farm.address || '—'}</dd>
          </div>
          <div>
            <dt>{t('farmSettings.detailsTimezone')}</dt>
            <dd>{farm.timezone || '—'}</dd>
          </div>
          <div>
            <dt>{t('farmSettings.colFeatures')}</dt>
            <dd className={styles.features}>
              {farm.enabledFeatureCodes.length === 0 ? (
                t('farm.noFeatures')
              ) : (
                farm.enabledFeatureCodes.map((code) => (
                  <span key={code} className={styles.chip} data-feature-code={code}>
                    {code === 'ANIMAL_MANAGEMENT' ? t('farm.animals') : code}
                  </span>
                ))
              )}
            </dd>
          </div>
        </dl>
      </div>
    </section>
  );
}
