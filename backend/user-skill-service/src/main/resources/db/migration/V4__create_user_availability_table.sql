CREATE TABLE user_availability (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_profile_id BIGINT NOT NULL,
    available_hours_per_week INT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_availability_user_profile_id UNIQUE (user_profile_id),
    CONSTRAINT fk_user_availability_user_profile FOREIGN KEY (user_profile_id) REFERENCES user_profiles (id),
    CONSTRAINT chk_user_availability_hours CHECK (available_hours_per_week BETWEEN 0 AND 168)
);
