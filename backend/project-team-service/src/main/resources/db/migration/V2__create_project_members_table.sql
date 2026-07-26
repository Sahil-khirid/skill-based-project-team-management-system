CREATE TABLE project_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    auth_user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_project_members_project_auth_user UNIQUE (project_id, auth_user_id),
    CONSTRAINT fk_project_members_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_project_members_role CHECK (role IN ('MEMBER', 'LEADER'))
);

CREATE INDEX idx_project_members_project_id ON project_members (project_id);
