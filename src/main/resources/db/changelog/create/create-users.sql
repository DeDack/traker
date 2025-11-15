CREATE TABLE users (
                       id BIGINT PRIMARY KEY GENERATED ALWAYS AS IDENTITY,
                       username VARCHAR(255) NOT NULL UNIQUE,
                       name VARCHAR(255),
                       created_at TIMESTAMP,
                       updated_at TIMESTAMP,
                       password VARCHAR(255) NOT NULL DEFAULT '',
                       encrypted_data_key TEXT
);