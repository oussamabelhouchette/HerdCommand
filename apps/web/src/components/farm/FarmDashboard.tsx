import { getLocale, getTranslations } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { AnimalIcon, FarmIcon, FolderIcon, LayersIcon, LockIcon } from '@/components/ui/Icons';
import { PageHeader } from '@/components/ui/PageHeader';
import { StatCard, StatGrid } from '@/components/ui/StatGrid';
import { Surface } from '@/components/ui/Surface';
import { getSession } from '@/lib/session';
import { listFarmAnimals, normalizePage } from '@/lib/animals';
import { listGroups, normalizeGroupPage } from '@/lib/groups';
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

function formatCount(value: number, locale: string) {
  return new Intl.NumberFormat(locale === 'ar' ? 'ar-EG' : 'en', { useGrouping: false }).format(value);
}

export async function FarmDashboard({ farm }: Props) {
  const locale = await getLocale();
  const t = await getTranslations();
  const session = await getSession();
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

  let animalTotal: number | null = null;
  let groupTotal: number | null = null;
  if (farm.animalManagementEnabled && session?.accessToken) {
    try {
      const [animals, groups] = await Promise.all([
        listFarmAnimals(session.accessToken, locale, farm.id, { page: 0, size: 1 }),
        listGroups(session.accessToken, locale, farm.id, { page: 0, size: 1 }),
      ]);
      animalTotal = normalizePage(animals).total;
      groupTotal = normalizeGroupPage(groups).total;
    } catch {
      animalTotal = null;
      groupTotal = null;
    }
  }

  const animalsHref = getPathname({ href: `/farm/${farm.id}/animals`, locale });
  const groupsHrefPath = getPathname({ href: `/farm/${farm.id}/groups`, locale });
  const dash = farm.animalManagementEnabled;

  return (
    <section>
      <PageHeader title={farm.name} subtitle={t('farm.dashboardHint')} />
      <StatGrid columns={4}>
        <StatCard icon={<FarmIcon />} value={farm.code} label={t('farm.statCode')} tone={1} />
        <StatCard icon={<LayersIcon />} value={planLabel} label={t('farm.statPlan')} tone={2} />
        <StatCard
          icon={dash ? <AnimalIcon /> : <LockIcon />}
          value={dash ? (animalTotal == null ? '—' : formatCount(animalTotal, locale)) : t('farm.notSupported')}
          label={t('farm.statAnimalCount')}
          tone={dash ? 2 : 3}
          href={dash ? animalsHref : undefined}
        />
        <StatCard
          icon={dash ? <FolderIcon /> : <LockIcon />}
          value={dash ? (groupTotal == null ? '—' : formatCount(groupTotal, locale)) : t('farm.notSupported')}
          label={t('farm.statGroupCount')}
          tone={dash ? 4 : 3}
          href={dash ? groupsHrefPath : undefined}
        />
      </StatGrid>
      <Surface>
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
      </Surface>
    </section>
  );
}
