CREATE TABLE day_log (
                         id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                         date DATE UNIQUE NOT NULL
);