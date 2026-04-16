ALTER TABLE app.point_transactions
    ADD COLUMN IF NOT EXISTS campaign_id BIGINT;

ALTER TABLE app.point_transactions
    ADD CONSTRAINT fk_point_transactions_campaign
        FOREIGN KEY (campaign_id)
            REFERENCES app.machine_campaigns(id);

CREATE INDEX IF NOT EXISTS idx_point_transactions_campaign
    ON app.point_transactions(campaign_id);

UPDATE app.point_transactions pt
SET campaign_id = (
    SELECT MAX(mc.id)
    FROM app.machine_campaigns mc
    WHERE pt.transaction_type = 'EARN'
      AND mc.opened_at IS NOT NULL
      AND pt.created_at >= mc.opened_at
      AND (
        (mc.closed_at IS NOT NULL AND pt.created_at < DATEADD('SECOND', 1, mc.closed_at))
        OR (mc.closed_at IS NULL AND mc.status = 'OPEN')
      )
)
WHERE pt.campaign_id IS NULL
  AND pt.transaction_type = 'EARN';
