import type { ReactNode } from 'react';
import styles from './Note.module.css';

export function Note({ icon, children }: { icon?: ReactNode; children: ReactNode }) {
  return (
    <div className={styles.hint}>
      {icon}
      {children}
    </div>
  );
}
