'use client';

import { useCallback, useState } from 'react';
import { useTranslations } from 'next-intl';
import type { BreedPage } from '@/lib/breeds';
import type { StatusPage } from '@/lib/statuses';
import { BreedManagement } from './BreedManagement';
import { StatusManagement } from './StatusManagement';
import { ClockIcon, DnaIcon, LayersIcon, TagsIcon } from './AdminIcons';
import styles from './BreedManagement.module.css';

type Tab = 'breeds' | 'statuses';

type Props = {
  initialBreedPage: BreedPage;
  initialActiveBreedCount: number;
  initialBreedError?: string;
  initialStatusPage: StatusPage;
  initialStatusError?: string;
};

export function AnimalSettings({
  initialBreedPage,
  initialActiveBreedCount,
  initialBreedError,
  initialStatusPage,
  initialStatusError,
}: Props) {
  const t = useTranslations('animalSettings');
  const [tab, setTab] = useState<Tab>('breeds');
  const [activeBreedCount, setActiveBreedCount] = useState(initialActiveBreedCount);
  const [breedTotal, setBreedTotal] = useState(initialBreedPage.total);
  const [statusTotal, setStatusTotal] = useState(initialStatusPage.total);

  const onBreedStats = useCallback((stats: { activeCount: number; total: number }) => {
    setActiveBreedCount(stats.activeCount);
    setBreedTotal(stats.total);
  }, []);

  const onStatusStats = useCallback((stats: { total: number }) => {
    setStatusTotal(stats.total);
  }, []);

  return (
    <>
      <header className={styles.header}>
        <div className={styles.title}>
          <h1>{t('title')}</h1>
          <p>{t('subtitle')}</p>
        </div>
        <div className={styles.headerActions}>
          <button type="button" className={styles.btn} disabled>
            <ClockIcon />
            {t('changelog')}
          </button>
        </div>
      </header>

      <section className={styles.stats}>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <DnaIcon />
          </div>
          <div>
            <strong>{activeBreedCount}</strong>
            <span>{t('statActiveBreeds')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <TagsIcon />
          </div>
          <div>
            <strong>{statusTotal}</strong>
            <span>{t('statStatuses')}</span>
          </div>
        </div>
        <div className={styles.stat}>
          <div className={styles.statIcon}>
            <LayersIcon />
          </div>
          <div>
            <strong>0</strong>
            <span>{t('statGroups')}</span>
          </div>
        </div>
      </section>

      <section className={styles.panel}>
        <div className={styles.tabs}>
          <button
            type="button"
            className={`${styles.tab} ${tab === 'breeds' ? styles.tabActive : ''}`}
            onClick={() => setTab('breeds')}
          >
            {t('tabBreeds')}
            <span className={styles.count}>{breedTotal}</span>
          </button>
          <button
            type="button"
            className={`${styles.tab} ${tab === 'statuses' ? styles.tabActive : ''}`}
            onClick={() => setTab('statuses')}
          >
            {t('tabStatuses')}
            <span className={styles.count}>{statusTotal}</span>
          </button>
          <button type="button" className={styles.tab} disabled title={t('tabSoon')}>
            {t('tabGroups')}
            <span className={styles.count}>0</span>
          </button>
        </div>
        <div hidden={tab !== 'breeds'}>
          <BreedManagement
            hideChrome
            initialPage={initialBreedPage}
            initialActiveCount={initialActiveBreedCount}
            initialError={initialBreedError}
            onStatsChange={onBreedStats}
          />
        </div>
        <div hidden={tab !== 'statuses'}>
          <StatusManagement
            initialPage={initialStatusPage}
            initialError={initialStatusError}
            onStatsChange={onStatusStats}
          />
        </div>
      </section>
    </>
  );
}
