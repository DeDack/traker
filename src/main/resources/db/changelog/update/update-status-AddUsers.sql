ALTER TABLE status
    ADD COLUMN user_id BIGINT NOT NULL REFERENCES users(id);

UPDATE status
SET user_id = 1
WHERE user_id IS NULL;