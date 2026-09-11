import type { ReactNode } from 'react';
import styles from './DataTable.module.css';

type Props = {
  error?: ReactNode;
  empty?: ReactNode;
  footer?: ReactNode;
  children?: ReactNode;
  tone?: 'card' | 'plain';
};

export function DataTable({ error, empty, footer, children, tone = 'card' }: Props) {
  return (
    <section className={tone === 'plain' ? `${styles.tableCard} ${styles.plainTable}` : styles.tableCard}>
      {error ? <div className={styles.tableError}>{error}</div> : null}
      {empty ? <div className={styles.tableEmpty}>{empty}</div> : null}
      {children ? <div className={styles.scroll}>{children}</div> : null}
      {footer}
    </section>
  );
}

export function DataTableFoot({ summary, children }: { summary?: ReactNode; children?: ReactNode }) {
  return (
    <footer className={styles.footer}>
      {summary ? <span>{summary}</span> : <span />}
      {children ? <div className={styles.pagination}>{children}</div> : null}
    </footer>
  );
}
