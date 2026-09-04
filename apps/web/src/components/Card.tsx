import type { HTMLAttributes, ReactNode } from 'react';
import styles from './Card.module.css';

type Props = HTMLAttributes<HTMLElement> & {
  title?: string;
  children: ReactNode;
};

export function Card({ title, children, className, ...rest }: Props) {
  return (
    <section className={[styles.card, className].filter(Boolean).join(' ')} {...rest}>
      {title ? <h2 className={styles.title}>{title}</h2> : null}
      {children}
    </section>
  );
}
