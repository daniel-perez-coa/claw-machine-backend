ALTER TABLE app.machine_campaigns
ADD COLUMN IF NOT EXISTS major_prize_alert_active BOOLEAN NOT NULL DEFAULT FALSE;
