ALTER TABLE tasks ADD COLUMN assigned_auth_user_id BIGINT NULL;
ALTER TABLE tasks ADD COLUMN progress_percentage INT NOT NULL DEFAULT 0;

CREATE INDEX idx_tasks_assigned_auth_user_id ON tasks (assigned_auth_user_id);
