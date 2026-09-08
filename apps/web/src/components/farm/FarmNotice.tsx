import styles from './FarmWorkspace.module.css';

type Props = {
  title: string;
  body?: string;
};

export function FarmNotice({ title, body }: Props) {
  return (
    <section>
      <div className={styles.header}>
        <div className={styles.title}>
          <h1>{title}</h1>
          {body ? <p>{body}</p> : null}
        </div>
      </div>
    </section>
  );
}
