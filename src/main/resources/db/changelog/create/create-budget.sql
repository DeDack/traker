CREATE TABLE IF NOT EXISTS budget (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users (id),
    period_start DATE NOT NULL,
    planned_income TEXT,
    planned_expense TEXT,
    savings_goal TEXT,
    notes TEXT,
    CONSTRAINT uk_budget_user_period UNIQUE (user_id, period_start)
);

CREATE INDEX IF NOT EXISTS idx_budget_user_period
    ON budget (user_id, period_start);
