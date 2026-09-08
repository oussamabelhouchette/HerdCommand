'use client';

import { useEffect, useState } from 'react';
import { useLocale, useTranslations } from 'next-intl';
import { useSession } from 'next-auth/react';
import { ApiRequestError } from '@/lib/api';
import {
  isValidEmail,
  lookupIdentity,
  ownerLookupStatus,
  type IdentityLookup,
} from '@/lib/identity';
import styles from './FarmSettings.module.css';

const DEBOUNCE_MS = 400;

export function OwnerLookup() {
  const t = useTranslations('farmSettings');
  const locale = useLocale();
  const { data: session } = useSession();
  const token = session?.accessToken;

  const [email, setEmail] = useState('');
  const [debounced, setDebounced] = useState('');
  const [result, setResult] = useState<IdentityLookup | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);

  useEffect(() => {
    const handle = window.setTimeout(() => setDebounced(email.trim()), DEBOUNCE_MS);
    return () => window.clearTimeout(handle);
  }, [email]);

  useEffect(() => {
    if (!token || !debounced || !isValidEmail(debounced)) {
      setResult(null);
      setLoading(false);
      setError(false);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError(false);
    lookupIdentity(token, locale, debounced)
      .then((next) => {
        if (!cancelled) {
          setResult(next);
        }
      })
      .catch((cause) => {
        if (!cancelled) {
          setResult(null);
          setError(!(cause instanceof ApiRequestError && cause.status === 400));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [debounced, locale, token]);

  const status = ownerLookupStatus(result, { loading, error, email });

  return (
    <section className={styles.owner} data-owner-status={status}>
      <h2 className={styles.featuresTitle}>{t('ownerTitle')}</h2>
      <p className={styles.featuresHint}>{t('ownerHint')}</p>
      <label className={styles.ownerLabel} htmlFor="owner-email">
        {t('ownerEmail')}
      </label>
      <input
        id="owner-email"
        type="email"
        className={styles.ownerInput}
        value={email}
        onChange={(event) => setEmail(event.target.value)}
        placeholder={t('ownerPlaceholder')}
        autoComplete="off"
      />
      {status === 'loading' ? <p className={styles.ownerState}>{t('ownerLoading')}</p> : null}
      {status === 'found' && result ? (
        <p className={styles.ownerState} data-owner-id={result.keycloakUserId ?? ''}>
          {t('ownerFound', { name: result.displayName || result.email })}
        </p>
      ) : null}
      {status === 'invitation' ? <p className={styles.ownerState}>{t('ownerInvite')}</p> : null}
      {status === 'disabled' ? (
        <p className={`${styles.ownerState} ${styles.statusError}`}>{t('ownerDisabled')}</p>
      ) : null}
      {status === 'error' ? (
        <p className={`${styles.ownerState} ${styles.statusError}`}>{t('ownerError')}</p>
      ) : null}
    </section>
  );
}
