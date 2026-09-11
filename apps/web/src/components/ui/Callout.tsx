import type { ReactNode } from 'react';
import styles from './Callout.module.css';

type Tone = 'warning' | 'success';

export function Callout({
  icon,
  title,
  children,
  tone = 'warning',
}: {
  icon: ReactNode;
  title: ReactNode;
  children?: ReactNode;
  tone?: Tone;
}) {
  return (
    <div className={styles.callout}>
      <div className={styles.calloutRow}>
        <span className={`${styles.calloutIcon} ${tone === 'success' ? styles.calloutSuccess : styles.calloutWarning}`}>{icon}</span>
        <div>
          <h2>{title}</h2>
          {children ? <p className={styles.calloutBody}>{children}</p> : null}
        </div>
      </div>
    </div>
  );
}
