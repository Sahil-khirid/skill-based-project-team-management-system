import { useCallback, useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { extractErrorMessage } from '../../api/errorMessage';
import * as taskApi from '../../api/taskApi';
import LoadingSpinner from '../../components/LoadingSpinner';

export default function TaskDetailsPage() {
  const { id } = useParams();

  const [task, setTask] = useState(null);
  const [initialLoading, setInitialLoading] = useState(true);
  const [loadError, setLoadError] = useState('');

  const loadTask = useCallback(async () => {
    setInitialLoading(true);
    setLoadError('');
    try {
      const data = await taskApi.getTask(id);
      setTask(data);
    } catch (error) {
      setLoadError(extractErrorMessage(error));
    } finally {
      setInitialLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadTask();
  }, [loadTask]);

  if (initialLoading) {
    return <LoadingSpinner label="Loading task..." />;
  }

  if (loadError) {
    return (
      <div className="row justify-content-center">
        <div className="col-12 col-lg-8">
          <div className="alert alert-danger" role="alert">
            {loadError}
          </div>
          <button type="button" className="btn btn-primary" onClick={loadTask}>
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
          <h1 className="h4 mb-0">Task Details</h1>
        </div>

        <div className="card shadow-sm">
          <div className="card-body">
            <h2 className="h5 mb-3">{task.title}</h2>

            <dl className="row mb-0">
              <dt className="col-sm-4">Description</dt>
              <dd className="col-sm-8">{task.description || 'Not provided'}</dd>

              <dt className="col-sm-4">Status</dt>
              <dd className="col-sm-8">{task.status}</dd>

              <dt className="col-sm-4">Priority</dt>
              <dd className="col-sm-8">{task.priority}</dd>

              <dt className="col-sm-4">Progress Percentage</dt>
              <dd className="col-sm-8">{task.progressPercentage}%</dd>

              <dt className="col-sm-4">Due Date</dt>
              <dd className="col-sm-8">{task.dueDate || 'Not set'}</dd>

              <dt className="col-sm-4">Project ID</dt>
              <dd className="col-sm-8">{task.projectId}</dd>

              <dt className="col-sm-4">Assigned Auth User ID</dt>
              <dd className="col-sm-8">{task.assignedAuthUserId == null ? 'Unassigned' : task.assignedAuthUserId}</dd>

              <dt className="col-sm-4">Created At</dt>
              <dd className="col-sm-8">{task.createdAt}</dd>

              <dt className="col-sm-4">Updated At</dt>
              <dd className="col-sm-8">{task.updatedAt}</dd>
            </dl>
          </div>
        </div>
      </div>
    </div>
  );
}
