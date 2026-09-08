import { getTranslations } from 'next-intl/server';
import { Link, getPathname } from '@/i18n/navigation';
import type { OwnerFarm } from '@/lib/owner-farms';
import {
  ANIMAL_PAGE_SIZE,
  animalsHref,
  emptyAnimalLookups,
  emptyAnimalPage,
  getAnimalLookups,
  getFarmAnimal,
  listFarmAnimals,
  normalizeLookups,
  normalizePage,
  paginationItems,
  summaryRange,
  type AnimalLookups,
  type AnimalSearchState,
  type FarmAnimal,
} from '@/lib/animals';
import { archiveAnimalAction, saveAnimalAction } from '@/lib/animal-actions';
import styles from './OwnerAnimals.module.css';

type Props = {
  farm: OwnerFarm;
  token: string;
  locale: string;
  query: AnimalSearchState;
};

function localized(locale: string, ar?: string | null, en?: string | null) {
  return locale === 'ar' ? ar || en || '' : en || ar || '';
}

function formatCount(value: number, locale: string) {
  return new Intl.NumberFormat(locale === 'ar' ? 'ar-EG' : 'en', { useGrouping: false }).format(value);
}

function statusBadgeClass(token?: string) {
  if (token === 'purple') {
    return styles.pregnant;
  }
  if (token === 'danger') {
    return styles.sick;
  }
  if (token === 'warning') {
    return styles.isolated;
  }
  return styles.activeB;
}

function listQuery(query: AnimalSearchState) {
  return {
    search: query.search,
    breedId: query.breedId,
    statusCode: query.statusCode,
    groupId: query.groupId,
    gender: query.gender,
    page: query.page,
  };
}

export async function OwnerAnimals({ farm, token, locale, query }: Props) {
  const farmT = await getTranslations('farm');
  const t = await getTranslations('animals');

  if (!farm.animalManagementEnabled) {
    return (
      <section>
        <div className={styles.lockHeader}>
          <div className={styles.lockTitle}>
            <h1>{farmT('animalsTitle')}</h1>
            <p>{farmT('animalsHint', { farm: farm.name })}</p>
          </div>
        </div>
        <div className={styles.lockCard}>
          <div className={styles.lockRow}>
            <span className={styles.lockIcon}>
              <svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" aria-hidden>
                <rect x="7" y="11" width="10" height="9" rx="1.5" stroke="currentColor" strokeWidth="1.8" />
                <path d="M9 11V8a3 3 0 0 1 6 0v3" stroke="currentColor" strokeWidth="1.8" />
              </svg>
            </span>
            <div>
              <h2>{farmT('unsupportedTitle')}</h2>
              <p className={styles.lockBody}>{farmT('unsupportedBody')}</p>
            </div>
          </div>
        </div>
      </section>
    );
  }

  let lookups: AnimalLookups = emptyAnimalLookups();
  let data = emptyAnimalPage();
  let loadError = query.formError && !query.compose && !query.archiveId ? query.formError : '';
  try {
    const [nextLookups, nextPage] = await Promise.all([
      getAnimalLookups(token, locale, farm.id),
      listFarmAnimals(token, locale, farm.id, listQuery(query)),
    ]);
    lookups = normalizeLookups(nextLookups);
    data = normalizePage(nextPage);
  } catch {
    loadError = t('loadError');
  }

  let editing: FarmAnimal | null = null;
  if (query.editId) {
    editing = data.items.find((animal) => animal.id === query.editId) ?? null;
    if (!editing) {
      try {
        editing = await getFarmAnimal(token, locale, farm.id, query.editId);
      } catch {
        editing = null;
      }
    }
  }

  const filters = listQuery(query);
  const rows = data.items ?? [];
  const pages = paginationItems(data.page, data.totalPages);
  const range = summaryRange(data.page, data.size || ANIMAL_PAGE_SIZE, data.total);
  const showTable = !loadError && rows.length > 0;
  const showEmpty = !loadError && rows.length === 0;
  const formAction = getPathname({ href: `/farm/${farm.id}/animals`, locale });
  const closingHref = animalsHref(farm.id, filters);
  const composeOpen = query.compose === 'new' || Boolean(editing);
  const archiveTarget = query.archiveId
    ? rows.find((animal) => animal.id === query.archiveId) ?? { id: query.archiveId, identificationNumber: query.archiveId }
    : null;

  return (
    <section data-farm-id={farm.id}>
      <header className={styles.topbar}>
        <div className={styles.heading}>
          <h1>{t('title')}</h1>
          <p>{t('subtitle')}</p>
        </div>
        <div className={styles.actions}>
          <button type="button" className={styles.iconBtn} disabled title={t('qrLater')} aria-label={t('qrScan')}>
            <svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" aria-hidden>
              <path d="M5 5h5v5H5zM14 5h5v5h-5zM5 14h5v5H5z" stroke="currentColor" strokeWidth="1.8" />
              <path d="M14 14h2v2h-2zM18 14h2v2h-2zM14 18h2v2h-2zM18 18h2v2h-2z" fill="currentColor" />
            </svg>
          </button>
          <Link href={animalsHref(farm.id, { ...filters, compose: 'new' })} className={styles.addBtn}>
            <svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" aria-hidden>
              <path d="M12 5v14M5 12h14" stroke="currentColor" strokeWidth="2" strokeLinecap="round" />
            </svg>
            {t('add')}
          </Link>
        </div>
      </header>

      <form className={styles.filters} action={formAction} method="get">
        <div className={styles.search}>
          <svg viewBox="0 0 24 24" width="1em" height="1em" fill="none" aria-hidden>
            <circle cx="11" cy="11" r="6" stroke="currentColor" strokeWidth="1.8" />
            <path d="m16 16 4 4" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
          </svg>
          <input name="q" defaultValue={query.search ?? ''} placeholder={t('searchPlaceholder')} aria-label={t('searchPlaceholder')} />
        </div>
        <div className={styles.selectWrap}>
          <select name="breedId" defaultValue={query.breedId ?? ''} aria-label={t('filterBreed')}>
            <option value="">{t('filterBreed')}</option>
            {lookups.breeds.map((breed) => (
              <option key={breed.id} value={breed.id}>
                {localized(locale, breed.nameAr, breed.nameEn)}
              </option>
            ))}
          </select>
        </div>
        <div className={styles.selectWrap}>
          <select name="statusCode" defaultValue={query.statusCode ?? ''} aria-label={t('filterStatus')}>
            <option value="">{t('filterStatus')}</option>
            {lookups.statuses.map((status) => (
              <option key={status.code} value={status.code}>
                {localized(locale, status.nameAr, status.nameEn)}
              </option>
            ))}
          </select>
        </div>
        <div className={styles.selectWrap}>
          <select name="groupId" defaultValue={query.groupId ?? ''} aria-label={t('filterGroup')}>
            <option value="">{t('filterGroup')}</option>
            {lookups.groups.map((group) => (
              <option key={group.id} value={group.id}>
                {localized(locale, group.nameAr, group.nameEn)}
              </option>
            ))}
          </select>
        </div>
        <div className={styles.selectWrap}>
          <select name="gender" defaultValue={query.gender ?? ''} aria-label={t('filterGender')}>
            <option value="">{t('filterGender')}</option>
            {lookups.genders.map((item) => (
              <option key={item.code} value={item.code}>
                {localized(locale, item.nameAr, item.nameEn)}
              </option>
            ))}
          </select>
        </div>
        <button type="submit" className={styles.filterApply}>
          {t('apply')}
        </button>
      </form>

      <section className={styles.tableCard}>
        {loadError ? (
          <div className={styles.error}>
            <p>{loadError}</p>
            <Link href={animalsHref(farm.id, filters)} className={styles.retry}>
              {t('retry')}
            </Link>
          </div>
        ) : null}
        {showEmpty ? <div className={styles.empty}>{t('empty')}</div> : null}
        {showTable ? (
          <table className={styles.table}>
            <thead>
              <tr>
                <th>{t('colId')}</th>
                <th>{t('colName')}</th>
                <th>{t('colBreed')}</th>
                <th>{t('colGender')}</th>
                <th>{t('colAge')}</th>
                <th>{t('colGroup')}</th>
                <th>{t('colStatus')}</th>
                <th>{t('colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((animal) => {
                const genderOption = lookups.genders.find((item) => item.code === animal.genderCode);
                return (
                  <tr key={animal.id} data-animal-id={animal.id}>
                    <td>{animal.identificationNumber}</td>
                    <td className={styles.name}>{animal.name || '—'}</td>
                    <td>{localized(locale, animal.breed?.nameAr, animal.breed?.nameEn) || '—'}</td>
                    <td>{localized(locale, genderOption?.nameAr, genderOption?.nameEn) || animal.genderCode}</td>
                    <td>{animal.ageDisplay || '—'}</td>
                    <td>{localized(locale, animal.group?.nameAr, animal.group?.nameEn) || '—'}</td>
                    <td>
                      <span className={`${styles.badge} ${statusBadgeClass(animal.status?.colorToken)}`}>
                        {localized(locale, animal.status?.nameAr, animal.status?.nameEn) || animal.status?.code}
                      </span>
                    </td>
                    <td>
                      <div className={styles.rowActions}>
                        <Link href={animalsHref(farm.id, { ...filters, edit: animal.id })} className={styles.edit} title={t('edit')}>
                          {t('edit')}
                        </Link>
                        <span className={styles.divider} />
                        <Link
                          href={animalsHref(farm.id, { ...filters, archive: animal.id })}
                          className={styles.delete}
                          title={t('delete')}
                        >
                          {t('delete')}
                        </Link>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        ) : null}
        {showTable ? (
          <footer className={styles.footer}>
            <span>
              {t('summary', {
                start: formatCount(range.start, locale),
                end: formatCount(range.end, locale),
                total: formatCount(data.total, locale),
              })}
            </span>
            <div className={styles.pagination}>
              {pages.map((item, index) =>
                item === 'ellipsis' ? (
                  <span key={`e-${index}`} className={styles.ellipsis}>
                    ...
                  </span>
                ) : (
                  <Link
                    key={item}
                    href={animalsHref(farm.id, { ...filters, page: item })}
                    className={styles.pageBtn}
                    data-active={item === data.page ? 'true' : 'false'}
                  >
                    {formatCount(item + 1, locale)}
                  </Link>
                ),
              )}
            </div>
          </footer>
        ) : null}
      </section>

      {composeOpen ? (
        <div className={styles.modalBg}>
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="animal-form-title">
            <div className={styles.modalHead}>
              <h2 id="animal-form-title">{editing ? t('editTitle') : t('addTitle')}</h2>
              <Link href={closingHref} className={styles.close} aria-label={t('cancel')}>
                ×
              </Link>
            </div>
            <form action={saveAnimalAction.bind(null, farm.id, locale, editing?.id ?? '')}>
              <div className={styles.form}>
                <div className={styles.field}>
                  <label htmlFor="animal-id">{t('fieldId')}</label>
                  <input
                    id="animal-id"
                    name="identificationNumber"
                    defaultValue={editing?.identificationNumber ?? ''}
                    required
                    maxLength={40}
                  />
                </div>
                <div className={styles.field}>
                  <label htmlFor="animal-name">{t('fieldName')}</label>
                  <input id="animal-name" name="name" defaultValue={editing?.name ?? ''} maxLength={100} />
                </div>
                <div className={styles.field}>
                  <label htmlFor="animal-breed">{t('fieldBreed')}</label>
                  <select id="animal-breed" name="breedId" defaultValue={editing?.breed?.id ?? lookups.breeds[0]?.id ?? ''} required>
                    <option value="">{t('choose')}</option>
                    {lookups.breeds.map((breed) => (
                      <option key={breed.id} value={breed.id}>
                        {localized(locale, breed.nameAr, breed.nameEn)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.field}>
                  <label htmlFor="animal-status">{t('fieldStatus')}</label>
                  <select
                    id="animal-status"
                    name="statusCode"
                    defaultValue={editing?.status?.code ?? lookups.statuses.find((status) => status.code === 'ACTIVE')?.code ?? ''}
                    required
                  >
                    <option value="">{t('choose')}</option>
                    {lookups.statuses.map((status) => (
                      <option key={status.code} value={status.code}>
                        {localized(locale, status.nameAr, status.nameEn)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.field}>
                  <label htmlFor="animal-gender">{t('fieldGender')}</label>
                  <select id="animal-gender" name="genderCode" defaultValue={editing?.genderCode ?? lookups.genders[0]?.code ?? 'FEMALE'} required>
                    {lookups.genders.map((item) => (
                      <option key={item.code} value={item.code}>
                        {localized(locale, item.nameAr, item.nameEn)}
                      </option>
                    ))}
                  </select>
                </div>
                <div className={styles.field}>
                  <label htmlFor="animal-dob">{t('fieldDob')}</label>
                  <input id="animal-dob" name="dateOfBirth" type="date" defaultValue={editing?.dateOfBirth ?? ''} />
                </div>
                <div className={`${styles.field} ${styles.fieldFull}`}>
                  <label htmlFor="animal-group">{t('fieldGroup')}</label>
                  <select id="animal-group" name="groupId" defaultValue={editing?.group?.id ?? ''}>
                    <option value="">{t('groupNone')}</option>
                    {lookups.groups.map((group) => (
                      <option key={group.id} value={group.id}>
                        {localized(locale, group.nameAr, group.nameEn)}
                      </option>
                    ))}
                  </select>
                </div>
                {query.formError ? <p className={styles.formError}>{query.formError}</p> : null}
              </div>
              <div className={styles.modalFoot}>
                <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`}>
                  {t('save')}
                </button>
                <Link href={closingHref} className={styles.btn}>
                  {t('cancel')}
                </Link>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {archiveTarget ? (
        <div className={styles.modalBg}>
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="animal-delete-title">
            <div className={styles.modalHead}>
              <h2 id="animal-delete-title">{t('deleteTitle')}</h2>
              <Link href={closingHref} className={styles.close} aria-label={t('cancel')}>
                ×
              </Link>
            </div>
            <p className={styles.confirmText}>{t('confirmArchive', { id: archiveTarget.identificationNumber })}</p>
            <div className={styles.modalFoot}>
              <form action={archiveAnimalAction.bind(null, farm.id, locale, archiveTarget.id)}>
                <button type="submit" className={`${styles.btn} ${styles.btnDanger}`}>
                  {t('delete')}
                </button>
              </form>
              <Link href={closingHref} className={styles.btn}>
                {t('cancel')}
              </Link>
            </div>
          </div>
        </div>
      ) : null}
    </section>
  );
}
