import { useEffect, useState } from 'react';
import { useAuth } from '../auth/useAuth';
import RoleBadge from '../components/ui/RoleBadge';
import MetricCard from '../components/ui/MetricCard';
import NavigationCard from '../components/ui/NavigationCard';
import * as userSkillApi from '../api/userSkillApi';
import * as projectApi from '../api/projectApi';
import * as taskApi from '../api/taskApi';

const COMMON_NAV_CARDS = [
  { to: '/profile', icon: '◑', title: 'Manage Profile', description: 'View and edit your profile details.' },
  { to: '/my-skills', icon: '✦', title: 'Manage My Skills', description: 'Add skills and update proficiency.' },
  { to: '/availability', icon: '⏱', title: 'Update Availability', description: 'Set your weekly available hours.' },
  { to: '/projects', icon: '▤', title: 'View Projects', description: 'Browse projects and their details.' },
  { to: '/tasks', icon: '☑', title: 'View Tasks', description: 'See tasks and update their progress.' },
];

const MANAGER_NAV_CARDS = [
  { to: '/projects/create', icon: '+', title: 'Create Project', description: 'Start a new project for your team.' },
  { to: '/tasks/create', icon: '+', title: 'Create Task', description: 'Add a task to an existing project.' },
];

export default function DashboardPage() {
  const { user, hasRole } = useAuth();
  const isManager = hasRole('PROJECT_MANAGER');

  const [metrics, setMetrics] = useState({ skills: null, projects: null, tasks: null });

  useEffect(() => {
    let cancelled = false;

    userSkillApi
      .getMySkills()
      .then((data) => {
        if (!cancelled) {
          setMetrics((prev) => ({ ...prev, skills: data.length }));
        }
      })
      .catch(() => {});

    projectApi
      .listProjects()
      .then((data) => {
        if (!cancelled) {
          setMetrics((prev) => ({ ...prev, projects: data.length }));
        }
      })
      .catch(() => {});

    taskApi
      .listTasks()
      .then((data) => {
        if (!cancelled) {
          setMetrics((prev) => ({ ...prev, tasks: data.length }));
        }
      })
      .catch(() => {});

    return () => {
      cancelled = true;
    };
  }, []);

  const navCards = isManager ? [...COMMON_NAV_CARDS, ...MANAGER_NAV_CARDS] : COMMON_NAV_CARDS;

  return (
    <div className="py-1">
      <div className="d-flex flex-wrap justify-content-between align-items-center gap-2 mb-4">
        <div>
          <h1 className="h4 mb-1">Welcome back{user?.email ? `, ${user.email}` : ''}</h1>
          <p className="text-muted mb-0">Here&apos;s a quick overview of your workspace.</p>
        </div>
        <RoleBadge role={user?.role} />
      </div>

      <div className="metric-card-grid mb-4">
        {metrics.skills !== null && <MetricCard icon="✦" label="My Skills" value={metrics.skills} />}
        {metrics.projects !== null && <MetricCard icon="▤" label="Projects" value={metrics.projects} />}
        {metrics.tasks !== null && <MetricCard icon="☑" label="Tasks" value={metrics.tasks} />}
      </div>

      <h2 className="h6 text-muted text-uppercase mb-3">Quick actions</h2>
      <div className="nav-card-grid">
        {navCards.map((card) => (
          <NavigationCard key={card.to} {...card} />
        ))}
      </div>
    </div>
  );
}
