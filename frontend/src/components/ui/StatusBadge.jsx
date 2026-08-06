const STATUS_VARIANTS = {
  PLANNING: 'status-badge--info',
  ACTIVE: 'status-badge--primary',
  ON_HOLD: 'status-badge--warning',
  COMPLETED: 'status-badge--success',
  CANCELLED: 'status-badge--danger',
  TODO: 'status-badge--secondary',
  IN_PROGRESS: 'status-badge--primary',
  BLOCKED: 'status-badge--danger',
  LOW: 'status-badge--secondary',
  MEDIUM: 'status-badge--warning',
  HIGH: 'status-badge--danger',
};

function humanize(value) {
  return value
    .toLowerCase()
    .split('_')
    .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
    .join(' ');
}

export default function StatusBadge({ status }) {
  if (!status) {
    return null;
  }

  const variant = STATUS_VARIANTS[status] || 'status-badge--secondary';

  return <span className={`status-badge ${variant}`}>{humanize(status)}</span>;
}
