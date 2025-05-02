CREATE TABLE time_entry (
                            id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                            day_log_id BIGINT REFERENCES day_log(id),
                            hour INTEGER CHECK (hour >= 0 AND hour <= 23),
    worked BOOLEAN,
    comment TEXT
);