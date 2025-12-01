CREATE SCHEMA IF NOT EXISTS vpn;

CREATE TABLE IF NOT EXISTS vpn.vpn_keys (
    id UUID PRIMARY KEY,
    owner_user_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    public_key TEXT NOT NULL,
    private_key_encrypted TEXT NOT NULL,
    address VARCHAR(32) NOT NULL,
    expiration_at TIMESTAMP,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS vpn.vpn_audit (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    action VARCHAR(32) NOT NULL,
    vpn_key_id UUID,
    timestamp TIMESTAMP NOT NULL,
    details TEXT
);
