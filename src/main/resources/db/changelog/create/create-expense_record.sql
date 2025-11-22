CREATE TABLE IF NOT EXISTS expense_record (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    category_id BIGINT NOT NULL REFERENCES expense_category (id),
    title TEXT NOT NULL,
    description TEXT,
    amount TEXT NOT NULL,
    period_start DATE NOT NULL,
    expense_date DATE
);

CREATE INDEX IF NOT EXISTS idx_expense_record_user_period
    ON expense_record (user_id, period_start);

CREATE INDEX IF NOT EXISTS idx_expense_record_user_date
    ON expense_record (user_id, expense_date);
