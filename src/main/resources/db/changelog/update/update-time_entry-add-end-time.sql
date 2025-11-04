ALTER TABLE time_entry
    ADD COLUMN end_hour INTEGER,
    ADD COLUMN end_minute INTEGER;

UPDATE time_entry
SET end_hour = CASE WHEN hour = 23 THEN 23 ELSE hour + 1 END,
    end_minute = CASE WHEN hour = 23 THEN 59 ELSE minute END
WHERE end_hour IS NULL OR end_minute IS NULL;

ALTER TABLE time_entry
    ALTER COLUMN end_hour SET NOT NULL,
    ALTER COLUMN end_minute SET NOT NULL;

ALTER TABLE time_entry
    ADD CONSTRAINT chk_time_entry_range
    CHECK (
        (end_hour > hour)
        OR (end_hour = hour AND end_minute > minute)
    );
