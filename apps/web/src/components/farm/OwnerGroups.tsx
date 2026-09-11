import { getLocale, getTranslations } from 'next-intl/server';
import { getPathname } from '@/i18n/navigation';
import { getSession } from '@/lib/session';
import type { OwnerFarm } from '@/lib/owner-farms';
import { animalsHref } from '@/lib/animals';
import {
  GROUP_PAGE_SIZE,
  emptyGroupPage,
  getGroup,
  groupListQuery,
  groupsHref,
  listGroups,
  normalizeGroupPage,
  paginationItems,
  summaryRange,
  type AnimalGroup,
  type GroupSearchState,
} from '@/lib/groups';
import { saveGroupAction, setGroupActiveAction } from '@/lib/group-actions';
import { BanIcon, CheckIcon, CloseIcon, InfoIcon, LockIcon, PenIcon, PlusIcon, SearchIcon } from '@/components/ui/Icons';
import { PageHeader } from '@/components/ui/PageHeader';
import { FilterBar, FilterSearch, FilterSelect } from '@/components/ui/FilterBar';
import { DataTable, DataTableFoot } from '@/components/ui/DataTable';
import { ConfirmText, Dialog, DialogFoot, FieldHint, FormError, FormField, FormFields } from '@/components/ui/Dialog';
import { ActionButton, ActionLink } from '@/components/ui/ActionButton';
import { Callout } from '@/components/ui/Callout';
import { Note } from '@/components/ui/Note';
import styles from './OwnerGroups.module.css';

type Props = {
  farm: OwnerFarm;
  query: GroupSearchState;
};

function formatCount(value: number, locale: string) {
  return new Intl.NumberFormat(locale === 'ar' ? 'ar-EG' : 'en', { useGrouping: false }).format(value);
}

function listHref(locale: string, farmId: string, query: Parameters<typeof groupsHref>[1] = {}) {
  const path = getPathname({ href: `/farm/${farmId}/groups`, locale });
  const href = groupsHref(farmId, query);
  const search = href.includes('?') ? href.slice(href.indexOf('?')) : '';
  return `${path}${search}`;
}

function animalsForGroupHref(locale: string, farmId: string, groupId: string) {
  const path = getPathname({ href: `/farm/${farmId}/animals`, locale });
  const href = animalsHref(farmId, { groupId });
  const search = href.includes('?') ? href.slice(href.indexOf('?')) : '';
  return `${path}${search}`;
}

export async function OwnerGroups({ farm, query }: Props) {
  const locale = await getLocale();
  const farmT = await getTranslations('farm');
  const t = await getTranslations('groups');

  if (!farm.animalManagementEnabled) {
    return (
      <section>
        <PageHeader title={farmT('groupsTitle')} subtitle={farmT('groupsHint', { farm: farm.name })} />
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

  const filters = groupListQuery(query);
  let data = { ...emptyGroupPage(), totalPages: 0 };
  let loadError = query.formError && !query.compose && !query.deactivateId ? query.formError : '';
  try {
    data = normalizeGroupPage(await listGroups(token, locale, farm.id, filters));
  } catch {
    loadError = t('loadError');
  }

  let editing: AnimalGroup | null = null;
  if (query.editId) {
    editing = data.items.find((group) => group.id === query.editId) ?? null;
    if (!editing) {
      try {
        editing = await getGroup(token, locale, farm.id, query.editId);
      } catch {
        editing = null;
      }
    }
  }

  const rows = data.items ?? [];
  const pages = paginationItems(data.page, data.totalPages);
  const range = summaryRange(data.page, data.size || GROUP_PAGE_SIZE, data.total);
  const showTable = !loadError && rows.length > 0;
  const showEmpty = !loadError && rows.length === 0;
  const formAction = getPathname({ href: `/farm/${farm.id}/groups`, locale });
  const closingHref = listHref(locale, farm.id, filters);
  const composeOpen = query.compose === 'new' || Boolean(editing);
  const deactivateTarget = query.deactivateId
    ? rows.find((group) => group.id === query.deactivateId) ?? { id: query.deactivateId, code: query.deactivateId }
    : null;

  return (
    <section data-farm-id={farm.id}>
      <PageHeader
        title={t('title')}
        subtitle={t('subtitle')}
        actions={
          <ActionLink variant="primary" href={listHref(locale, farm.id, { ...filters, compose: 'new' })}>
            <PlusIcon />
            {t('add')}
          </ActionLink>
        }
      />

      <FilterBar as="form" action={formAction} method="get">
        <FilterSearch>
          <SearchIcon />
          <input name="q" defaultValue={query.search ?? ''} placeholder={t('searchPlaceholder')} aria-label={t('searchPlaceholder')} />
        </FilterSearch>
        <FilterSelect
          name="active"
          defaultValue={query.active === true ? 'true' : query.active === false ? 'false' : ''}
          aria-label={t('filterAll')}
        >
          <option value="">{t('filterAll')}</option>
          <option value="true">{t('filterActive')}</option>
          <option value="false">{t('filterInactive')}</option>
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
                <th>{t('colCode')}</th>
                <th>{t('colName')}</th>
                <th>{t('colAnimals')}</th>
                <th>{t('colCapacity')}</th>
                <th>{t('colStatus')}</th>
                <th>{t('colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((group) => (
                <tr key={group.id} data-group-id={group.id}>
                  <td>
                    <span className={styles.code}>{group.code}</span>
                  </td>
                  <td className={styles.name}>
                    <b>{group.nameAr}</b>
                    <span className={styles.sub}>{group.nameEn}</span>
                  </td>
                  <td>
                    <a href={animalsForGroupHref(locale, farm.id, group.id)} className={styles.countLink} title={t('viewAnimals')}>
                      {formatCount(group.animalCount, locale)}
                    </a>
                  </td>
                  <td>{group.capacity == null ? t('unlimited') : formatCount(group.capacity, locale)}</td>
                  <td>
                    <span className={`${styles.pill} ${group.active ? styles.on : styles.off}`}>
                      {group.active ? t('active') : t('inactive')}
                    </span>
                  </td>
                  <td>
                    <div className={styles.rowActions}>
                      <a href={listHref(locale, farm.id, { ...filters, edit: group.id })} className={styles.edit} title={t('edit')}>
                        <PenIcon />
                      </a>
                      <span className={styles.divider} />
                      {group.active ? (
                        <a
                          href={listHref(locale, farm.id, { ...filters, deactivate: group.id })}
                          className={styles.delete}
                          title={t('deactivate')}
                        >
                          <BanIcon />
                        </a>
                      ) : (
                        <form action={setGroupActiveAction.bind(null, farm.id, locale, group.id, true)}>
                          <button type="submit" className={styles.edit} title={t('activate')}>
                            <CheckIcon />
                          </button>
                        </form>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        ) : null}
      </DataTable>

      <Note icon={<InfoIcon />}>{t('note')}</Note>

      {composeOpen ? (
        <Dialog
          titleId="group-form-title"
          title={editing ? t('editTitle') : t('addTitle')}
          close={
            <a href={closingHref} aria-label={t('cancel')}>
              <CloseIcon />
            </a>
          }
        >
          <form action={saveGroupAction.bind(null, farm.id, locale, editing?.id ?? '')}>
            <FormFields>
              <FormField>
                <label htmlFor="group-nameAr">{t('nameAr')}</label>
                <input id="group-nameAr" name="nameAr" defaultValue={editing?.nameAr ?? ''} required maxLength={100} />
              </FormField>
              <FormField>
                <label htmlFor="group-nameEn">{t('nameEn')}</label>
                <input id="group-nameEn" name="nameEn" defaultValue={editing?.nameEn ?? ''} required maxLength={100} />
              </FormField>
              <FormField>
                <label htmlFor="group-code">{t('code')}</label>
                <input
                  id="group-code"
                  name="code"
                  defaultValue={editing?.code ?? ''}
                  required={!editing}
                  maxLength={40}
                  disabled={Boolean(editing)}
                  autoComplete="off"
                />
                <FieldHint>{editing ? t('codeImmutable') : t('codeHelp')}</FieldHint>
              </FormField>
              <FormField>
                <label htmlFor="group-capacity">{t('capacity')}</label>
                <input
                  id="group-capacity"
                  name="capacity"
                  type="number"
                  min={1}
                  defaultValue={editing?.capacity == null ? '' : String(editing.capacity)}
                />
                <FieldHint>{t('capacityHelp')}</FieldHint>
              </FormField>
              <FormField full>
                <label htmlFor="group-description">{t('description')}</label>
                <textarea
                  id="group-description"
                  name="description"
                  defaultValue={editing?.description ?? ''}
                  maxLength={500}
                  rows={3}
                />
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

      {deactivateTarget ? (
        <Dialog
          titleId="group-deactivate-title"
          title={t('deactivateTitle')}
          close={
            <a href={closingHref} aria-label={t('cancel')}>
              <CloseIcon />
            </a>
          }
        >
          <ConfirmText>{t('confirmDeactivate', { code: deactivateTarget.code })}</ConfirmText>
          <DialogFoot>
            <form action={setGroupActiveAction.bind(null, farm.id, locale, deactivateTarget.id, false)}>
              <ActionButton type="submit" variant="danger">
                {t('deactivate')}
              </ActionButton>
            </form>
            <ActionLink href={closingHref}>{t('cancel')}</ActionLink>
          </DialogFoot>
        </Dialog>
      ) : null}
    </section>
  );
}
