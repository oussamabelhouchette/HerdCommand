import type { ReactNode } from 'react';
import styles from './StatGrid.module.css';

type Tone = 1 | 2 | 3 | 4;

export function StatGrid({ columns = 3, children }: { columns?: 3 | 4; children: ReactNode }) {
  return (
    <section className={`${styles.statGrid} ${columns === 4 ? styles.statCols4 : styles.statCols3}`}>{children}</section>
  );
}

export function StatCard({
  icon,
  value,
  label,
  tone = 1,
  href,
}: {
  icon: ReactNode;
  value: ReactNode;
  label: ReactNode;
  tone?: Tone;
  href?: string;
}) {
  const inner = (
    <>
      <div className={styles.statIcon}>{icon}</div>
      <div>
        <strong>{value}</strong>
        <span>{label}</span>
      </div>
    </>
  );
  if (href) {
    return (
      <a href={href} className={styles.statCard} data-tone={tone}>
        {inner}
      </a>
    );
  }
  return (
    <div className={styles.statCard} data-tone={tone}>
      {inner}
    </div>
  );
}
