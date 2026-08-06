const PROFICIENCY_LABELS = {
  BEGINNER: 'Beginner',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
  EXPERT: 'Expert',
};

const PROFICIENCY_VARIANTS = {
  BEGINNER: 'proficiency-badge--level-1',
  INTERMEDIATE: 'proficiency-badge--level-2',
  ADVANCED: 'proficiency-badge--level-3',
  EXPERT: 'proficiency-badge--level-4',
};

export default function ProficiencyBadge({ level }) {
  if (!level) {
    return null;
  }

  const variant = PROFICIENCY_VARIANTS[level] || 'proficiency-badge--level-1';

  return <span className={`proficiency-badge ${variant}`}>{PROFICIENCY_LABELS[level] || level}</span>;
}
