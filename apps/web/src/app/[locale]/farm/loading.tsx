export default function FarmLoading() {
  return (
    <div aria-busy="true" aria-live="polite">
      <div style={{ height: 28, width: 220, borderRadius: 8, background: '#eeeae4', marginBottom: 12 }} />
      <div style={{ height: 16, width: 360, borderRadius: 8, background: '#f4f0e8', marginBottom: 22 }} />
      <div style={{ height: 220, borderRadius: 16, background: '#fff', border: '1px solid #ece3ca' }} />
    </div>
  );
}
