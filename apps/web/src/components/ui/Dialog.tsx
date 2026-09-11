import type { ReactNode } from 'react';
import styles from './Dialog.module.css';

type DialogProps = {
  titleId: string;
  title: ReactNode;
  close: ReactNode;
  children: ReactNode;
  size?: 'default' | 'wide';
};

export function Dialog({ titleId, title, close, children, size = 'default' }: DialogProps) {
  return (
    <div className={styles.backdrop}>
      <div
        className={size === 'wide' ? `${styles.dialogPanel} ${styles.wide}` : styles.dialogPanel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
      >
        <div className={styles.head}>
          <h2 id={titleId}>{title}</h2>
          <div className={styles.close}>{close}</div>
        </div>
        {children}
      </div>
    </div>
  );
}

export function DialogFoot({ children }: { children: ReactNode }) {
  return <div className={styles.foot}>{children}</div>;
}

export function FormFields({ children }: { children: ReactNode }) {
  return <div className={styles.dialogForm}>{children}</div>;
}

export function FormField({ full = false, children }: { full?: boolean; children: ReactNode }) {
  return <div className={full ? `${styles.dialogField} ${styles.dialogFieldFull}` : styles.dialogField}>{children}</div>;
}

export function ConfirmText({ children }: { children: ReactNode }) {
  return <p className={styles.confirmText}>{children}</p>;
}

export function FieldHint({ children }: { children: ReactNode }) {
  return <span className={styles.dialogHelp}>{children}</span>;
}

export function FieldError({ children }: { children: ReactNode }) {
  return <span className={styles.dialogFieldError}>{children}</span>;
}

export function FormError({ children }: { children: ReactNode }) {
  return <p className={styles.dialogFormError}>{children}</p>;
}
