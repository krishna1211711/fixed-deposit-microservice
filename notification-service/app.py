import json
import os
import smtplib
import threading
import time
from datetime import datetime, timezone
from email.message import EmailMessage

import mysql.connector
from flask import Flask, jsonify
from kafka import KafkaConsumer

app = Flask(__name__)
state = {"consumerThreadAlive": False, "connected": False, "lastError": None, "processed": 0}


def db_connection():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "mysql"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER", "fd_user"),
        password=os.getenv("DB_PASSWORD", "fd_demo_password"),
        database=os.getenv("DB_NAME", "fd_bank_db"),
    )


def already_processed(event_id):
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("SELECT 1 FROM notification_log WHERE event_id = %s LIMIT 1", (event_id,))
        return cursor.fetchone() is not None
    finally:
        connection.close()


def record(event, status):
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute(
            """
            INSERT INTO notification_log
              (event_id, customer_id, event_type, channel, message_body, status, sent_at)
            VALUES (%s, %s, %s, 'EMAIL', %s, %s, %s)
            """,
            (
                event["eventId"], event["customerId"], event["eventType"],
                event["messageBody"], status,
                datetime.now(timezone.utc).replace(tzinfo=None) if status == "SENT" else None,
            ),
        )
        connection.commit()
    finally:
        connection.close()


def send_email(event):
    message = EmailMessage()
    message["From"] = os.getenv("SMTP_FROM", "noreply@fd-demo.local")
    message["To"] = f'{event["customerId"]}@fd-demo.local'
    message["Subject"] = event["subject"]
    message.set_content(event["messageBody"])
    with smtplib.SMTP(os.getenv("SMTP_HOST", "mailpit"), int(os.getenv("SMTP_PORT", "1025")), timeout=10) as smtp:
        smtp.send_message(message)


def consume_forever():
    state["consumerThreadAlive"] = True
    topic = os.getenv("FD_EVENTS_TOPIC", "fd.lifecycle.v1")
    while True:
        try:
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
                event = message.value
                try:
                    if not already_processed(event["eventId"]):
                        try:
                            send_email(event)
                            record(event, "SENT")
                        except Exception:
                            record(event, "FAILED")
                            raise
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


threading.Thread(target=consume_forever, name="kafka-notification-consumer", daemon=True).start()
