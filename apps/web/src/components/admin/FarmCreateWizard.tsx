'use client';

import { useEffect, useMemo, useRef, useState } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSession } from 'next-auth/react';
import { ApiRequestError, fieldErrorsMap } from '@/lib/api';
import type { CatalogFeature } from '@/lib/features';
import { selectedFeatureCodes } from '@/lib/features';
import {
  CREATE_INITIAL_STATUS,
  DEFAULT_FARM_CURRENCY,
  DEFAULT_FARM_LANGUAGE,
  DEFAULT_FARM_TIMEZONE,
  FARM_LANGUAGES,
  FARM_PLANS,
  GOVERNORATE_CODES,
  PLAN_CAPS,
  createPlatformFarm,
  newIdempotencyKey,
  wizardStepForErrors,
  type CreatePlatformFarmInput,
  type FarmPlan,
  type PlatformFarmCreated,
} from '@/lib/farms';
import { isValidEmail, lookupIdentity, ownerLookupStatus, type IdentityLookup } from '@/lib/identity';
import { CloseIcon } from '@/components/ui/Icons';
import { Dialog, DialogFoot, FormFields } from './AdminDialog';
import styles from './FarmSettings.module.css';

type Step = 1 | 2 | 3 | 4;

type FormState = {
  nameAr: string;
  nameEn: string;
  nameFr: string;
  governorateCode: string;
  address: string;
  timezone: string;
  defaultLanguage: string;
  currencyCode: string;
  ownerEmail: string;
  ownerDisplayName: string;
  ownerPhone: string;
  planCode: FarmPlan;
  enabledFeatureCodes: string[];
};

type Props = {
  open: boolean;
  features: CatalogFeature[];
  onClose: () => void;
  onCreated: (created: PlatformFarmCreated) => void;
};

const SEARCH_DEBOUNCE_MS = 400;

function emptyForm(features: CatalogFeature[]): FormState {
  return {
    nameAr: '',
    nameEn: '',
    nameFr: '',
    governorateCode: 'TN-11',
    address: '',
    timezone: DEFAULT_FARM_TIMEZONE,
    defaultLanguage: DEFAULT_FARM_LANGUAGE,
    currencyCode: DEFAULT_FARM_CURRENCY,
    ownerEmail: '',
    ownerDisplayName: '',
    ownerPhone: '',
    planCode: 'TRIAL',
    enabledFeatureCodes: selectedFeatureCodes(features),
  };
}

function toRequest(form: FormState): CreatePlatformFarmInput {
  return {
    nameAr: form.nameAr.trim(),
    nameEn: form.nameEn.trim() || undefined,
    nameFr: form.nameFr.trim(),
    governorateCode: form.governorateCode,
    address: form.address.trim() || undefined,
    timezone: form.timezone,
    defaultLanguage: form.defaultLanguage,
    currencyCode: form.currencyCode,
    initialStatus: CREATE_INITIAL_STATUS,
    owner: {
      email: form.ownerEmail.trim().toLowerCase(),
      displayName: form.ownerDisplayName.trim(),
      phoneNumber: form.ownerPhone.trim() || undefined,
    },
    subscription: {
      planCode: form.planCode,
    },
    enabledFeatureCodes: form.enabledFeatureCodes,
  };
}

export function FarmCreateWizard({ open, features, onClose, onCreated }: Props) {
  const t = useTranslations('farmSettings');
  const locale = useLocale();
  const { data: session } = useSession();
  const token = session?.accessToken;

  const [step, setStep] = useState<Step>(1);
  const [form, setForm] = useState<FormState>(() => emptyForm(features));
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [formError, setFormError] = useState('');
  const [saving, setSaving] = useState(false);
  const [lookup, setLookup] = useState<IdentityLookup | null>(null);
  const [lookupLoading, setLookupLoading] = useState(false);
  const [lookupError, setLookupError] = useState(false);
  const idempotencyKey = useRef<string | null>(null);
  const payloadFingerprint = useRef('');

  const ownerStatus = ownerLookupStatus(lookup, {
    loading: lookupLoading,
    error: lookupError,
    email: form.ownerEmail,
  });
  const planCaps = PLAN_CAPS[form.planCode];
  const featureNames = useMemo(
    () => new Map(features.map((feature) => [feature.code, feature.name])),
    [features],
  );

  useEffect(() => {
    if (!open) {
      return;
    }
    setStep(1);
    setForm(emptyForm(features));
    setFieldErrors({});
    setFormError('');
    setSaving(false);
    setLookup(null);
    setLookupLoading(false);
    setLookupError(false);
    idempotencyKey.current = null;
    payloadFingerprint.current = '';
  }, [open, features]);

  useEffect(() => {
    if (!open || !token || !isValidEmail(form.ownerEmail)) {
      setLookup(null);
      setLookupLoading(false);
      setLookupError(false);
      return;
    }
    let cancelled = false;
    const handle = window.setTimeout(() => {
      setLookupLoading(true);
      setLookupError(false);
      lookupIdentity(token, locale, form.ownerEmail)
        .then((result) => {
          if (cancelled) {
            return;
          }
          setLookup(result);
          if (result.found && result.displayName) {
            setForm((current) =>
              current.ownerDisplayName ? current : { ...current, ownerDisplayName: result.displayName ?? '' },
            );
          }
        })
        .catch((error) => {
          if (cancelled) {
            return;
          }
          if (error instanceof ApiRequestError && error.status === 401) {
            window.location.href = '/api/auth/federated-logout';
          }
          setLookup(null);
          setLookupError(!(error instanceof ApiRequestError && error.status === 400));
        })
        .finally(() => {
          if (!cancelled) {
            setLookupLoading(false);
          }
        });
    }, SEARCH_DEBOUNCE_MS);
    return () => {
      cancelled = true;
      window.clearTimeout(handle);
    };
  }, [form.ownerEmail, open, token, locale]);

  if (!open) {
    return null;
  }

  function handleAuthError(error: unknown) {
    if (error instanceof ApiRequestError && error.status === 401) {
      window.location.href = '/api/auth/federated-logout';
    }
  }

  function patch<K extends keyof FormState>(key: K, value: FormState[K]) {
    setForm((current) => ({ ...current, [key]: value }));
    setFieldErrors((current) => {
      const next = { ...current };
      delete next[String(key)];
      return next;
    });
  }

  function toggleFeature(code: string, enableable: boolean) {
    if (!enableable) {
      return;
    }
    setForm((current) => {
      const selected = current.enabledFeatureCodes.includes(code)
        ? current.enabledFeatureCodes.filter((item) => item !== code)
        : [...current.enabledFeatureCodes, code];
      return { ...current, enabledFeatureCodes: selected };
    });
  }

  function validateStep(current: Step): boolean {
    const errors: Record<string, string> = {};
    if (current === 1) {
      if (!form.nameAr.trim()) {
        errors.nameAr = t('wizard.required');
      }
      if (!form.nameFr.trim()) {
        errors.nameFr = t('wizard.required');
      }
      if (!form.governorateCode) {
        errors.governorateCode = t('wizard.required');
      }
    }
    if (current === 2) {
      if (!isValidEmail(form.ownerEmail)) {
        errors['owner.email'] = t('wizard.invalidEmail');
      }
      if (!form.ownerDisplayName.trim()) {
        errors['owner.displayName'] = t('wizard.required');
      }
      if (ownerStatus === 'disabled') {
        errors['owner.email'] = t('ownerDisabled');
      }
      if (ownerStatus === 'error') {
        errors['owner.email'] = t('ownerError');
      }
      if (ownerStatus === 'loading' || ownerStatus === 'idle' || ownerStatus === 'invalid') {
        errors['owner.email'] = errors['owner.email'] ?? t('wizard.lookupFirst');
      }
    }
    setFieldErrors(errors);
    return Object.keys(errors).length === 0;
  }

  function goNext() {
    if (!validateStep(step)) {
      return;
    }
    setStep((current) => (current < 4 ? ((current + 1) as Step) : current));
  }

  function goBack() {
    if (saving) {
      return;
    }
    setFormError('');
    setStep((current) => (current > 1 ? ((current - 1) as Step) : current));
  }

  function close() {
    if (saving) {
      return;
    }
    onClose();
  }

  async function submit() {
    if (!token || saving) {
      return;
    }
    if (!validateStep(1) || !validateStep(2)) {
      setStep(!form.nameAr.trim() || !form.nameFr.trim() ? 1 : 2);
      return;
    }
    const body = toRequest(form);
    const fingerprint = JSON.stringify(body);
    if (!idempotencyKey.current || payloadFingerprint.current !== fingerprint) {
      idempotencyKey.current = newIdempotencyKey();
      payloadFingerprint.current = fingerprint;
    }
    setSaving(true);
    setFormError('');
    try {
      const created = await createPlatformFarm(token, locale, body, idempotencyKey.current);
      onCreated(created);
    } catch (error) {
      handleAuthError(error);
      const mapped = fieldErrorsMap(error);
      setFieldErrors(mapped);
      if (Object.keys(mapped).length > 0) {
        setStep(wizardStepForErrors(mapped));
      }
      setFormError(error instanceof Error ? error.message : t('wizard.createError'));
    } finally {
      setSaving(false);
    }
  }

  const steps: { id: Step; label: string }[] = [
    { id: 1, label: t('wizard.stepFarm') },
    { id: 2, label: t('wizard.stepOwner') },
    { id: 3, label: t('wizard.stepFeatures') },
    { id: 4, label: t('wizard.stepReview') },
  ];

  return (
    <Dialog
      size="wide"
      titleId="farm-wizard-title"
      title={t('wizard.title')}
      close={
        <button type="button" onClick={close} aria-label={t('close')} disabled={saving}>
          <CloseIcon />
        </button>
      }
    >
        <ol className={styles.wizardSteps} data-wizard-step={step}>
          {steps.map((item) => (
            <li key={item.id} data-active={item.id === step || undefined} data-done={item.id < step || undefined}>
              <span>{item.id}</span>
              {item.label}
            </li>
          ))}
        </ol>

        {step === 1 ? (
          <FormFields>
            <div className={styles.field}>
              <label htmlFor="wizard-nameAr">{t('wizard.nameAr')}</label>
              <input
                id="wizard-nameAr"
                value={form.nameAr}
                onChange={(event) => patch('nameAr', event.target.value)}
                required
                maxLength={150}
                aria-invalid={fieldErrors.nameAr ? true : undefined}
              />
              {fieldErrors.nameAr ? <span className={styles.fieldError}>{fieldErrors.nameAr}</span> : null}
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-nameFr">{t('wizard.nameFr')}</label>
              <input
                id="wizard-nameFr"
                value={form.nameFr}
                onChange={(event) => patch('nameFr', event.target.value)}
                required
                maxLength={150}
                aria-invalid={fieldErrors.nameFr ? true : undefined}
              />
              {fieldErrors.nameFr ? <span className={styles.fieldError}>{fieldErrors.nameFr}</span> : null}
            </div>
            <div className={`${styles.field} ${styles.fieldFull}`}>
              <label htmlFor="wizard-nameEn">{t('wizard.nameEn')}</label>
              <input
                id="wizard-nameEn"
                value={form.nameEn}
                onChange={(event) => patch('nameEn', event.target.value)}
                maxLength={150}
              />
              <span className={styles.help}>{t('wizard.nameEnHelp')}</span>
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-governorate">{t('detailsGovernorate')}</label>
              <select
                id="wizard-governorate"
                value={form.governorateCode}
                onChange={(event) => patch('governorateCode', event.target.value)}
              >
                {GOVERNORATE_CODES.map((code) => (
                  <option key={code} value={code}>
                    {t.has(`governorate.${code}`) ? t(`governorate.${code}`) : code}
                  </option>
                ))}
              </select>
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-language">{t('detailsLanguage')}</label>
              <select
                id="wizard-language"
                value={form.defaultLanguage}
                onChange={(event) => patch('defaultLanguage', event.target.value)}
              >
                {FARM_LANGUAGES.map((code) => (
                  <option key={code} value={code}>
                    {code === 'ar' ? t('wizard.langAr') : t('wizard.langEn')}
                  </option>
                ))}
              </select>
            </div>
            <div className={`${styles.field} ${styles.fieldFull}`}>
              <label htmlFor="wizard-address">{t('detailsAddress')}</label>
              <input
                id="wizard-address"
                value={form.address}
                onChange={(event) => patch('address', event.target.value)}
                maxLength={500}
              />
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-timezone">{t('detailsTimezone')}</label>
              <input id="wizard-timezone" value={form.timezone} readOnly />
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-currency">{t('detailsCurrency')}</label>
              <input id="wizard-currency" value={form.currencyCode} readOnly />
            </div>
            <div className={`${styles.field} ${styles.fieldFull}`}>
              <label htmlFor="wizard-status">{t('colStatus')}</label>
              <input id="wizard-status" value={t('status.SETUP')} readOnly />
              <span className={styles.help}>{t('wizard.statusHelp')}</span>
            </div>
          </FormFields>
        ) : null}

        {step === 2 ? (
          <FormFields>
            <div className={`${styles.field} ${styles.fieldFull}`}>
              <label htmlFor="wizard-owner-email">{t('ownerEmail')}</label>
              <input
                id="wizard-owner-email"
                type="email"
                value={form.ownerEmail}
                onChange={(event) => patch('ownerEmail', event.target.value)}
                placeholder={t('ownerPlaceholder')}
                autoComplete="off"
                aria-invalid={fieldErrors['owner.email'] ? true : undefined}
              />
              {fieldErrors['owner.email'] ? (
                <span className={styles.fieldError}>{fieldErrors['owner.email']}</span>
              ) : null}
              {ownerStatus === 'loading' ? <p className={styles.ownerState}>{t('ownerLoading')}</p> : null}
              {ownerStatus === 'found' ? <p className={styles.ownerState}>{t('wizard.ownerFound')}</p> : null}
              {ownerStatus === 'invitation' ? <p className={styles.ownerState}>{t('ownerInvite')}</p> : null}
              {ownerStatus === 'disabled' ? (
                <p className={`${styles.ownerState} ${styles.statusError}`}>{t('ownerDisabled')}</p>
              ) : null}
              {ownerStatus === 'error' ? (
                <p className={`${styles.ownerState} ${styles.statusError}`}>{t('ownerError')}</p>
              ) : null}
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-owner-name">{t('wizard.ownerName')}</label>
              <input
                id="wizard-owner-name"
                value={form.ownerDisplayName}
                onChange={(event) => patch('ownerDisplayName', event.target.value)}
                maxLength={150}
                aria-invalid={fieldErrors['owner.displayName'] ? true : undefined}
              />
              {fieldErrors['owner.displayName'] ? (
                <span className={styles.fieldError}>{fieldErrors['owner.displayName']}</span>
              ) : null}
            </div>
            <div className={styles.field}>
              <label htmlFor="wizard-owner-phone">{t('wizard.ownerPhone')}</label>
              <input
                id="wizard-owner-phone"
                value={form.ownerPhone}
                onChange={(event) => patch('ownerPhone', event.target.value)}
                placeholder="+21620000000"
                dir="ltr"
              />
              <span className={styles.help}>{t('wizard.ownerPhoneHelp')}</span>
            </div>
          </FormFields>
        ) : null}

        {step === 3 ? (
          <FormFields>
            <div className={`${styles.field} ${styles.fieldFull}`}>
              <label htmlFor="wizard-plan">{t('colPlan')}</label>
              <select
                id="wizard-plan"
                value={form.planCode}
                onChange={(event) => patch('planCode', event.target.value as FarmPlan)}
              >
                {FARM_PLANS.map((code) => (
                  <option key={code} value={code}>
                    {t(`plan.${code}`)}
                  </option>
                ))}
              </select>
              <span className={styles.help}>
                {t('wizard.planCaps', { animals: planCaps.maxActiveAnimals, team: planCaps.maxTeamMembers })}
              </span>
            </div>
            <div className={`${styles.field} ${styles.fieldFull}`}>
              <span className={styles.ownerLabel}>{t('colFeatures')}</span>
              <ul className={styles.features}>
                {features.map((feature) => {
                  const disabled = !feature.enableable;
                  const checked = form.enabledFeatureCodes.includes(feature.code);
                  return (
                    <li
                      key={feature.id}
                      className={disabled ? styles.featureDisabled : undefined}
                      data-feature-code={feature.code}
                    >
                      <label className={styles.featureRow}>
                        <input
                          type="checkbox"
                          value={feature.code}
                          checked={checked}
                          disabled={disabled}
                          onChange={() => toggleFeature(feature.code, feature.enableable)}
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
            </div>
          </FormFields>
        ) : null}

        {step === 4 ? (
          <div className={styles.detailsBody}>
            <dl className={styles.details} data-wizard-review="true">
              <div>
                <dt>{t('wizard.nameAr')}</dt>
                <dd>{form.nameAr}</dd>
              </div>
              <div>
                <dt>{t('wizard.nameFr')}</dt>
                <dd>{form.nameFr}</dd>
              </div>
              {form.nameEn ? (
                <div>
                  <dt>{t('wizard.nameEn')}</dt>
                  <dd>{form.nameEn}</dd>
                </div>
              ) : null}
              <div>
                <dt>{t('detailsGovernorate')}</dt>
                <dd>{t.has(`governorate.${form.governorateCode}`) ? t(`governorate.${form.governorateCode}`) : form.governorateCode}</dd>
              </div>
              <div>
                <dt>{t('colOwner')}</dt>
                <dd>
                  {form.ownerDisplayName}
                  <span className={`${styles.sub} ${styles.ltr}`}>{form.ownerEmail}</span>
                  {ownerStatus === 'invitation' ? <span className={styles.sub}>{t('ownerInvite')}</span> : null}
                </dd>
              </div>
              <div>
                <dt>{t('colPlan')}</dt>
                <dd>{t(`plan.${form.planCode}`)}</dd>
              </div>
              <div>
                <dt>{t('colFeatures')}</dt>
                <dd>
                  <div className={styles.featuresCell}>
                    {form.enabledFeatureCodes.map((code) => (
                      <span key={code} className={styles.chip} data-feature-code={code}>
                        {featureNames.get(code) ?? code}
                      </span>
                    ))}
                  </div>
                </dd>
              </div>
              <div>
                <dt>{t('colStatus')}</dt>
                <dd>{t('status.SETUP')}</dd>
              </div>
            </dl>
            {formError ? <p className={styles.formError}>{formError}</p> : null}
            {saving ? <p className={styles.ownerState}>{t('wizard.creating')}</p> : null}
          </div>
        ) : null}

        <DialogFoot>
          {step > 1 ? (
            <button type="button" className={styles.btn} onClick={goBack} disabled={saving}>
              {t('wizard.back')}
            </button>
          ) : (
            <button type="button" className={styles.btn} onClick={close} disabled={saving}>
              {t('close')}
            </button>
          )}
          {step < 4 ? (
            <button type="button" className={`${styles.btn} ${styles.btnPrimary}`} onClick={goNext}>
              {t('wizard.next')}
            </button>
          ) : (
            <button
              type="button"
              className={`${styles.btn} ${styles.btnPrimary}`}
              onClick={() => void submit()}
              disabled={saving}
            >
              {saving ? t('wizard.creating') : t('wizard.create')}
            </button>
          )}
        </DialogFoot>
    </Dialog>
  );
}
