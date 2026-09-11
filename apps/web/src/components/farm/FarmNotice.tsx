import { PageHeader } from '@/components/ui/PageHeader';

type Props = {
  title: string;
  body?: string;
};

export function FarmNotice({ title, body }: Props) {
  return (
    <section>
      <PageHeader title={title} subtitle={body} />
    </section>
  );
}
