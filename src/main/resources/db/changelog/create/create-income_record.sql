CREATE TABLE IF NOT EXISTS income_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    category_id BIGINT NOT NULL REFERENCES income_category (id),
    title TEXT NOT NULL,
    description TEXT,
    amount TEXT NOT NULL,
    period_start DATE NOT NULL,
    income_date DATE
);

CREATE INDEX IF NOT EXISTS idx_income_record_user_period
    ON income_record (user_id, period_start);

CREATE INDEX IF NOT EXISTS idx_income_record_user_date
    ON income_record (user_id, income_date);
