export default function MetricCard({ label, value, icon }) {
  return (
    <div className="metric-card">
      {icon && (
        <span className="metric-card__icon" aria-hidden="true">
          {icon}
        </span>
      )}
      <span className="metric-card__value">{value}</span>
      <span className="metric-card__label">{label}</span>
    </div>
  );
}
