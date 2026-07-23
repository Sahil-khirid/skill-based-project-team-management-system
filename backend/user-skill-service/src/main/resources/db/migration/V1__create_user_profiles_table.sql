CREATE TABLE user_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    auth_user_id BIGINT NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    headline VARCHAR(160),
    bio VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_profiles_auth_user_id UNIQUE (auth_user_id)
);
