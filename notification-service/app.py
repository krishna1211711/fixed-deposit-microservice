import json
import os
import smtplib
import threading
import time
from datetime import datetime, timezone
from email.message import EmailMessage

import mysql.connector
from flask import Flask, jsonify
from kafka import KafkaConsumer, KafkaProducer

app = Flask(__name__)
state = {
    "consumerThreadAlive": False,
    "connected": False,
    "lastError": None,
    "processed": 0,
    "deadLettered": 0,
}


def utc_now():
    return datetime.now(timezone.utc).replace(tzinfo=None)


def db_connection():
    return mysql.connector.connect(
        host=os.getenv("NOTIFICATION_DB_HOST", "notification-mysql"),
        port=int(os.getenv("NOTIFICATION_DB_PORT", "3306")),
        user=os.getenv("NOTIFICATION_DB_USER", "notification_user"),
        password=os.getenv("NOTIFICATION_DB_PASSWORD", "notification_demo_password"),
        database=os.getenv("NOTIFICATION_DB_NAME", "notification_db"),
    )


def ensure_schema():
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS notification_inbox (
              event_id VARCHAR(36) PRIMARY KEY,
              received_at TIMESTAMP(6) NOT NULL
            )
            """
        )
        cursor.execute(
            """
            CREATE TABLE IF NOT EXISTS notification_deliveries (
              event_id VARCHAR(36) PRIMARY KEY,
              customer_id VARCHAR(80) NOT NULL,
              event_type VARCHAR(60) NOT NULL,
              channel VARCHAR(20) NOT NULL,
              message_body TEXT NOT NULL,
              status VARCHAR(20) NOT NULL,
              attempt_count INT NOT NULL DEFAULT 0,
              last_error VARCHAR(1000),
              sent_at TIMESTAMP(6),
              updated_at TIMESTAMP(6) NOT NULL
            )
            """
        )
        connection.commit()
    finally:
        connection.close()


def already_consumed(event_id):
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("SELECT 1 FROM notification_inbox WHERE event_id = %s", (event_id,))
        return cursor.fetchone() is not None
    finally:
        connection.close()


def record_attempt(event, status, attempt_count, error=None, mark_consumed=False):
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute(
            """
            INSERT INTO notification_deliveries
              (event_id, customer_id, event_type, channel, message_body, status,
               attempt_count, last_error, sent_at, updated_at)
            VALUES (%s, %s, %s, 'EMAIL', %s, %s, %s, %s, %s, %s)
            ON DUPLICATE KEY UPDATE
              status = VALUES(status),
              attempt_count = VALUES(attempt_count),
              last_error = VALUES(last_error),
              sent_at = VALUES(sent_at),
              updated_at = VALUES(updated_at)
            """,
            (
                event["eventId"], event["customerId"], event["eventType"],
                event["messageBody"], status, attempt_count,
                str(error)[:1000] if error else None,
                utc_now() if status == "SENT" else None,
                utc_now(),
            ),
        )
        if mark_consumed:
            cursor.execute(
                "INSERT IGNORE INTO notification_inbox (event_id, received_at) VALUES (%s, %s)",
                (event["eventId"], utc_now()),
            )
        connection.commit()
    finally:
        connection.close()


def send_email(event):
    message = EmailMessage()
    message["From"] = os.getenv("SMTP_FROM", "noreply@fd-demo.local")
    message["To"] = f'{event["customerId"]}@fd-demo.local'
    message["Subject"] = event["subject"]
    message["X-Event-Id"] = event["eventId"]
    message.set_content(event["messageBody"])
    with smtplib.SMTP(
        os.getenv("SMTP_HOST", "mailpit"),
        int(os.getenv("SMTP_PORT", "1025")),
        timeout=10,
    ) as smtp:
        smtp.send_message(message)


def validate_event(event):
    required = {"eventId", "eventType", "customerId", "subject", "messageBody"}
    missing = sorted(required.difference(event))
    if missing:
        raise ValueError(f"Missing required event fields: {', '.join(missing)}")


def publish_dead_letter(producer, source_topic, event, error):
    envelope = {
        "sourceTopic": source_topic,
        "failedAt": datetime.now(timezone.utc).isoformat(),
        "error": str(error)[:1000],
        "event": event,
    }
    producer.send(
        os.getenv("FD_EVENTS_DLQ_TOPIC", f"{source_topic}.dlq"),
        key=str(event.get("aggregateId", event.get("eventId"))).encode("utf-8"),
        value=json.dumps(envelope).encode("utf-8"),
    ).get(timeout=10)


def process_event(event, producer, topic):
    validate_event(event)
    if already_consumed(event["eventId"]):
        return

    max_attempts = int(os.getenv("NOTIFICATION_MAX_ATTEMPTS", "3"))
    last_error = None
    for attempt in range(1, max_attempts + 1):
        try:
            send_email(event)
            record_attempt(event, "SENT", attempt, mark_consumed=True)
            return
        except Exception as error:
            last_error = error
            status = "RETRYING" if attempt < max_attempts else "FAILED"
            record_attempt(event, status, attempt, error)
            if attempt < max_attempts:
                time.sleep(min(8, 2 ** (attempt - 1)))

    publish_dead_letter(producer, topic, event, last_error)
    record_attempt(event, "DEAD_LETTER", max_attempts, last_error, mark_consumed=True)
    state["deadLettered"] += 1


def consume_forever():
    state["consumerThreadAlive"] = True
    topic = os.getenv("FD_EVENTS_TOPIC", "fd.lifecycle.v1")
    while True:
        try:
            ensure_schema()
            producer = KafkaProducer(
                bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092").split(","),
                acks="all",
            )
            consumer = KafkaConsumer(
                topic,
                bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092").split(","),
                group_id=os.getenv("KAFKA_CONSUMER_GROUP", "fd-notification-service-v1"),
                auto_offset_reset="earliest",
                enable_auto_commit=False,
                value_deserializer=lambda value: json.loads(value.decode("utf-8")),
            )
            state["connected"] = True
            state["lastError"] = None
            for message in consumer:
                try:
                    process_event(message.value, producer, topic)
                    consumer.commit()
                    state["processed"] += 1
                    state["lastError"] = None
                except Exception as error:
                    state["lastError"] = str(error)
                    time.sleep(2)
        except Exception as error:
            state["connected"] = False
            state["lastError"] = str(error)
            time.sleep(5)


@app.get("/health")
def health():
    status = "UP" if state["consumerThreadAlive"] and state["connected"] else "DOWN"
    return jsonify({"status": status, **state}), 200 if status == "UP" else 503


def start_consumer():
    threading.Thread(
        target=consume_forever,
        name="kafka-notification-consumer",
        daemon=True,
    ).start()


if os.getenv("NOTIFICATION_DISABLE_CONSUMER", "false").lower() != "true":
    start_consumer()
