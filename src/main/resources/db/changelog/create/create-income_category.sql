CREATE TABLE IF NOT EXISTS income_category (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    user_id BIGINT NOT NULL REFERENCES users (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_income_category_user_name
    ON income_category (user_id, lower(name));
