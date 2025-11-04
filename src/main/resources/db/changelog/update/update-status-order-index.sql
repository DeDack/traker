ALTER TABLE status
    ADD COLUMN IF NOT EXISTS order_index INT DEFAULT 0 NOT NULL;

UPDATE status
SET order_index = sub.rn - 1
FROM (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS rn
    FROM status
) AS sub
WHERE status.id = sub.id
  AND status.order_index = 0;
