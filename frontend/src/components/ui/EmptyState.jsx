export default function EmptyState({ title, description, suggestions, action }) {
  return (
    <div className="card shadow-sm empty-state">
      <div className="card-body text-center py-5">
        <h2 className="h5 empty-state__title">{title}</h2>
        {description && <p className="text-muted empty-state__description mb-3">{description}</p>}
        {suggestions && suggestions.length > 0 && (
          <ul className="empty-state__suggestions small mb-3">
            {suggestions.map((item) => (
              <li key={item}>{item}</li>
            ))}
          </ul>
        )}
        {action}
      </div>
    </div>
  );
}
