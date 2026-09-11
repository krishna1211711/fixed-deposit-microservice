ALTER TABLE notification_log
    ADD COLUMN event_id VARCHAR(36) NULL;

CREATE UNIQUE INDEX uk_notification_log_event_id
    ON notification_log (event_id);
