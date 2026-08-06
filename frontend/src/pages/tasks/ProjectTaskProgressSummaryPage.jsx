import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import MetricCard from '../../components/ui/MetricCard';

export default function ProjectTaskProgressSummaryPage() {
  const { projectId } = useParams();

  const [summary, setSummary] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const loadSummary = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await taskApi.getTaskProgressSummary(projectId);
      setSummary(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadSummary();
  }, [loadSummary]);

  if (initialLoading) {
    return <LoadingSpinner label="Loading project progress summary..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadSummary}>
            Retry
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="row justify-content-center">
      <div className="col-12 col-lg-8">
        <div className="d-flex justify-content-between align-items-center mb-4">
          <h1 className="h4 mb-0">Project Task Progress Summary</h1>
        </div>

        <div className="card shadow-sm mb-4">
          <div className="card-body">
            <div className="d-flex justify-content-between align-items-center mb-2">
              <span className="form-label mb-0">Overall completion</span>
              <span className="fw-bold">{summary.completionPercentage}%</span>
            </div>
            <div className="progress">
              <div
                className="progress-bar"
                role="progressbar"
                style={{ width: `${summary.completionPercentage}%` }}
                aria-valuenow={summary.completionPercentage}
                aria-valuemin="0"
                aria-valuemax="100"
              />
            </div>
          </div>
        </div>

        <div className="metric-card-grid mb-4">
          <MetricCard label="Total Tasks" value={summary.totalTasks} />
          <MetricCard label="To Do" value={summary.todoCount} />
          <MetricCard label="In Progress" value={summary.inProgressCount} />
          <MetricCard label="Completed" value={summary.completedCount} />
          <MetricCard label="Blocked" value={summary.blockedCount} />
        </div>

        <div className="card shadow-sm">
          <div className="card-body">
            <dl className="row mb-0">
              <dt className="col-sm-4">Project ID</dt>
              <dd className="col-sm-8">{summary.projectId}</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
