ALTER TABLE users
    ADD COLUMN IF NOT EXISTS encrypted_data_key TEXT;
