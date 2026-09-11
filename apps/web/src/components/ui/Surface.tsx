import type { ReactNode } from 'react';
import styles from './Surface.module.css';

export function Surface({ children }: { children: ReactNode }) {
  return <div className={styles.surface}>{children}</div>;
}
