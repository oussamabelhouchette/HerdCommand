import { getTranslations } from 'next-intl/server';
import { CheckIcon, LockIcon } from '@/components/admin/AdminIcons';
import type { OwnerFarm } from '@/lib/owner-farms';
import styles from './FarmWorkspace.module.css';

type Props = {
  farm: OwnerFarm;
};

export async function FarmAnimals({ farm }: Props) {
  const t = await getTranslations('farm');

  return (
    <section>
      <div className={styles.header}>
        <div className={styles.title}>
          <h1>{t('animalsTitle')}</h1>
          <p>{t('animalsHint', { farm: farm.name })}</p>
        </div>
      </div>
      <div className={styles.card}>
        <div className={styles.supported}>
          <span className={`${styles.supportedIcon} ${farm.animalManagementEnabled ? '' : styles.unsupportedIcon}`}>
            {farm.animalManagementEnabled ? <CheckIcon /> : <LockIcon />}
          </span>
          <div>
            <h2>{farm.animalManagementEnabled ? t('supportedTitle') : t('unsupportedTitle')}</h2>
            <p className={styles.empty}>
              {farm.animalManagementEnabled ? t('supportedBody') : t('unsupportedBody')}
            </p>
          </div>
        </div>
      </div>
    </section>
  );
}
