import type { ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './Button.module.css';

type Variant = 'primary' | 'secondary' | 'tertiary' | 'destructive';

type Props = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  loading?: boolean;
  children: ReactNode;
};

export function Button({
  variant = 'primary',
  loading = false,
  disabled,
  children,
  className,
  type = 'button',
  ...rest
}: Props) {
  const classes = [styles.button, styles[variant], className].filter(Boolean).join(' ');
  return (
    <button
      type={type}
      className={classes}
      disabled={disabled || loading}
      aria-busy={loading || undefined}
      {...rest}
    >
      {loading ? <span className={styles.spinner} aria-hidden /> : null}
      <span>{children}</span>
    </button>
  );
}
