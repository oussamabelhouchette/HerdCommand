'use client';

import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSession } from 'next-auth/react';
import { ApiRequestError, fieldErrorsMap } from '@/lib/api';
import {
  SPECIES_CODES,
  createBreed,
  listBreeds,
  updateBreed,
  updateBreedStatus,
  type Breed,
  type BreedPage,
  type SpeciesCode,
} from '@/lib/breeds';
import {
  BanIcon,
  CheckIcon,
  ClockIcon,
  CloseIcon,
  DnaIcon,
  InfoIcon,
  LayersIcon,
  PenIcon,
  PlusIcon,
  SearchIcon,
  TagsIcon,
} from '@/components/ui/Icons';
import { ConfirmText, Dialog, DialogFoot, FormField, FormFields } from './AdminDialog';
import styles from './BreedManagement.module.css';

type Props = {
  initialPage: BreedPage;
  initialActiveCount: number;
  initialError?: string;
  hideChrome?: boolean;
  onStatsChange?: (stats: { activeCount: number; total: number }) => void;
};

type FormState = {
  code: string;
  nameAr: string;
  nameEn: string;
  speciesCode: SpeciesCode;
  displayOrder: string;
};

const emptyForm: FormState = {
  code: '',
  nameAr: '',
  nameEn: '',
  speciesCode: 'SHEEP',
  displayOrder: '0',
};

export function BreedManagement({
  initialPage,
  initialActiveCount,
  initialError,
  hideChrome = false,
  onStatsChange,
}: Props) {
  const t = useTranslations('animalSettings');
  const locale = useLocale();
  const { data: session } = useSession();
  const token = session?.accessToken;

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [species, setSpecies] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(initialPage);
  const [activeCount, setActiveCount] = useState(initialActiveCount);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState(initialError ?? '');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<Breed | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState('');
  const [pendingStatus, setPendingStatus] = useState<Breed | null>(null);
  const skipFirstFetch = useRef(true);

  useEffect(() => {
    const id = window.setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => window.clearTimeout(id);
  }, [search]);

  useEffect(() => {
    if (!token) {
      return;
    }
    void listBreeds(token, locale, { active: true, size: 1 })
      .then((result) => setActiveCount(result.total))
      .catch(() => {
        /* keep the server-provided count */
      });
  }, [token, locale]);

  useEffect(() => {
    onStatsChange?.({ activeCount, total: data.total });
  }, [activeCount, data.total, onStatsChange]);

  useEffect(() => {
    if (!toast) {
      return;
    }
    const id = window.setTimeout(() => setToast(''), 2200);
    return () => window.clearTimeout(id);
  }, [toast]);

  const filters = useMemo(
    () => ({
      search: debouncedSearch,
      speciesCode: species as SpeciesCode | '',
      active: status === '' ? ('' as const) : status === 'true',
      page,
    }),
    [debouncedSearch, species, status, page],
  );

  useEffect(() => {
    if (skipFirstFetch.current) {
      skipFirstFetch.current = false;
      return;
    }
    void reload(filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.search, filters.speciesCode, filters.active, filters.page, token, locale]);

  async function reload(query = filters) {
    if (!token) {
      return;
    }
    setLoading(true);
    setListError('');
    try {
      const [result, activeResult] = await Promise.all([
        listBreeds(token, locale, query),
        listBreeds(token, locale, { active: true, size: 1 }),
      ]);
      setData(result);
      setActiveCount(activeResult.total);
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('loadError'));
    } finally {
      setLoading(false);
    }
  }

  function handleAuthError(error: unknown) {
    if (error instanceof ApiRequestError && error.status === 401) {
      window.location.href = '/api/auth/federated-logout';
    }
  }

  function openCreate() {
    setEditing(null);
    setForm(emptyForm);
    setFieldErrors({});
    setFormError('');
    setModalOpen(true);
  }

  function openEdit(breed: Breed) {
    setEditing(breed);
    setForm({
      code: breed.code,
      nameAr: breed.nameAr,
      nameEn: breed.nameEn,
      speciesCode: breed.speciesCode,
      displayOrder: String(breed.displayOrder),
    });
    setFieldErrors({});
    setFormError('');
    setModalOpen(true);
  }

  function closeModal() {
    if (saving) {
      return;
    }
    setModalOpen(false);
    setEditing(null);
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token) {
      return;
    }
    setSaving(true);
    setFieldErrors({});
    setFormError('');
    const displayOrder = Number.parseInt(form.displayOrder, 10);
    const payload = {
      nameAr: form.nameAr.trim(),
      nameEn: form.nameEn.trim(),
      speciesCode: form.speciesCode,
      displayOrder: Number.isFinite(displayOrder) ? Math.max(0, displayOrder) : 0,
    };
    try {
      if (editing) {
        await updateBreed(token, locale, editing.id, payload);
      } else {
        await createBreed(token, locale, { ...payload, code: form.code.trim() });
      }
      setModalOpen(false);
      setEditing(null);
      setToast(t('saved'));
      await reload();
    } catch (error) {
      handleAuthError(error);
      const fields = fieldErrorsMap(error);
      setFieldErrors(fields);
      if (error instanceof ApiRequestError && error.body?.code === 'BREED_CODE_ALREADY_EXISTS') {
        setFormError(error.body.message);
        return;
      }
      setFormError(error instanceof Error ? error.message : t('saveError'));
    } finally {
      setSaving(false);
    }
  }

  async function applyStatus(breed: Breed, active: boolean) {
    if (!token) {
      return;
    }
    setPendingStatus(null);
    try {
      await updateBreedStatus(token, locale, breed.id, active);
      setToast(active ? t('activated') : t('deactivated'));
      await reload();
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('saveError'));
    }
  }

  const pageCount = Math.max(1, Math.ceil(data.total / data.size));

  return (
    <>
      {hideChrome ? null : (
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
              <button type="button" className={`${styles.btn} ${styles.btnPrimary}`} onClick={openCreate}>
                <PlusIcon />
                <span>{t('addBreed')}</span>
              </button>
            </div>
          </header>
          <section className={styles.stats}>
            <div className={styles.stat}>
              <div className={styles.statIcon}>
                <DnaIcon />
              </div>
              <div>
                <strong>{activeCount}</strong>
                <span>{t('statActiveBreeds')}</span>
              </div>
            </div>
            <div className={styles.stat}>
              <div className={styles.statIcon}>
                <TagsIcon />
              </div>
              <div>
                <strong>0</strong>
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
        </>
      )}

      <section className={hideChrome ? styles.embedded : styles.panel}>
        {hideChrome ? null : (
          <div className={styles.tabs}>
            <button type="button" className={`${styles.tab} ${styles.tabActive}`}>
              {t('tabBreeds')}
              <span className={styles.count}>{data.total}</span>
            </button>
            <button type="button" className={styles.tab} disabled title={t('tabSoon')}>
              {t('tabStatuses')}
              <span className={styles.count}>0</span>
            </button>
            <button type="button" className={styles.tab} disabled title={t('tabSoon')}>
              {t('tabGroups')}
              <span className={styles.count}>0</span>
            </button>
          </div>
        )}
        <div className={styles.toolbar}>
          <div className={styles.search}>
            <input
              value={search}
              onChange={(event) => {
                setSearch(event.target.value);
                setPage(0);
              }}
              placeholder={t('searchPlaceholder')}
              aria-label={t('searchPlaceholder')}
            />
            <SearchIcon />
          </div>
          <select
            className={styles.filter}
            value={species}
            onChange={(event) => {
              setSpecies(event.target.value);
              setPage(0);
            }}
            aria-label={t('filterAllSpecies')}
          >
            <option value="">{t('filterAllSpecies')}</option>
            {SPECIES_CODES.map((code) => (
              <option key={code} value={code}>
                {t(`species.${code}`)}
              </option>
            ))}
          </select>
          <select
            className={styles.filter}
            value={status}
            onChange={(event) => {
              setStatus(event.target.value);
              setPage(0);
            }}
            aria-label={t('filterAll')}
          >
            <option value="">{t('filterAll')}</option>
            <option value="true">{t('filterActive')}</option>
            <option value="false">{t('filterInactive')}</option>
          </select>
          <button type="button" className={`${styles.btn} ${styles.btnPrimary}`} onClick={openCreate}>
            <PlusIcon />
            <span>{t('addBreed')}</span>
          </button>
        </div>
        {listError ? <p className={styles.listError}>{listError}</p> : null}
        <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead>
              <tr>
                <th>{t('colCode')}</th>
                <th>{t('colName')}</th>
                <th>{t('colSpecies')}</th>
                <th>{t('colStatus')}</th>
                <th>{t('colActions')}</th>
              </tr>
            </thead>
            <tbody>
              {data.items.length === 0 ? (
                <tr>
                  <td colSpan={5}>{loading ? t('loading') : t('empty')}</td>
                </tr>
              ) : (
                data.items.map((breed) => (
                  <tr key={breed.id}>
                    <td>
                      <span className={styles.code}>{breed.code}</span>
                    </td>
                    <td>
                      <b>{breed.nameAr}</b>
                      <span className={styles.sub}>{breed.nameEn}</span>
                    </td>
                    <td>{t(`species.${breed.speciesCode}`)}</td>
                    <td>
                      <span className={`${styles.pill} ${breed.active ? styles.on : styles.off}`}>
                        {breed.active ? t('active') : t('inactive')}
                      </span>
                    </td>
                    <td className={styles.actions}>
                      <button type="button" title={t('edit')} onClick={() => openEdit(breed)}>
                        <PenIcon />
                      </button>
                      {breed.active ? (
                        <button
                          type="button"
                          className={styles.danger}
                          title={t('deactivate')}
                          onClick={() => setPendingStatus(breed)}
                        >
                          <BanIcon />
                        </button>
                      ) : (
                        <button type="button" title={t('activate')} onClick={() => applyStatus(breed, true)}>
                          <CheckIcon />
                        </button>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
        {data.total > data.size ? (
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

      {modalOpen ? (
        <Dialog
          titleId="breed-modal-title"
          title={editing ? t('editTitle') : t('createTitle')}
          close={
            <button type="button" onClick={closeModal} aria-label={t('cancel')}>
              <CloseIcon />
            </button>
          }
        >
            <form onSubmit={onSubmit}>
              <FormFields>
                <FormField>
                  <label htmlFor="breed-nameAr">{t('nameAr')}</label>
                  <input
                    id="breed-nameAr"
                    value={form.nameAr}
                    onChange={(event) => setForm((current) => ({ ...current, nameAr: event.target.value }))}
                    required
                    maxLength={100}
                    aria-invalid={fieldErrors.nameAr ? true : undefined}
                  />
                  {fieldErrors.nameAr ? <span className={styles.fieldError}>{fieldErrors.nameAr}</span> : null}
                </FormField>
                <FormField>
                  <label htmlFor="breed-nameEn">{t('nameEn')}</label>
                  <input
                    id="breed-nameEn"
                    value={form.nameEn}
                    onChange={(event) => setForm((current) => ({ ...current, nameEn: event.target.value }))}
                    required
                    maxLength={100}
                    aria-invalid={fieldErrors.nameEn ? true : undefined}
                  />
                  {fieldErrors.nameEn ? <span className={styles.fieldError}>{fieldErrors.nameEn}</span> : null}
                </FormField>
                <FormField>
                  <label htmlFor="breed-code">{t('code')}</label>
                  <input
                    id="breed-code"
                    value={form.code}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, code: event.target.value.toUpperCase() }))
                    }
                    required
                    maxLength={40}
                    disabled={Boolean(editing)}
                    aria-invalid={fieldErrors.code ? true : undefined}
                    autoComplete="off"
                  />
                  <span className={styles.help}>{editing ? t('codeImmutable') : t('codeHelp')}</span>
                  {fieldErrors.code ? <span className={styles.fieldError}>{fieldErrors.code}</span> : null}
                </FormField>
                <FormField>
                  <label htmlFor="breed-species">{t('speciesLabel')}</label>
                  <select
                    id="breed-species"
                    value={form.speciesCode}
                    onChange={(event) =>
                      setForm((current) => ({ ...current, speciesCode: event.target.value as SpeciesCode }))
                    }
                    required
                  >
                    {SPECIES_CODES.map((code) => (
                      <option key={code} value={code}>
                        {t(`species.${code}`)}
                      </option>
                    ))}
                  </select>
                </FormField>
                <FormField>
                  <label htmlFor="breed-order">{t('displayOrder')}</label>
                  <input
                    id="breed-order"
                    type="number"
                    min={0}
                    value={form.displayOrder}
                    onChange={(event) => setForm((current) => ({ ...current, displayOrder: event.target.value }))}
                    required
                    aria-invalid={fieldErrors.displayOrder ? true : undefined}
                  />
                  {fieldErrors.displayOrder ? (
                    <span className={styles.fieldError}>{fieldErrors.displayOrder}</span>
                  ) : null}
                </FormField>
                {formError ? (
                  <FormField full>
                    <p className={styles.formError}>{formError}</p>
                  </FormField>
                ) : null}
              </FormFields>
              <DialogFoot>
                <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={saving}>
                  {t('save')}
                </button>
                <button type="button" className={styles.btn} onClick={closeModal} disabled={saving}>
                  {t('cancel')}
                </button>
              </DialogFoot>
            </form>
        </Dialog>
      ) : null}

      {pendingStatus ? (
        <Dialog
          titleId="breed-deactivate-title"
          title={t('deactivateTitle')}
          close={
            <button type="button" onClick={() => setPendingStatus(null)} aria-label={t('cancel')}>
              <CloseIcon />
            </button>
          }
        >
            <ConfirmText>{t('confirmDeactivate', { code: pendingStatus.code })}</ConfirmText>
            <DialogFoot>
              <button
                type="button"
                className={`${styles.btn} ${styles.btnPrimary}`}
                onClick={() => applyStatus(pendingStatus, false)}
              >
                {t('deactivate')}
              </button>
              <button type="button" className={styles.btn} onClick={() => setPendingStatus(null)}>
                {t('cancel')}
              </button>
            </DialogFoot>
        </Dialog>
      ) : null}

      {toast ? (
        <div className={styles.toast} role="status">
          <CheckIcon />
          {toast}
        </div>
      ) : null}
    </>
  );
}
