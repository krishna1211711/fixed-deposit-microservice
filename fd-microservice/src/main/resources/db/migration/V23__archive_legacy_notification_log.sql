-- Notification delivery is now owned by notification-service and notification_db.
-- Preserve historic demo evidence losslessly while removing the active cross-service table name.
RENAME TABLE notification_log TO legacy_notification_log_archive;
