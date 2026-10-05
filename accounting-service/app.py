import json
import os
import threading
import time
from datetime import datetime, timezone

import mysql.connector
from flask import Flask, jsonify, request
from kafka import KafkaConsumer

app = Flask(__name__)
state = {"consumerThreadAlive": False, "connected": False, "lastError": None, "posted": 0}


def utc_now():
    return datetime.now(timezone.utc).replace(tzinfo=None)


def db_connection():
    return mysql.connector.connect(
        host=os.getenv("ACCOUNTING_DB_HOST", "accounting-mysql"),
        port=int(os.getenv("ACCOUNTING_DB_PORT", "3306")),
        user=os.getenv("ACCOUNTING_DB_USER", "accounting_user"),
        password=os.getenv("ACCOUNTING_DB_PASSWORD", "accounting_demo_password"),
        database=os.getenv("ACCOUNTING_DB_NAME", "accounting_db"),
    )


def ensure_schema():
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS accounting_inbox (
              event_id VARCHAR(36) PRIMARY KEY,
              received_at TIMESTAMP(6) NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS journal_entries (
              journal_id VARCHAR(36) PRIMARY KEY,
              event_id VARCHAR(36) NOT NULL,
              transaction_reference VARCHAR(255) NOT NULL,
              fd_account_no VARCHAR(20) NOT NULL,
              business_date DATE NOT NULL,
              currency VARCHAR(3) NOT NULL,
              amount DECIMAL(18,3) NOT NULL,
              debit_gl_account VARCHAR(100) NOT NULL,
              credit_gl_account VARCHAR(100) NOT NULL,
              status VARCHAR(20) NOT NULL,
              posted_at TIMESTAMP(6) NOT NULL,
              UNIQUE KEY uk_accounting_event (event_id),
              UNIQUE KEY uk_accounting_reference (transaction_reference),
              CONSTRAINT chk_journal_amount_positive CHECK (amount > 0)
            )
        """)
        connection.commit()
    finally:
        connection.close()


def process_event(event):
    if event.get("eventType") != "FD_TRANSACTION_RECORDED":
        return
    required = {"eventId", "aggregateId", "transactionReference", "fdAccountNo", "businessDate",
                "currency", "amount", "debitGlAccount", "creditGlAccount"}
    missing = sorted(required.difference(event))
    if missing:
        raise ValueError(f"Missing accounting fields: {', '.join(missing)}")
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("SELECT 1 FROM accounting_inbox WHERE event_id = %s", (event["eventId"],))
        if cursor.fetchone():
            return
        cursor.execute("""
            INSERT INTO journal_entries
              (journal_id, event_id, transaction_reference, fd_account_no, business_date,
               currency, amount, debit_gl_account, credit_gl_account, status, posted_at)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, 'POSTED', %s)
        """, (
            event["aggregateId"], event["eventId"], event["transactionReference"],
            event["fdAccountNo"], event["businessDate"], event["currency"], event["amount"],
            event["debitGlAccount"], event["creditGlAccount"], utc_now(),
        ))
        cursor.execute("INSERT INTO accounting_inbox (event_id, received_at) VALUES (%s, %s)",
                       (event["eventId"], utc_now()))
        connection.commit()
        state["posted"] += 1
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
                group_id=os.getenv("KAFKA_CONSUMER_GROUP", "fd-accounting-service-v1"),
                auto_offset_reset="earliest", enable_auto_commit=False,
                value_deserializer=lambda value: json.loads(value.decode("utf-8")),
            )
            state["connected"] = True
            state["lastError"] = None
            for message in consumer:
                process_event(message.value)
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
    disabled = os.getenv("ACCOUNTING_DISABLE_CONSUMER", "false").lower() == "true"
    status = "UP" if database == "UP" and (state["connected"] or disabled) else "DOWN"
    return jsonify({"status": status, "database": database, **state}), 200 if status == "UP" else 503


@app.get("/entries")
def entries():
    if request.headers.get("X-User-Role", "").removeprefix("ROLE_") not in {"ADMIN", "AUDITOR"}:
        return jsonify({"message": "ADMIN or AUDITOR role required"}), 403
    limit = min(max(int(request.args.get("limit", "100")), 1), 500)
    connection = db_connection()
    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute("""
            SELECT journal_id AS journalId, event_id AS eventId,
                   transaction_reference AS transactionReference,
                   fd_account_no AS fdAccountNo, business_date AS businessDate,
                   currency, amount, debit_gl_account AS debitGlAccount,
                   credit_gl_account AS creditGlAccount, status, posted_at AS postedAt
              FROM journal_entries
             ORDER BY posted_at DESC
             LIMIT %s
        """, (limit,))
        return jsonify(cursor.fetchall())
    finally:
        connection.close()


if os.getenv("ACCOUNTING_DISABLE_CONSUMER", "false").lower() != "true":
    threading.Thread(target=consume_forever, name="kafka-accounting-consumer", daemon=True).start()
