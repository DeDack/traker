ALTER TABLE expense_record
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN amount TYPE TEXT USING amount::TEXT;

ALTER TABLE income_record
    ALTER COLUMN title TYPE TEXT,
    ALTER COLUMN description TYPE TEXT,
    ALTER COLUMN amount TYPE TEXT USING amount::TEXT;

ALTER TABLE budget
    ALTER COLUMN planned_income TYPE TEXT USING planned_income::TEXT,
    ALTER COLUMN planned_expense TYPE TEXT USING planned_expense::TEXT,
    ALTER COLUMN savings_goal TYPE TEXT USING savings_goal::TEXT,
    ALTER COLUMN notes TYPE TEXT;
