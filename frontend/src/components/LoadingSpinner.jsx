export default function LoadingSpinner({ label = 'Loading...' }) {
  return (
    <div className="d-flex flex-column align-items-center justify-content-center py-5">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">{label}</span>
      </div>
      <p className="mt-2 text-muted">{label}</p>
    </div>
  );
}
