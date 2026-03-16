ALTER TABLE app.prize_redemptions
    ADD COLUMN campaign_id BIGINT NOT NULL;

ALTER TABLE app.prize_redemptions
    ADD CONSTRAINT fk_prize_redemptions_campaign
        FOREIGN KEY (campaign_id) REFERENCES app.machine_campaigns(id);

CREATE INDEX idx_prize_redemptions_campaign
    ON app.prize_redemptions(campaign_id);