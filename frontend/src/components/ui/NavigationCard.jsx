import { Link } from 'react-router-dom';

export default function NavigationCard({ to, icon, title, description }) {
  return (
    <Link to={to} className="nav-card">
      {icon && (
        <span className="nav-card__icon" aria-hidden="true">
          {icon}
        </span>
      )}
      <span className="nav-card__title">{title}</span>
      {description && <span className="nav-card__description">{description}</span>}
    </Link>
  );
}
