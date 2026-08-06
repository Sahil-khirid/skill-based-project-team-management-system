const ROLE_LABELS = {
  PROJECT_MANAGER: 'Project Manager',
  USER: 'User',
  LEADER: 'Leader',
  MEMBER: 'Member',
};

const ROLE_VARIANTS = {
  PROJECT_MANAGER: 'role-badge--manager',
  LEADER: 'role-badge--manager',
  USER: 'role-badge--user',
  MEMBER: 'role-badge--user',
};

export default function RoleBadge({ role }) {
  if (!role) {
    return null;
  }

  const variant = ROLE_VARIANTS[role] || 'role-badge--user';

  return <span className={`role-badge ${variant}`}>{ROLE_LABELS[role] || role}</span>;
}
