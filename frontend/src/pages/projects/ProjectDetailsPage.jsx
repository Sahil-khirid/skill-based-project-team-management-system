import { useCallback, useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as projectApi from '../../api/projectApi';
import LoadingSpinner from '../../components/LoadingSpinner';
import { formatDateTime } from '../../utils/formatDate';

export default function ProjectDetailsPage() {
  const { projectId } = useParams();

  const [project, setProject] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const loadProject = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await projectApi.getProject(projectId);
      setProject(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [projectId]);

  useEffect(() => {
    loadProject();
  }, [loadProject]);

  if (initialLoading) {
    return <LoadingSpinner label="Loading project..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadProject}>
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
          <h1 className="h4 mb-0">Project Details</h1>
          <Link className="btn btn-outline-secondary" to="/projects">
            Back to Projects
          </Link>
        </div>

        <div className="card shadow-sm">
          <div className="card-body">
            <h2 className="h5 mb-3">{project.name}</h2>

            <dl className="row mb-0">
              <dt className="col-sm-4">Description</dt>
              <dd className="col-sm-8">{project.description || 'Not provided'}</dd>

              <dt className="col-sm-4">Status</dt>
              <dd className="col-sm-8">{project.status}</dd>

              <dt className="col-sm-4">Created At</dt>
              <dd className="col-sm-8">{formatDateTime(project.createdAt)}</dd>

              <dt className="col-sm-4">Updated At</dt>
              <dd className="col-sm-8">{formatDateTime(project.updatedAt)}</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
