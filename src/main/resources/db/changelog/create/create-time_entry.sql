CREATE TABLE time_entry (
                            id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                             day_log_id BIGINT REFERENCES day_log(id),
                             hour INTEGER CHECK (hour >= 0 AND hour <= 23),
                             minute INTEGER DEFAULT 0 NOT NULL,
                             end_hour INTEGER NOT NULL,
                             end_minute INTEGER NOT NULL,
      worked BOOLEAN,
      comment TEXT,
      CONSTRAINT chk_time_entry_range CHECK (
          (end_hour > hour)
          OR (end_hour = hour AND end_minute > minute)
      )
  );