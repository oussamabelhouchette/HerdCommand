import type { AnchorHTMLAttributes, ButtonHTMLAttributes, ReactNode } from 'react';
import styles from './ActionButton.module.css';

type Variant = 'default' | 'primary' | 'danger' | 'icon';

function classNameFor(variant: Variant, className?: string) {
  const variantClass = variant === 'icon' ? styles.iconOnly : variant !== 'default' ? styles[variant] : '';
  return [styles.btn, variantClass, className].filter(Boolean).join(' ');
}

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: Variant;
  children: ReactNode;
};

export function ActionButton({ variant = 'default', className, type = 'button', children, ...rest }: ButtonProps) {
  return (
    <button type={type} className={classNameFor(variant, className)} {...rest}>
      {children}
    </button>
  );
}

type LinkProps = AnchorHTMLAttributes<HTMLAnchorElement> & {
  variant?: Variant;
  href: string;
  children: ReactNode;
};

export function ActionLink({ variant = 'default', className, href, children, ...rest }: LinkProps) {
  return (
    <a href={href} className={classNameFor(variant, className)} {...rest}>
      {children}
    </a>
  );
}
