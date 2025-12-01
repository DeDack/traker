ALTER TABLE vpn.vpn_keys
    ADD COLUMN IF NOT EXISTS protocol varchar(32) NOT NULL DEFAULT 'VLESS_REALITY',
    ADD COLUMN IF NOT EXISTS client_uuid uuid NOT NULL DEFAULT '00000000-0000-0000-0000-000000000000',
    ADD COLUMN IF NOT EXISTS reality_short_id varchar(32) NOT NULL DEFAULT 'pending',
    ADD COLUMN IF NOT EXISTS reality_sni varchar(255) NOT NULL DEFAULT 'www.cloudflare.com',
    ADD COLUMN IF NOT EXISTS reality_dest varchar(255) NOT NULL DEFAULT 'www.cloudflare.com:443',
    ADD COLUMN IF NOT EXISTS flow varchar(32) NOT NULL DEFAULT 'xtls-rprx-vision',
    ADD COLUMN IF NOT EXISTS meta jsonb DEFAULT '{}'::jsonb;
