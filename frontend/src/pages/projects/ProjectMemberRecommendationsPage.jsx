import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as projectApi from '../../api/projectApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import EmptyState from '../../components/ui/EmptyState';

const PROFICIENCY_LABELS = {
  BEGINNER: 'Beginner',
  INTERMEDIATE: 'Intermediate',
  ADVANCED: 'Advanced',
  EXPERT: 'Expert',
};

function proficiencyLabel(level) {
  return PROFICIENCY_LABELS[level] || level;
}

export default function ProjectMemberRecommendationsPage() {
  const { projectId } = useParams();

  const [recommendations, setRecommendations] = useState([]);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const loadRecommendations = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await projectApi.getProjectMemberRecommendations(projectId);
      setRecommendations(data.recommendations || []);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadRecommendations();
  }, [loadRecommendations]);

  if (initialLoading) {
    return <LoadingSpinner label="Loading member recommendations..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadRecommendations}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-10">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="h4 mb-0">Member Recommendations</h1>
          <Link className="btn btn-outline-secondary" to={`/projects/${projectId}`}>
            Back to Project
          </Link>
        </div>

        {recommendations.length === 0 ? (
          <EmptyState
            title="No suitable members found"
            description="No eligible users currently match this project's requirements."
            suggestions={['Review required skills', 'Check member availability', 'Confirm user profiles and skills']}
          />
        ) : (
          <div className="d-flex flex-column gap-3">
            {recommendations.map((recommendation) => (
              <div className="card shadow-sm" key={recommendation.authUserId}>
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-center mb-3">
                    <h2 className="h6 mb-0">User #{recommendation.authUserId}</h2>
                    <span className="badge text-bg-primary fs-6">{recommendation.matchPercentage}% match</span>
                  </div>

                  <p className="text-muted mb-3">
                    {recommendation.matchedSkillCount} of {recommendation.requiredSkillCount} required skills matched
                    {recommendation.missingSkillCount > 0 && ` (${recommendation.missingSkillCount} missing)`}
                  </p>

                  <div className="mb-3">
                    <h3 className="h6">Matched Skills</h3>
                    {recommendation.matchedSkills.length === 0 ? (
                      <p className="text-muted small mb-0">None</p>
                    ) : (
                      <div className="d-flex flex-wrap gap-2">
                        {recommendation.matchedSkills.map((skillMatch) => (
                          <span className="badge text-bg-success" key={skillMatch.skillId}>
                            {skillMatch.skillName} ({proficiencyLabel(skillMatch.memberLevel)} ≥{' '}
                            {proficiencyLabel(skillMatch.requiredLevel)} required)
                          </span>
                        ))}
                      </div>
                    )}
                  </div>

                  <div>
                    <h3 className="h6">Missing Skills</h3>
                    {recommendation.missingSkillIds.length === 0 ? (
                      <p className="text-muted small mb-0">None</p>
                    ) : (
                      <div className="d-flex flex-wrap gap-2">
                        {recommendation.missingSkillIds.map((skillId) => (
                          <span className="badge text-bg-secondary" key={skillId}>
                            Skill #{skillId}
                          </span>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
