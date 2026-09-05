'use client';

import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSession } from 'next-auth/react';
import { ApiRequestError, fieldErrorsMap } from '@/lib/api';
import {
  DEFAULT_GROUP_TYPE,
  createGroup,
  listGroups,
  updateGroup,
  updateGroupActive,
  type AnimalGroup,
  type GroupPage,
} from '@/lib/groups';
import { BanIcon, CheckIcon, CloseIcon, InfoIcon, PenIcon, PlusIcon, SearchIcon } from './AdminIcons';
import styles from './BreedManagement.module.css';
import groupStyles from './GroupManagement.module.css';

type Props = {
  farmId?: string;
  initialPage: GroupPage;
  initialError?: string;
  onStatsChange?: (stats: { total: number }) => void;
};

type FormState = {
  code: string;
  nameAr: string;
  nameEn: string;
  description: string;
  capacity: string;
};

const emptyForm: FormState = {
  code: '',
  nameAr: '',
  nameEn: '',
  description: '',
  capacity: '',
};

export function GroupManagement({ farmId, initialPage, initialError, onStatsChange }: Props) {
  const t = useTranslations('animalSettings');
  const locale = useLocale();
  const { data: session } = useSession();
  const token = session?.accessToken;

  const [search, setSearch] = useState('');
  const [debouncedSearch, setDebouncedSearch] = useState('');
  const [status, setStatus] = useState('');
  const [page, setPage] = useState(0);
  const [data, setData] = useState(initialPage);
  const [loading, setLoading] = useState(false);
  const [listError, setListError] = useState(initialError ?? '');
  const [modalOpen, setModalOpen] = useState(false);
  const [editing, setEditing] = useState<AnimalGroup | null>(null);
  const [form, setForm] = useState<FormState>(emptyForm);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState('');
  const [pendingStatus, setPendingStatus] = useState<AnimalGroup | null>(null);
  const skipFirstFetch = useRef(true);

  useEffect(() => {
    const id = window.setTimeout(() => setDebouncedSearch(search.trim()), 300);
    return () => window.clearTimeout(id);
  }, [search]);

  useEffect(() => {
    onStatsChange?.({ total: data.total });
  }, [data.total, onStatsChange]);

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
      active: status === '' ? ('' as const) : status === 'true',
      page,
    }),
    [debouncedSearch, status, page],
  );

  useEffect(() => {
    if (skipFirstFetch.current) {
      skipFirstFetch.current = false;
      return;
    }
    void reload(filters);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [filters.search, filters.active, filters.page, token, locale, farmId]);

  async function reload(query = filters) {
    if (!token || !farmId) {
      return;
    }
    setLoading(true);
    setListError('');
    try {
      setData(await listGroups(token, locale, farmId, query));
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('groupLoadError'));
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

  function openEdit(row: AnimalGroup) {
    setEditing(row);
    setForm({
      code: row.code,
      nameAr: row.nameAr,
      nameEn: row.nameEn,
      description: row.description ?? '',
      capacity: row.capacity == null ? '' : String(row.capacity),
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

  function parsedCapacity() {
    if (!form.capacity.trim()) {
      return null;
    }
    const value = Number.parseInt(form.capacity, 10);
    return Number.isFinite(value) ? value : null;
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || !farmId) {
      return;
    }
    setSaving(true);
    setFieldErrors({});
    setFormError('');
    const payload = {
      nameAr: form.nameAr.trim(),
      nameEn: form.nameEn.trim(),
      groupTypeCode: DEFAULT_GROUP_TYPE,
      description: form.description.trim() || undefined,
      capacity: parsedCapacity(),
    };
    try {
      if (editing) {
        await updateGroup(token, locale, farmId, editing.id, payload);
      } else {
        await createGroup(token, locale, farmId, { ...payload, code: form.code.trim() });
      }
      setModalOpen(false);
      setEditing(null);
      setToast(t('saved'));
      await reload();
    } catch (error) {
      handleAuthError(error);
      const fields = fieldErrorsMap(error);
      setFieldErrors(fields);
      if (error instanceof ApiRequestError && error.body?.code === 'GROUP_CODE_ALREADY_EXISTS') {
        setFormError(error.body.message);
        return;
      }
      setFormError(error instanceof Error ? error.message : t('saveError'));
    } finally {
      setSaving(false);
    }
  }

  async function applyStatus(row: AnimalGroup, active: boolean) {
    if (!token || !farmId) {
      return;
    }
    setPendingStatus(null);
    try {
      await updateGroupActive(token, locale, farmId, row.id, active);
      setToast(active ? t('groupActivated') : t('groupDeactivated'));
      await reload();
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('saveError'));
    }
  }

  const pageCount = Math.max(1, Math.ceil(data.total / data.size));

  return (
    <>
      <div className={styles.toolbar}>
        <div className={styles.search}>
          <SearchIcon />
          <input
            value={search}
            onChange={(event) => {
              setSearch(event.target.value);
              setPage(0);
            }}
            placeholder={t('groupSearchPlaceholder')}
            aria-label={t('groupSearchPlaceholder')}
          />
        </div>
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
        <button type="button" className={`${styles.btn} ${styles.btnPrimary}`} onClick={openCreate} disabled={!farmId}>
          <PlusIcon />
          <span>{t('addGroup')}</span>
        </button>
      </div>
      <div className={styles.tableWrap}>
        {listError ? <p className={styles.listError}>{listError}</p> : null}
        <table className={styles.table}>
          <thead>
            <tr>
              <th>{t('colCode')}</th>
              <th>{t('colName')}</th>
              <th>{t('colAnimalCount')}</th>
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
              data.items.map((row) => (
                <tr key={row.id}>
                  <td>
                    <span className={styles.code}>{row.code}</span>
                  </td>
                  <td>
                    <b>{row.nameAr}</b>
                    <span className={styles.sub}>{row.nameEn}</span>
                  </td>
                  <td>{row.animalCount}</td>
                  <td>
                    <span className={`${styles.pill} ${row.active ? styles.on : styles.off}`}>
                      {row.active ? t('active') : t('inactive')}
                    </span>
                  </td>
                  <td className={styles.actions}>
                    <button type="button" title={t('edit')} onClick={() => openEdit(row)}>
                      <PenIcon />
                    </button>
                    {row.active ? (
                      <button
                        type="button"
                        className={styles.danger}
                        title={t('deactivate')}
                        onClick={() => setPendingStatus(row)}
                      >
                        <BanIcon />
                      </button>
                    ) : (
                      <button type="button" title={t('activate')} onClick={() => applyStatus(row, true)}>
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
        {t('groupNote')}
      </div>

      {modalOpen ? (
        <div
          className={styles.modalBg}
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              closeModal();
            }
          }}
        >
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="group-modal-title">
            <div className={styles.modalHead}>
              <h2 id="group-modal-title">{editing ? t('groupEditTitle') : t('groupCreateTitle')}</h2>
              <button type="button" className={styles.close} onClick={closeModal} aria-label={t('cancel')}>
                <CloseIcon />
              </button>
            </div>
            <form onSubmit={onSubmit}>
              <div className={styles.form}>
                <div className={styles.field}>
                  <label htmlFor="group-nameAr">{t('nameAr')}</label>
                  <input
                    id="group-nameAr"
                    value={form.nameAr}
                    onChange={(event) => setForm((current) => ({ ...current, nameAr: event.target.value }))}
                    required
                    maxLength={100}
                    aria-invalid={fieldErrors.nameAr ? true : undefined}
                  />
                  {fieldErrors.nameAr ? <span className={styles.fieldError}>{fieldErrors.nameAr}</span> : null}
                </div>
                <div className={styles.field}>
                  <label htmlFor="group-nameEn">{t('nameEn')}</label>
                  <input
                    id="group-nameEn"
                    value={form.nameEn}
                    onChange={(event) => setForm((current) => ({ ...current, nameEn: event.target.value }))}
                    required
                    maxLength={100}
                    aria-invalid={fieldErrors.nameEn ? true : undefined}
                  />
                  {fieldErrors.nameEn ? <span className={styles.fieldError}>{fieldErrors.nameEn}</span> : null}
                </div>
                <div className={styles.field}>
                  <label htmlFor="group-code">{t('code')}</label>
                  <input
                    id="group-code"
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
                  <span className={styles.help}>{editing ? t('codeImmutable') : t('groupCodeHelp')}</span>
                  {fieldErrors.code ? <span className={styles.fieldError}>{fieldErrors.code}</span> : null}
                </div>
                <div className={styles.field}>
                  <label htmlFor="group-capacity">{t('capacity')}</label>
                  <input
                    id="group-capacity"
                    type="number"
                    min={1}
                    value={form.capacity}
                    onChange={(event) => setForm((current) => ({ ...current, capacity: event.target.value }))}
                    aria-invalid={fieldErrors.capacity ? true : undefined}
                  />
                  <span className={styles.help}>{t('capacityHelp')}</span>
                  {fieldErrors.capacity ? <span className={styles.fieldError}>{fieldErrors.capacity}</span> : null}
                </div>
                <div className={`${styles.field} ${styles.fieldFull}`}>
                  <label htmlFor="group-description">{t('description')}</label>
                  <textarea
                    id="group-description"
                    className={groupStyles.textarea}
                    value={form.description}
                    onChange={(event) => setForm((current) => ({ ...current, description: event.target.value }))}
                    maxLength={500}
                    rows={3}
                  />
                </div>
                {formError ? <p className={styles.formError}>{formError}</p> : null}
              </div>
              <div className={styles.modalFoot}>
                <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={saving || !farmId}>
                  {t('save')}
                </button>
                <button type="button" className={styles.btn} onClick={closeModal} disabled={saving}>
                  {t('cancel')}
                </button>
              </div>
            </form>
          </div>
        </div>
      ) : null}

      {pendingStatus ? (
        <div
          className={styles.modalBg}
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              setPendingStatus(null);
            }
          }}
        >
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="group-deactivate-title">
            <div className={styles.modalHead}>
              <h2 id="group-deactivate-title">{t('deactivateGroupTitle')}</h2>
              <button type="button" className={styles.close} onClick={() => setPendingStatus(null)} aria-label={t('cancel')}>
                <CloseIcon />
              </button>
            </div>
            <p className={styles.confirmText}>{t('confirmDeactivateGroup', { code: pendingStatus.code })}</p>
            <div className={styles.modalFoot}>
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
            </div>
          </div>
        </div>
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
