ALTER TABLE time_entry
    ADD COLUMN status_id BIGINT;

ALTER TABLE time_entry
    ADD CONSTRAINT fk_time_entry_status
        FOREIGN KEY (status_id) REFERENCES status(id);