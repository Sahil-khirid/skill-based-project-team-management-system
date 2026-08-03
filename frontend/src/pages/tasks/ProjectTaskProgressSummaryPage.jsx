import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';

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

        <div className="card shadow-sm">
          <div className="card-body">
            <dl className="row mb-0">
              <dt className="col-sm-4">Project ID</dt>
              <dd className="col-sm-8">{summary.projectId}</dd>

              <dt className="col-sm-4">Total Tasks</dt>
              <dd className="col-sm-8">{summary.totalTasks}</dd>

              <dt className="col-sm-4">TODO Count</dt>
              <dd className="col-sm-8">{summary.todoCount}</dd>

              <dt className="col-sm-4">IN_PROGRESS Count</dt>
              <dd className="col-sm-8">{summary.inProgressCount}</dd>

              <dt className="col-sm-4">COMPLETED Count</dt>
              <dd className="col-sm-8">{summary.completedCount}</dd>

              <dt className="col-sm-4">BLOCKED Count</dt>
              <dd className="col-sm-8">{summary.blockedCount}</dd>

              <dt className="col-sm-4">Completion Percentage</dt>
              <dd className="col-sm-8">{summary.completionPercentage}%</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
