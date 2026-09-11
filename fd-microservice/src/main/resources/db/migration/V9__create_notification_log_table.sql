CREATE TABLE notification_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    customer_id VARCHAR(20),
    event_type VARCHAR(50),
    channel VARCHAR(20),
    message_body TEXT,
    status VARCHAR(20),
    sent_at TIMESTAMP
);
