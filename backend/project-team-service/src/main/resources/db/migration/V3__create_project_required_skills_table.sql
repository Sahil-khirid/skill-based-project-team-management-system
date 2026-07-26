CREATE TABLE project_required_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    proficiency_level VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_project_required_skills_project_skill UNIQUE (project_id, skill_id),
    CONSTRAINT fk_project_required_skills_project FOREIGN KEY (project_id) REFERENCES projects (id),
    CONSTRAINT chk_project_required_skills_proficiency_level CHECK (proficiency_level IN ('BEGINNER', 'INTERMEDIATE', 'ADVANCED', 'EXPERT'))
);

CREATE INDEX idx_project_required_skills_project_id ON project_required_skills (project_id);
