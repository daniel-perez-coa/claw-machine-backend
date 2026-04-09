ALTER TABLE app.machine_expense_records
    ADD COLUMN operation_group_id VARCHAR(64);

UPDATE app.machine_expense_records
SET operation_group_id = CONCAT(
        'legacy-',
        CAST(campaign_id AS VARCHAR),
        '-',
        FORMATDATETIME(registered_at, 'yyyyMMddHHmmss')
    )
WHERE operation_group_id IS NULL;

ALTER TABLE app.machine_expense_records
    ALTER COLUMN operation_group_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_machine_expense_records_operation_group
    ON app.machine_expense_records(operation_group_id);
