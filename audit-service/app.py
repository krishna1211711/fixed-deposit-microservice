import json
import os
import threading
import time
from datetime import datetime, timezone

import mysql.connector
from flask import Flask, jsonify, request
from kafka import KafkaConsumer

app = Flask(__name__)
state = {"consumerThreadAlive": False, "connected": False, "lastError": None,
         "processed": 0, "invalid": 0}


def utc_now():
    return datetime.now(timezone.utc).replace(tzinfo=None)


def db_connection():
    return mysql.connector.connect(
        host=os.getenv("AUDIT_DB_HOST", "audit-mysql"),
        port=int(os.getenv("AUDIT_DB_PORT", "3306")),
        user=os.getenv("AUDIT_DB_USER", "audit_user"),
        password=os.getenv("AUDIT_DB_PASSWORD", "audit_demo_password"),
        database=os.getenv("AUDIT_DB_NAME", "audit_db"),
    )


def ensure_schema():
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS audit_inbox (
              event_id VARCHAR(36) PRIMARY KEY,
              received_at TIMESTAMP(6) NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS distributed_audit_events (
              event_id VARCHAR(36) PRIMARY KEY,
              event_type VARCHAR(60) NOT NULL,
              schema_version VARCHAR(20) NOT NULL,
              aggregate_type VARCHAR(40) NOT NULL,
              aggregate_id VARCHAR(80) NOT NULL,
              business_date DATE NULL,
              correlation_id VARCHAR(100) NULL,
              causation_id VARCHAR(100) NULL,
              occurred_at VARCHAR(50) NOT NULL,
              producer VARCHAR(80) NOT NULL,
              payload JSON NOT NULL,
              recorded_at TIMESTAMP(6) NOT NULL,
              INDEX idx_audit_aggregate (aggregate_type, aggregate_id),
              INDEX idx_audit_correlation (correlation_id),
              INDEX idx_audit_type_date (event_type, business_date)
            )
        """)
        connection.commit()
    finally:
        connection.close()


def process_event(event):
    required = {"eventId", "eventType"}
    missing = sorted(required.difference(event))
    if missing:
        raise ValueError(f"Missing audit event fields: {', '.join(missing)}")
    # The topic existed before the versioned envelope. Preserve those records as
    # explicitly legacy audit evidence while all newly emitted events use 1.0.
    event.setdefault("schemaVersion", "legacy-0")
    event.setdefault("aggregateType", "FD_ACCOUNT" if event.get("fdAccountNo") else "UNKNOWN")
    event.setdefault("aggregateId", event.get("fdAccountNo") or event.get("requestId") or event["eventId"])
    event.setdefault("occurredAt", utc_now().isoformat() + "Z")
    business_date = event.get("businessDate")
    if isinstance(business_date, list) and len(business_date) == 3:
        business_date = f"{business_date[0]:04d}-{business_date[1]:02d}-{business_date[2]:02d}"

    def scalar(value):
        return json.dumps(value) if isinstance(value, (list, dict)) else value
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("SELECT 1 FROM audit_inbox WHERE event_id = %s", (event["eventId"],))
        if cursor.fetchone():
            return
        cursor.execute("""
            INSERT INTO distributed_audit_events
              (event_id, event_type, schema_version, aggregate_type, aggregate_id,
               business_date, correlation_id, causation_id, occurred_at, producer, payload, recorded_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
        """, (
            event["eventId"], event["eventType"], str(event["schemaVersion"]),
            scalar(event["aggregateType"]), scalar(event["aggregateId"]), business_date,
            scalar(event.get("correlationId")), scalar(event.get("causationId")), scalar(event["occurredAt"]),
            scalar(event.get("producer", "unknown")), json.dumps(event), utc_now(),
        ))
        cursor.execute("INSERT INTO audit_inbox (event_id, received_at) VALUES (%s, %s)",
                       (event["eventId"], utc_now()))
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def consume_forever():
    state["consumerThreadAlive"] = True
    while True:
        try:
            ensure_schema()
            consumer = KafkaConsumer(
                os.getenv("FD_EVENTS_TOPIC", "fd.lifecycle.v1"),
                bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092").split(","),
                group_id=os.getenv("KAFKA_CONSUMER_GROUP", "fd-audit-service-v1"),
                auto_offset_reset="earliest", enable_auto_commit=False,
                value_deserializer=lambda value: json.loads(value.decode("utf-8")),
            )
            state["connected"] = True
            state["lastError"] = None
            for message in consumer:
                try:
                    process_event(message.value)
                    state["processed"] += 1
                except Exception as error:
                    # An unparseable historic record must not poison the entire
                    # audit partition. Health exposes the count/error for review.
                    state["invalid"] += 1
                    state["lastError"] = str(error)
                finally:
                    consumer.commit()
        except Exception as error:
            state["connected"] = False
            state["lastError"] = str(error)
            time.sleep(5)


@app.get("/health")
def health():
    try:
        ensure_schema()
        database = "UP"
    except Exception as error:
        database = "DOWN"
        state["lastError"] = str(error)
    disabled = os.getenv("AUDIT_DISABLE_CONSUMER", "false").lower() == "true"
    status = "UP" if database == "UP" and (state["connected"] or disabled) else "DOWN"
    return jsonify({"status": status, "database": database, **state}), 200 if status == "UP" else 503


@app.get("/events")
def recent_events():
    if request.headers.get("X-User-Role", "").removeprefix("ROLE_") not in {"ADMIN", "AUDITOR"}:
        return jsonify(error="ADMIN or AUDITOR role required"), 403
    connection = db_connection()
    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute("""
            SELECT event_id AS eventId, event_type AS eventType, aggregate_type AS aggregateType,
                   aggregate_id AS aggregateId, business_date AS businessDate,
                   correlation_id AS correlationId, occurred_at AS occurredAt, producer
              FROM distributed_audit_events ORDER BY recorded_at DESC LIMIT 200
        """)
        return jsonify(cursor.fetchall())
    finally:
        connection.close()


if os.getenv("AUDIT_DISABLE_CONSUMER", "false").lower() != "true":
    threading.Thread(target=consume_forever, name="kafka-audit-consumer", daemon=True).start()
