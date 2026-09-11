import { getLocale, getTranslations } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { getSession } from '@/lib/session';
import type { OwnerFarm } from '@/lib/owner-farms';
import {
  ANIMAL_PAGE_SIZE,
  animalListQuery,
  animalsHref,
  emptyAnimalLookups,
  emptyAnimalPage,
  getAnimalLookups,
  getFarmAnimal,
  listFarmAnimals,
  localizedName,
  normalizeLookups,
  normalizePage,
  paginationItems,
  summaryRange,
  type AnimalLookups,
  type AnimalSearchState,
  type FarmAnimal,
} from '@/lib/animals';
import { archiveAnimalAction, saveAnimalAction } from '@/lib/animal-actions';
import { Callout } from '@/components/ui/Callout';
import { CloseIcon, LockIcon, PenIcon, PlusIcon, QrIcon, SearchIcon, TrashIcon } from '@/components/ui/Icons';
import { PageHeader } from '@/components/ui/PageHeader';
import { FilterBar, FilterSearch, FilterSelect } from '@/components/ui/FilterBar';
import { DataTable, DataTableFoot } from '@/components/ui/DataTable';
import { ConfirmText, Dialog, DialogFoot, FormError, FormField, FormFields } from '@/components/ui/Dialog';
import { ActionButton, ActionLink } from '@/components/ui/ActionButton';
import styles from './OwnerAnimals.module.css';

type Props = {
  farm: OwnerFarm;
  query: AnimalSearchState;
};

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

function listHref(locale: string, farmId: string, query: Parameters<typeof animalsHref>[1] = {}) {
  const path = getPathname({ href: `/farm/${farmId}/animals`, locale });
  const href = animalsHref(farmId, query);
  const search = href.includes('?') ? href.slice(href.indexOf('?')) : '';
  return `${path}${search}`;
}

export async function OwnerAnimals({ farm, query }: Props) {
  const locale = await getLocale();
  const farmT = await getTranslations('farm');
  const t = await getTranslations('animals');

  if (!farm.animalManagementEnabled) {
    return (
      <section>
        <PageHeader title={farmT('animalsTitle')} subtitle={farmT('animalsHint', { farm: farm.name })} />
        <Callout icon={<LockIcon />} title={farmT('unsupportedTitle')}>
          {farmT('unsupportedBody')}
        </Callout>
      </section>
    );
  }

  const session = await getSession();
  const token = session?.accessToken;
  if (!token) {
    return null;
  }

  const filters = animalListQuery(query);
  let lookups: AnimalLookups = emptyAnimalLookups();
  let data = emptyAnimalPage();
  let loadError = query.formError && !query.compose && !query.archiveId ? query.formError : '';
  try {
    const [nextLookups, nextPage] = await Promise.all([
      getAnimalLookups(token, locale, farm.id),
      listFarmAnimals(token, locale, farm.id, filters),
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

  const rows = data.items ?? [];
  const pages = paginationItems(data.page, data.totalPages);
  const range = summaryRange(data.page, data.size || ANIMAL_PAGE_SIZE, data.total);
  const showTable = !loadError && rows.length > 0;
  const showEmpty = !loadError && rows.length === 0;
  const formAction = getPathname({ href: `/farm/${farm.id}/animals`, locale });
  const closingHref = listHref(locale, farm.id, filters);
  const composeOpen = query.compose === 'new' || Boolean(editing);
  const archiveTarget = query.archiveId
    ? rows.find((animal) => animal.id === query.archiveId) ?? { id: query.archiveId, identificationNumber: query.archiveId }
    : null;

  return (
    <section data-farm-id={farm.id}>
      <PageHeader
        title={t('title')}
        subtitle={t('subtitle')}
        actions={
          <>
            <ActionButton variant="icon" disabled title={t('qrLater')} aria-label={t('qrScan')}>
              <QrIcon />
            </ActionButton>
            <ActionLink variant="primary" href={listHref(locale, farm.id, { ...filters, compose: 'new' })}>
              <PlusIcon />
              {t('add')}
            </ActionLink>
          </>
        }
      />

      <FilterBar as="form" action={formAction} method="get">
        <FilterSearch>
          <SearchIcon />
          <input name="q" defaultValue={query.search ?? ''} placeholder={t('searchPlaceholder')} aria-label={t('searchPlaceholder')} />
        </FilterSearch>
        <FilterSelect name="breedId" defaultValue={query.breedId ?? ''} aria-label={t('filterBreed')}>
          <option value="">{t('filterBreed')}</option>
          {lookups.breeds.map((breed) => (
            <option key={breed.id} value={breed.id}>
              {localizedName(locale, breed.nameAr, breed.nameEn)}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect name="statusCode" defaultValue={query.statusCode ?? ''} aria-label={t('filterStatus')}>
          <option value="">{t('filterStatus')}</option>
          {lookups.statuses.map((status) => (
            <option key={status.code} value={status.code}>
              {localizedName(locale, status.nameAr, status.nameEn)}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect name="groupId" defaultValue={query.groupId ?? ''} aria-label={t('filterGroup')}>
          <option value="">{t('filterGroup')}</option>
          {lookups.groups.map((group) => (
            <option key={group.id} value={group.id}>
              {localizedName(locale, group.nameAr, group.nameEn)}
            </option>
          ))}
        </FilterSelect>
        <FilterSelect name="gender" defaultValue={query.gender ?? ''} aria-label={t('filterGender')}>
          <option value="">{t('filterGender')}</option>
          {lookups.genders.map((item) => (
            <option key={item.code} value={item.code}>
              {localizedName(locale, item.nameAr, item.nameEn)}
            </option>
          ))}
        </FilterSelect>
        <ActionButton type="submit" variant="primary">
          {t('apply')}
        </ActionButton>
      </FilterBar>

      <DataTable
        error={
          loadError ? (
            <>
              <p>{loadError}</p>
              <ActionLink variant="primary" href={listHref(locale, farm.id, filters)}>
                {t('retry')}
              </ActionLink>
            </>
          ) : null
        }
        empty={showEmpty ? t('empty') : null}
        footer={
          showTable ? (
            <DataTableFoot
              summary={t('summary', {
                start: formatCount(range.start, locale),
                end: formatCount(range.end, locale),
                total: formatCount(data.total, locale),
              })}
            >
              {pages.map((item, index) =>
                item === 'ellipsis' ? (
                  <span key={`e-${index}`} className={styles.ellipsis}>
                    ...
                  </span>
                ) : (
                  <a
                    key={item}
                    href={listHref(locale, farm.id, { ...filters, page: item })}
                    className={styles.pageBtn}
                    data-active={item === data.page ? 'true' : 'false'}
                  >
                    {formatCount(item + 1, locale)}
                  </a>
                ),
              )}
            </DataTableFoot>
          ) : null
        }
      >
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
                    <td>{localizedName(locale, animal.breed?.nameAr, animal.breed?.nameEn) || '—'}</td>
                    <td>{localizedName(locale, genderOption?.nameAr, genderOption?.nameEn) || animal.genderCode}</td>
                    <td>{animal.ageDisplay || '—'}</td>
                    <td>{localizedName(locale, animal.group?.nameAr, animal.group?.nameEn) || '—'}</td>
                    <td>
                      <span className={`${styles.badge} ${statusBadgeClass(animal.status?.colorToken)}`}>
                        {localizedName(locale, animal.status?.nameAr, animal.status?.nameEn) || animal.status?.code}
                      </span>
                    </td>
                    <td>
                      <div className={styles.rowActions}>
                        <a href={listHref(locale, farm.id, { ...filters, edit: animal.id })} className={styles.edit} title={t('edit')}>
                          <PenIcon />
                        </a>
                        <span className={styles.divider} />
                        <a href={listHref(locale, farm.id, { ...filters, archive: animal.id })} className={styles.delete} title={t('delete')}>
                          <TrashIcon />
                        </a>
                      </div>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        ) : null}
      </DataTable>

      {composeOpen ? (
        <Dialog
          titleId="animal-form-title"
          title={editing ? t('editTitle') : t('addTitle')}
          close={
            <a href={closingHref} aria-label={t('cancel')}>
              <CloseIcon />
            </a>
          }
        >
          <form action={saveAnimalAction.bind(null, farm.id, locale, editing?.id ?? '')}>
            <FormFields>
              <FormField>
                <label htmlFor="animal-id">{t('fieldId')}</label>
                <input
                  id="animal-id"
                  name="identificationNumber"
                  defaultValue={editing?.identificationNumber ?? ''}
                  required
                  maxLength={40}
                />
              </FormField>
              <FormField>
                <label htmlFor="animal-name">{t('fieldName')}</label>
                <input id="animal-name" name="name" defaultValue={editing?.name ?? ''} maxLength={100} />
              </FormField>
              <FormField>
                <label htmlFor="animal-breed">{t('fieldBreed')}</label>
                <select id="animal-breed" name="breedId" defaultValue={editing?.breed?.id ?? lookups.breeds[0]?.id ?? ''} required>
                  <option value="">{t('choose')}</option>
                  {lookups.breeds.map((breed) => (
                    <option key={breed.id} value={breed.id}>
                      {localizedName(locale, breed.nameAr, breed.nameEn)}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField>
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
                      {localizedName(locale, status.nameAr, status.nameEn)}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField>
                <label htmlFor="animal-gender">{t('fieldGender')}</label>
                <select id="animal-gender" name="genderCode" defaultValue={editing?.genderCode ?? lookups.genders[0]?.code ?? 'FEMALE'} required>
                  {lookups.genders.map((item) => (
                    <option key={item.code} value={item.code}>
                      {localizedName(locale, item.nameAr, item.nameEn)}
                    </option>
                  ))}
                </select>
              </FormField>
              <FormField>
                <label htmlFor="animal-dob">{t('fieldDob')}</label>
                <input id="animal-dob" name="dateOfBirth" type="date" defaultValue={editing?.dateOfBirth ?? ''} />
              </FormField>
              <FormField full>
                <label htmlFor="animal-group">{t('fieldGroup')}</label>
                <select id="animal-group" name="groupId" defaultValue={editing?.group?.id ?? ''}>
                  <option value="">{t('groupNone')}</option>
                  {lookups.groups.map((group) => (
                    <option key={group.id} value={group.id}>
                      {localizedName(locale, group.nameAr, group.nameEn)}
                    </option>
                  ))}
                </select>
              </FormField>
              {query.formError ? <FormError>{query.formError}</FormError> : null}
            </FormFields>
            <DialogFoot>
              <ActionButton type="submit" variant="primary">
                {t('save')}
              </ActionButton>
              <ActionLink href={closingHref}>{t('cancel')}</ActionLink>
            </DialogFoot>
          </form>
        </Dialog>
      ) : null}

      {archiveTarget ? (
        <Dialog
          titleId="animal-delete-title"
          title={t('deleteTitle')}
          close={
            <a href={closingHref} aria-label={t('cancel')}>
              <CloseIcon />
            </a>
          }
        >
          <ConfirmText>{t('confirmArchive', { id: archiveTarget.identificationNumber })}</ConfirmText>
          <DialogFoot>
            <form action={archiveAnimalAction.bind(null, farm.id, locale, archiveTarget.id)}>
              <ActionButton type="submit" variant="danger">
                {t('delete')}
              </ActionButton>
            </form>
            <ActionLink href={closingHref}>{t('cancel')}</ActionLink>
          </DialogFoot>
        </Dialog>
      ) : null}
    </section>
  );
}
