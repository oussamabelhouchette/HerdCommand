'use client';

import { useEffect, useMemo, useRef, useState, type FormEvent } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSession } from 'next-auth/react';
import { ApiRequestError, fieldErrorsMap } from '@/lib/api';
import {
  COLOR_TOKENS,
  isRequiredStatus,
  listStatuses,
  statusLabel,
  updateStatus,
  updateStatusActive,
  type AnimalStatus,
  type ColorToken,
  type StatusPage,
} from '@/lib/statuses';
import { BanIcon, CheckIcon, CloseIcon, InfoIcon, LockIcon, PenIcon, SearchIcon } from './AdminIcons';
import styles from './BreedManagement.module.css';
import statusStyles from './StatusManagement.module.css';

type Props = {
  initialPage: StatusPage;
  initialError?: string;
  onStatsChange?: (stats: { total: number }) => void;
};

type FormState = {
  labelAr: string;
  labelEn: string;
  colorToken: ColorToken;
  displayOrder: string;
  visibleInFilter: boolean;
  active: boolean;
};

export function StatusManagement({ initialPage, initialError, onStatsChange }: Props) {
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
  const [editing, setEditing] = useState<AnimalStatus | null>(null);
  const [form, setForm] = useState<FormState | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [toast, setToast] = useState('');
  const [pendingStatus, setPendingStatus] = useState<AnimalStatus | null>(null);
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
  }, [filters.search, filters.active, filters.page, token, locale]);

  async function reload(query = filters) {
    if (!token) {
      return;
    }
    setLoading(true);
    setListError('');
    try {
      setData(await listStatuses(token, locale, query));
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('statusLoadError'));
    } finally {
      setLoading(false);
    }
  }

  function handleAuthError(error: unknown) {
    if (error instanceof ApiRequestError && error.status === 401) {
      window.location.href = '/api/auth/federated-logout';
    }
  }

  function openEdit(row: AnimalStatus) {
    setEditing(row);
    setForm({
      labelAr: row.labelAr,
      labelEn: row.labelEn,
      colorToken: row.colorToken,
      displayOrder: String(row.displayOrder),
      visibleInFilter: row.visibleInFilter,
      active: row.active,
    });
    setFieldErrors({});
    setFormError('');
  }

  function closeModal() {
    if (saving) {
      return;
    }
    setEditing(null);
    setForm(null);
  }

  async function onSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!token || !editing || !form) {
      return;
    }
    setSaving(true);
    setFieldErrors({});
    setFormError('');
    const displayOrder = Number.parseInt(form.displayOrder, 10);
    try {
      await updateStatus(token, locale, editing.code, {
        labelAr: form.labelAr.trim(),
        labelEn: form.labelEn.trim(),
        colorToken: form.colorToken,
        displayOrder: Number.isFinite(displayOrder) ? Math.max(0, displayOrder) : 0,
        visibleInFilter: form.visibleInFilter,
        active: form.active,
      });
      setEditing(null);
      setForm(null);
      setToast(t('saved'));
      await reload();
    } catch (error) {
      handleAuthError(error);
      setFieldErrors(fieldErrorsMap(error));
      if (error instanceof ApiRequestError && error.body?.code === 'STATUS_REQUIRED') {
        setFormError(error.body.message);
        return;
      }
      setFormError(error instanceof Error ? error.message : t('saveError'));
    } finally {
      setSaving(false);
    }
  }

  async function applyStatus(row: AnimalStatus, active: boolean) {
    if (!token) {
      return;
    }
    setPendingStatus(null);
    try {
      await updateStatusActive(token, locale, row.code, active);
      setToast(active ? t('statusActivated') : t('statusDeactivated'));
      await reload();
    } catch (error) {
      handleAuthError(error);
      setListError(error instanceof Error ? error.message : t('saveError'));
    }
  }

  const pageCount = Math.max(1, Math.ceil(data.total / data.size));
  const previewLabel = form
    ? statusLabel({ labelAr: form.labelAr, labelEn: form.labelEn }, locale)
    : '';

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
            placeholder={t('statusSearchPlaceholder')}
            aria-label={t('statusSearchPlaceholder')}
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
      </div>
      <div className={styles.tableWrap}>
        {listError ? <p className={styles.listError}>{listError}</p> : null}
        <table className={styles.table}>
          <thead>
            <tr>
              <th>{t('colCode')}</th>
              <th>{t('colLabel')}</th>
              <th>{t('colColor')}</th>
              <th>{t('colFilter')}</th>
              <th>{t('colStatus')}</th>
              <th>{t('colActions')}</th>
            </tr>
          </thead>
          <tbody>
            {data.items.length === 0 ? (
              <tr>
                <td colSpan={6}>{loading ? t('loading') : t('empty')}</td>
              </tr>
            ) : (
              data.items.map((row) => (
                <tr key={row.code}>
                  <td>
                    <span className={statusStyles.codeCell}>
                      <span className={styles.code}>{row.code}</span>
                      {row.systemProtected ? (
                        <span className={statusStyles.lock} title={t('protectedCode')}>
                          <LockIcon />
                        </span>
                      ) : null}
                    </span>
                  </td>
                  <td>
                    <b>{row.labelAr}</b>
                    <span className={styles.sub}>{row.labelEn}</span>
                  </td>
                  <td>
                    <span className={`${statusStyles.badge} ${statusStyles[row.colorToken]}`}>
                      {statusLabel(row, locale)}
                    </span>
                  </td>
                  <td>
                    <span className={`${styles.pill} ${row.visibleInFilter ? styles.on : styles.off}`}>
                      {row.visibleInFilter ? t('filterVisible') : t('filterHidden')}
                    </span>
                  </td>
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
                        title={isRequiredStatus(row.code) ? t('cannotDeactivateActive') : t('deactivate')}
                        disabled={isRequiredStatus(row.code)}
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
        {t('statusNote')}
      </div>

      {editing && form ? (
        <div
          className={styles.modalBg}
          onClick={(event) => {
            if (event.target === event.currentTarget) {
              closeModal();
            }
          }}
        >
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="status-modal-title">
            <div className={styles.modalHead}>
              <h2 id="status-modal-title">{t('statusEditTitle')}</h2>
              <button type="button" className={styles.close} onClick={closeModal} aria-label={t('cancel')}>
                <CloseIcon />
              </button>
            </div>
            <form onSubmit={onSubmit}>
              <div className={styles.form}>
                <div className={`${styles.field} ${styles.fieldFull}`}>
                  <label htmlFor="status-code">{t('code')}</label>
                  <input id="status-code" value={editing.code} readOnly disabled />
                  <span className={styles.help}>{t('codeImmutable')}</span>
                </div>
                <div className={styles.field}>
                  <label htmlFor="status-labelAr">{t('nameAr')}</label>
                  <input
                    id="status-labelAr"
                    value={form.labelAr}
                    onChange={(event) => setForm((current) => current && { ...current, labelAr: event.target.value })}
                    required
                    maxLength={100}
                    aria-invalid={fieldErrors.labelAr ? true : undefined}
                  />
                  {fieldErrors.labelAr ? <span className={styles.fieldError}>{fieldErrors.labelAr}</span> : null}
                </div>
                <div className={styles.field}>
                  <label htmlFor="status-labelEn">{t('nameEn')}</label>
                  <input
                    id="status-labelEn"
                    value={form.labelEn}
                    onChange={(event) => setForm((current) => current && { ...current, labelEn: event.target.value })}
                    required
                    maxLength={100}
                    aria-invalid={fieldErrors.labelEn ? true : undefined}
                  />
                  {fieldErrors.labelEn ? <span className={styles.fieldError}>{fieldErrors.labelEn}</span> : null}
                </div>
                <div className={`${styles.field} ${styles.fieldFull}`}>
                  <span id="status-color-label">{t('colorLabel')}</span>
                  <div className={statusStyles.swatches} role="radiogroup" aria-labelledby="status-color-label">
                    {COLOR_TOKENS.map((token) => (
                      <button
                        key={token}
                        type="button"
                        role="radio"
                        aria-checked={form.colorToken === token}
                        className={`${statusStyles.swatch} ${statusStyles[token]} ${
                          form.colorToken === token ? statusStyles.swatchSelected : ''
                        }`}
                        onClick={() => setForm((current) => current && { ...current, colorToken: token })}
                      >
                        {t(`colorTokens.${token}`)}
                      </button>
                    ))}
                  </div>
                  {fieldErrors.colorToken ? <span className={styles.fieldError}>{fieldErrors.colorToken}</span> : null}
                </div>
                <div className={`${styles.field} ${styles.fieldFull}`}>
                  <span>{t('preview')}</span>
                  <div className={statusStyles.preview} data-testid="status-preview">
                    <span className={`${statusStyles.badge} ${statusStyles[form.colorToken]}`}>
                      {previewLabel || t('previewEmpty')}
                    </span>
                    <span className={styles.help}>{t('previewHelp')}</span>
                  </div>
                </div>
                <div className={styles.field}>
                  <label htmlFor="status-order">{t('displayOrder')}</label>
                  <input
                    id="status-order"
                    type="number"
                    min={0}
                    value={form.displayOrder}
                    onChange={(event) =>
                      setForm((current) => current && { ...current, displayOrder: event.target.value })
                    }
                    required
                    aria-invalid={fieldErrors.displayOrder ? true : undefined}
                  />
                  {fieldErrors.displayOrder ? (
                    <span className={styles.fieldError}>{fieldErrors.displayOrder}</span>
                  ) : null}
                </div>
                <div className={styles.field}>
                  <label className={statusStyles.check}>
                    <input
                      type="checkbox"
                      checked={form.visibleInFilter}
                      onChange={(event) =>
                        setForm((current) => current && { ...current, visibleInFilter: event.target.checked })
                      }
                    />
                    {t('visibleInFilter')}
                  </label>
                  <label className={statusStyles.check}>
                    <input
                      type="checkbox"
                      checked={form.active}
                      disabled={isRequiredStatus(editing.code)}
                      onChange={(event) =>
                        setForm((current) => current && { ...current, active: event.target.checked })
                      }
                    />
                    {t('active')}
                  </label>
                  {isRequiredStatus(editing.code) ? (
                    <span className={styles.help}>{t('cannotDeactivateActive')}</span>
                  ) : null}
                </div>
                {formError ? <p className={styles.formError}>{formError}</p> : null}
              </div>
              <div className={styles.modalFoot}>
                <button type="submit" className={`${styles.btn} ${styles.btnPrimary}`} disabled={saving}>
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
          <div className={styles.modal} role="dialog" aria-modal="true" aria-labelledby="status-deactivate-title">
            <div className={styles.modalHead}>
              <h2 id="status-deactivate-title">{t('deactivateStatusTitle')}</h2>
              <button
                type="button"
                className={styles.close}
                onClick={() => setPendingStatus(null)}
                aria-label={t('cancel')}
              >
                <CloseIcon />
              </button>
            </div>
            <p className={styles.confirmText}>{t('confirmDeactivateStatus', { code: pendingStatus.code })}</p>
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
