import type { FormHTMLAttributes, HTMLAttributes, ReactNode, SelectHTMLAttributes } from 'react';
import styles from './FilterBar.module.css';

type BarProps = {
  as?: 'form' | 'div';
  tone?: 'card' | 'plain';
  children: ReactNode;
  className?: string;
} & FormHTMLAttributes<HTMLFormElement> &
  HTMLAttributes<HTMLDivElement>;

export function FilterBar({ as = 'div', tone = 'card', children, className, ...rest }: BarProps) {
  const classes = [styles.bar, tone === 'plain' ? styles.plainBar : '', className].filter(Boolean).join(' ');
  if (as === 'form') {
    return (
      <form className={classes} {...rest}>
        {children}
      </form>
    );
  }
  return (
    <div className={classes} {...rest}>
      {children}
    </div>
  );
}

export function FilterSearch({ children }: { children: ReactNode }) {
  return <div className={styles.search}>{children}</div>;
}

export function FilterSelect({ children, ...rest }: SelectHTMLAttributes<HTMLSelectElement> & { children: ReactNode }) {
  return (
    <div className={styles.selectWrap}>
      <select {...rest}>{children}</select>
    </div>
  );
}

export function FilterActions({ children }: { children: ReactNode }) {
  return <div className={styles.filterActions}>{children}</div>;
}
