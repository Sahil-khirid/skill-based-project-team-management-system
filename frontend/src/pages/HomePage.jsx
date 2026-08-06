import { Link } from 'react-router-dom';
import { useAuth } from '../auth/useAuth';

const FEATURES = [
  {
    icon: '✓',
    title: 'Skill-Based Matching',
    description: 'Match project requirements against member skills and proficiency levels to build the right team.',
  },
  {
    icon: '▤',
    title: 'Project Team Formation',
    description: 'Assemble project teams, manage membership, and get member recommendations based on required skills.',
  },
  {
    icon: '↗',
    title: 'Task and Progress Tracking',
    description: 'Create tasks, assign them to team members, and track status and progress through to completion.',
  },
];

export default function HomePage() {
  const { isAuthenticated } = useAuth();

  return (
    <div>
      <section className="hero-section">
        <div className="hero-section__content">
          <span className="hero-section__eyebrow">Skill-Based Team Management</span>
          <h1 className="hero-section__title">Build the right project team, faster.</h1>
          <p className="hero-section__subtitle">
            Form project teams based on member skills, assign tasks, and track progress in one place.
          </p>
          <div className="d-flex flex-wrap gap-3">
            {isAuthenticated ? (
              <>
                <Link className="btn btn-light btn-lg" to="/dashboard">
                  Go to Dashboard
                </Link>
                <Link className="btn btn-outline-light btn-lg" to="/projects">
                  Explore Projects
                </Link>
              </>
            ) : (
              <>
                <Link className="btn btn-light btn-lg" to="/login">
                  Login
                </Link>
                <Link className="btn btn-outline-light btn-lg" to="/register">
                  Register
                </Link>
              </>
            )}
          </div>
        </div>
      </section>

      <div className="row g-4">
        {FEATURES.map((feature) => (
          <div className="col-12 col-md-4" key={feature.title}>
            <div className="feature-card">
              <span className="feature-card__icon" aria-hidden="true">
                {feature.icon}
              </span>
              <h2 className="h6 feature-card__title">{feature.title}</h2>
              <p className="feature-card__description">{feature.description}</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
