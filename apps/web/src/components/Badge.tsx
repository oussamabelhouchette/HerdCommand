import type { HTMLAttributes } from 'react';
import styles from './Badge.module.css';

type Tone = 'neutral' | 'success' | 'warning' | 'error' | 'info';

type Props = HTMLAttributes<HTMLSpanElement> & {
  tone?: Tone;
};

export function Badge({ tone = 'neutral', className, children, ...rest }: Props) {
  return (
    <span className={[styles.badge, styles[tone], className].filter(Boolean).join(' ')} {...rest}>
      <span className={styles.dot} aria-hidden />
      {children}
    </span>
  );
}
