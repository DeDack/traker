CREATE TABLE IF NOT EXISTS budget (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    period_start DATE NOT NULL,
    planned_income NUMERIC(19, 2) DEFAULT 0,
    planned_expense NUMERIC(19, 2) DEFAULT 0,
    savings_goal NUMERIC(19, 2) DEFAULT 0,
    notes VARCHAR(1000),
    CONSTRAINT uk_budget_user_period UNIQUE (user_id, period_start)
);

CREATE INDEX IF NOT EXISTS idx_budget_user_period
    ON budget (user_id, period_start);
