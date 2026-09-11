'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSession } from 'next-auth/react';
import { useRouter } from '@/i18n/navigation';
import { ApiRequestError } from '@/lib/api';
import type { CatalogFeature } from '@/lib/features';
import { selectedFeatureCodes } from '@/lib/features';
import {
  FARM_PLANS,
  FARM_SORTS,
  FARM_STATUSES,
  SEARCH_DEBOUNCE_MS,
  farmListHref,
  getPlatformFarm,
  getPlatformFarmSummary,
  listPlatformFarms,
  type FarmListQuery,
  type FarmPage,
  type FarmPlan,
  type FarmSort,
  type FarmStatus,
  type FarmSummary,
  type PlatformFarmCreated,
  type PlatformFarmDetails,
  type PlatformFarmListItem,
} from '@/lib/farms';
import { FarmCreateWizard } from './FarmCreateWizard';
import {
  BanIcon,
  CheckIcon,
  ClockIcon,
  CloseIcon,
  EyeIcon,
  FarmIcon,
  InfoIcon,
  LayersIcon,
  PenIcon,
  PlusIcon,
  SearchIcon,
} from '@/components/ui/Icons';
import { Dialog } from './AdminDialog';
import { OwnerLookup } from './OwnerLookup';
import styles from './FarmSettings.module.css';

type Props = {
  apiReady: boolean;
  loadError?: string;
  features: CatalogFeature[];
  initialPage: FarmPage;
  initialSummary: FarmSummary;
  initialQuery: FarmListQuery;
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

export function FarmSettings({
  apiReady,
  loadError,
  features,
  initialPage,
  initialSummary,
  initialQuery,
}: Props) {
  const t = useTranslations('farmSettings');
  const locale = useLocale();
  const router = useRouter();
  const { data: session } = useSession();
  const token = session?.accessToken;
  const selectedCodes = selectedFeatureCodes(features);
  const featureNames = useMemo(
    () => new Map(features.map((feature) => [feature.code, feature.name])),
    [features],
  );

  const [search, setSearch] = useState(initialQuery.search ?? '');
  const [debouncedSearch, setDebouncedSearch] = useState(initialQuery.search ?? '');
  const [status, setStatus] = useState<FarmStatus | ''>(initialQuery.status ?? '');
  const [planCode, setPlanCode] = useState<FarmPlan | ''>(initialQuery.planCode ?? '');
  const [featureCode, setFeatureCode] = useState(initialQuery.featureCode ?? '');
  const [sort, setSort] = useState<FarmSort | string>(initialQuery.sort ?? 'createdAt,desc');
  const [page, setPage] = useState(initialQuery.page ?? 0);
  const [data, setData] = useState(initialPage);
  const [summary, setSummary] = useState(initialSummary);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState(loadError ?? '');
  const [detailsId, setDetailsId] = useState<string | null>(null);
  const [details, setDetails] = useState<PlatformFarmDetails | null>(null);
  const [detailsLoading, setDetailsLoading] = useState(false);
  const [detailsError, setDetailsError] = useState('');
  const [wizardOpen, setWizardOpen] = useState(false);
  const [createdId, setCreatedId] = useState<string | null>(null);
  const [toast, setToast] = useState('');
  const skipFirstFetch = useRef(true);

  const filters = useMemo<FarmListQuery>(
    () => ({
      search: debouncedSearch,
      status,
      planCode,
      featureCode,
      page,
      sort,
    }),
    [debouncedSearch, status, planCode, featureCode, page, sort],
  );

  useEffect(() => {
    const id = window.setTimeout(() => {
      const next = search.trim();
      if (next !== debouncedSearch) {
        setDebouncedSearch(next);
        setPage(0);
      }
    }, SEARCH_DEBOUNCE_MS);
    return () => window.clearTimeout(id);
  }, [search, debouncedSearch]);

  useEffect(() => {
    if (skipFirstFetch.current) {
      skipFirstFetch.current = false;
      return;
    }
    router.replace(farmListHref(filters), { scroll: false });
    void reload(filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.search, filters.status, filters.planCode, filters.featureCode, filters.page, filters.sort, token, locale]);

  useEffect(() => {
    if (!detailsId || !token) {
      return;
    }
    void loadDetails(detailsId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [detailsId, token, locale]);

  useEffect(() => {
    if (!toast) {
      return;
    }
    const id = window.setTimeout(() => setToast(''), 2800);
    return () => window.clearTimeout(id);
  }, [toast]);

  async function reload(query = filters) {
    if (!token) {
      return;
    }
    setLoading(true);
    setListError('');
    try {
      const [result, counts] = await Promise.all([
        listPlatformFarms(token, locale, query),
        getPlatformFarmSummary(token, locale),
      ]);
      setData(result);
      setSummary(counts);
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('loadError'));
    } finally {
      setLoading(false);
    }
  }

  async function loadDetails(farmId: string) {
    if (!token) {
      return;
    }
    setDetailsLoading(true);
    setDetailsError('');
    try {
      setDetails(await getPlatformFarm(token, locale, farmId));
    } catch (error) {
      handleAuthError(error);
      setDetails(null);
      setDetailsError(error instanceof Error ? error.message : t('detailsError'));
    } finally {
      setDetailsLoading(false);
    }
  }

  function handleAuthError(error: unknown) {
    if (error instanceof ApiRequestError && error.status === 401) {
      window.location.href = '/api/auth/federated-logout';
    }
  }

  function closeDetails() {
    setDetailsId(null);
    setDetails(null);
    setDetailsError('');
  }

  function labelFor(prefix: 'status' | 'plan', code: string | null | undefined) {
    if (!code) {
      return '—';
    }
    const key = `${prefix}.${code}`;
    return t.has(key) ? t(key) : code;
  }

  function featureLabel(code: string) {
    return featureNames.get(code) ?? code;
  }

  function formatDate(value: string) {
    return new Intl.DateTimeFormat(locale === 'ar' ? 'ar-TN' : 'en-GB', {
      dateStyle: 'medium',
    }).format(new Date(value));
  }

  function ownerLine(farm: Pick<PlatformFarmListItem, 'ownerDisplayName' | 'ownerEmail'>) {
    return farm.ownerDisplayName || farm.ownerEmail || t('noOwner');
  }

  const pageCount = Math.max(1, Math.ceil(data.total / data.size));
  const statsFailed = Boolean(listError) && summary.totalFarms === 0;
  const count = (value: number) => (statsFailed ? t('statPendingValue') : String(value));

  return (
    <>
      <div className={styles.header}>
        <div className={styles.title}>
          <h1>{t('title')}</h1>
          <p>{t('subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button type="button" className={`${styles.btn} ${styles.btnPrimary}`} onClick={() => setWizardOpen(true)}>
            <PlusIcon />
            {t('addFarm')}
          </button>
        </div>
      </div>

      <section className={styles.stats}>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <FarmIcon />
          </div>
          <div>
            <strong>{count(summary.totalFarms)}</strong>
            <span>{t('statFarms')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <CheckIcon />
          </div>
          <div>
            <strong>{count(summary.activeFarms)}</strong>
            <span>{t('statActive')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <LayersIcon />
          </div>
          <div>
            <strong>{count(summary.activeAnimals)}</strong>
            <span>{t('statAnimals')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <ClockIcon />
          </div>
          <div>
            <strong>{count(summary.setupFarms)}</strong>
            <span>{t('statPending')}</span>
          </div>
        </div>
      </section>

      <section className={styles.panel} aria-busy={loading}>
        {apiReady && !listError ? <p className={styles.status}>{t('apiReady')}</p> : null}
        <div className={styles.toolbar}>
          <div className={styles.search}>
            <input
              value={search}
              onChange={(event) => setSearch(event.target.value)}
              placeholder={t('searchPlaceholder')}
              aria-label={t('searchPlaceholder')}
            />
            <SearchIcon />
          </div>
          <select
            className={styles.filter}
            value={status}
            onChange={(event) => {
              setStatus(event.target.value as FarmStatus | '');
              setPage(0);
            }}
            aria-label={t('filterAllStatuses')}
          >
            <option value="">{t('filterAllStatuses')}</option>
            {FARM_STATUSES.map((code) => (
              <option key={code} value={code}>
                {t(`status.${code}`)}
              </option>
            ))}
          </select>
          <select
            className={styles.filter}
            value={planCode}
            onChange={(event) => {
              setPlanCode(event.target.value as FarmPlan | '');
              setPage(0);
            }}
            aria-label={t('filterAllPlans')}
          >
            <option value="">{t('filterAllPlans')}</option>
            {FARM_PLANS.map((code) => (
              <option key={code} value={code}>
                {t(`plan.${code}`)}
              </option>
            ))}
          </select>
          <select
            className={styles.filter}
            value={featureCode}
            onChange={(event) => {
              setFeatureCode(event.target.value);
              setPage(0);
            }}
            aria-label={t('filterAllFeatures')}
          >
            <option value="">{t('filterAllFeatures')}</option>
            {features.map((feature) => (
              <option key={feature.id} value={feature.code}>
                {feature.name}
              </option>
            ))}
          </select>
          <select
            className={styles.filter}
            value={sort}
            onChange={(event) => {
              setSort(event.target.value);
              setPage(0);
            }}
            aria-label={t('sortLabel')}
          >
            {FARM_SORTS.map((value) => (
              <option key={value} value={value}>
                {t(`sort.${value.replace(',', '_')}`)}
              </option>
            ))}
          </select>
        </div>

        {listError ? (
          <div className={styles.listError}>
            <p>{listError}</p>
            <button type="button" className={styles.btn} onClick={() => void reload()}>
              {t('retry')}
            </button>
          </div>
        ) : (
          <div className={styles.tableWrap}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>{t('colFarm')}</th>
                  <th>{t('colOwner')}</th>
                  <th>{t('colPlan')}</th>
                  <th>{t('colUsage')}</th>
                  <th>{t('colFeatures')}</th>
                  <th>{t('colCreated')}</th>
                  <th>{t('colStatus')}</th>
                  <th>{t('colActions')}</th>
                </tr>
              </thead>
              <tbody>
                {loading ? (
                  Array.from({ length: 5 }, (_, index) => (
                    <tr key={`skeleton-${index}`} className={styles.skeleton} data-loading="true">
                      {Array.from({ length: 8 }, (__, cell) => (
                        <td key={cell}>
                          <span className={styles.skeletonBar} />
                        </td>
                      ))}
                    </tr>
                  ))
                ) : data.items.length === 0 ? (
                  <tr>
                    <td colSpan={8} className={styles.empty}>
                      {t('empty')}
                    </td>
                  </tr>
                ) : (
                  data.items.map((farm) => (
                    <tr
                      key={farm.id}
                      className={farm.id === createdId ? styles.createdRow : undefined}
                      data-farm-id={farm.id}
                      data-farm-code={farm.code}
                      data-status={farm.status}
                      data-created={farm.id === createdId || undefined}
                    >
                      <td>
                        <strong>{farm.name}</strong>
                        <span className={styles.code}>{farm.code}</span>
                      </td>
                      <td>
                        {ownerLine(farm)}
                        {farm.ownerEmail && farm.ownerDisplayName ? (
                          <span className={`${styles.sub} ${styles.ltr}`}>{farm.ownerEmail}</span>
                        ) : null}
                      </td>
                      <td>{labelFor('plan', farm.planCode)}</td>
                      <td className={styles.ltr}>
                        {t('usageOf', { used: farm.activeAnimalCount, max: farm.maxActiveAnimals })}
                      </td>
                      <td>
                        <div className={styles.featuresCell}>
                          {farm.enabledFeatureCodes.map((code) => (
                            <span key={code} className={styles.chip} data-feature-code={code}>
                              {featureLabel(code)}
                            </span>
                          ))}
                        </div>
                      </td>
                      <td>{formatDate(farm.createdAt)}</td>
                      <td>
                        <span className={`${styles.pill} ${statusClass(farm.status)}`}>
                          {labelFor('status', farm.status)}
                        </span>
                      </td>
                      <td>
                        <div className={styles.actions}>
                          <button type="button" disabled title={t('editLater')} aria-label={t('edit')}>
                            <PenIcon />
                          </button>
                          <button
                            type="button"
                            title={t('openDetails')}
                            aria-label={t('openDetails')}
                            onClick={() => setDetailsId(farm.id)}
                          >
                            <EyeIcon />
                          </button>
                          <button
                            type="button"
                            disabled
                            title={t('suspendLater')}
                            aria-label={farm.status === 'SUSPENDED' ? t('reactivate') : t('suspend')}
                          >
                            {farm.status === 'SUSPENDED' ? <CheckIcon /> : <BanIcon />}
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        )}

        {!listError && data.total > data.size ? (
          <div className={styles.pager}>
            <button
              type="button"
              className={styles.btn}
              disabled={page <= 0 || loading}
              onClick={() => setPage((value) => Math.max(0, value - 1))}
            >
              {t('prev')}
            </button>
            <span>
              {page + 1} / {pageCount}
            </span>
            <button
              type="button"
              className={styles.btn}
              disabled={page + 1 >= pageCount || loading}
              onClick={() => setPage((value) => value + 1)}
            >
              {t('next')}
            </button>
          </div>
        ) : null}

        <div className={styles.note}>
          <InfoIcon />
          {t('note')}
        </div>
      </section>

      <section className={styles.ownerPanel}>
        <OwnerLookup />
        <h2 className={styles.featuresTitle}>{t('featuresTitle')}</h2>
        <p className={styles.featuresHint}>{t('featuresHint')}</p>
        {features.length === 0 && !listError ? (
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

      {detailsId ? (
        <Dialog
          titleId="farm-details-title"
          title={t('detailsTitle')}
          close={
            <button type="button" onClick={closeDetails} aria-label={t('close')}>
              <CloseIcon />
            </button>
          }
        >
          <div className={styles.detailsBody}>
              {detailsLoading ? <p>{t('loading')}</p> : null}
              {detailsError ? (
                <div className={styles.listError}>
                  <p>{detailsError}</p>
                  <button
                    type="button"
                    className={styles.btn}
                    onClick={() => detailsId && void loadDetails(detailsId)}
                  >
                    {t('retry')}
                  </button>
                </div>
              ) : null}
              {details ? (
                <dl className={styles.details}>
                  <div>
                    <dt>{t('colFarm')}</dt>
                    <dd>
                      {details.name} <span className={styles.code}>{details.code}</span>
                    </dd>
                  </div>
                  <div>
                    <dt>{t('detailsNameAr')}</dt>
                    <dd>{details.nameAr}</dd>
                  </div>
                  <div>
                    <dt>{t('detailsNameEn')}</dt>
                    <dd>{details.nameEn}</dd>
                  </div>
                  {details.nameFr ? (
                    <div>
                      <dt>{t('detailsNameFr')}</dt>
                      <dd>{details.nameFr}</dd>
                    </div>
                  ) : null}
                  <div>
                    <dt>{t('colOwner')}</dt>
                    <dd>
                      {ownerLine(details)}
                      {details.ownerEmail ? <span className={`${styles.sub} ${styles.ltr}`}>{details.ownerEmail}</span> : null}
                    </dd>
                  </div>
                  <div>
                    <dt>{t('colPlan')}</dt>
                    <dd>{labelFor('plan', details.planCode)}</dd>
                  </div>
                  <div>
                    <dt>{t('colStatus')}</dt>
                    <dd>
                      <span className={`${styles.pill} ${statusClass(details.status)}`}>
                        {labelFor('status', details.status)}
                      </span>
                    </dd>
                  </div>
                  <div>
                    <dt>{t('colUsage')}</dt>
                    <dd className={styles.ltr}>
                      {t('usageOf', { used: details.activeAnimalCount, max: details.maxActiveAnimals })}
                    </dd>
                  </div>
                  <div>
                    <dt>{t('detailsGovernorate')}</dt>
                    <dd>{details.governorateCode || '—'}</dd>
                  </div>
                  <div>
                    <dt>{t('detailsAddress')}</dt>
                    <dd>{details.address || '—'}</dd>
                  </div>
                  <div>
                    <dt>{t('detailsTimezone')}</dt>
                    <dd className={styles.ltr}>{details.timezone || '—'}</dd>
                  </div>
                  <div>
                    <dt>{t('detailsLanguage')}</dt>
                    <dd>{details.defaultLanguage || '—'}</dd>
                  </div>
                  <div>
                    <dt>{t('detailsCurrency')}</dt>
                    <dd>{details.currencyCode || '—'}</dd>
                  </div>
                  <div>
                    <dt>{t('colFeatures')}</dt>
                    <dd>
                      <div className={styles.featuresCell}>
                        {details.enabledFeatureCodes.map((code) => (
                          <span key={code} className={styles.chip} data-feature-code={code}>
                            {featureLabel(code)}
                          </span>
                        ))}
                      </div>
                    </dd>
                  </div>
                  <div>
                    <dt>{t('colCreated')}</dt>
                    <dd>{formatDate(details.createdAt)}</dd>
                  </div>
                  {details.createdBy ? (
                    <div>
                      <dt>{t('detailsCreatedBy')}</dt>
                      <dd className={styles.ltr}>{details.createdBy}</dd>
                    </div>
                  ) : null}
                  <div>
                    <dt>{t('detailsVersion')}</dt>
                    <dd>{details.version}</dd>
                  </div>
                </dl>
              ) : null}
            </div>
        </Dialog>
      ) : null}

      <FarmCreateWizard
        open={wizardOpen}
        features={features}
        onClose={() => setWizardOpen(false)}
        onCreated={(created: PlatformFarmCreated) => {
          setWizardOpen(false);
          setCreatedId(created.id);
          setDetailsId(created.id);
          setToast(t('wizard.created', { code: created.code }));
          void reload();
        }}
      />

      {toast ? (
        <div className={styles.toast} role="status">
          <CheckIcon />
          {toast}
        </div>
      ) : null}
    </>
  );
}
