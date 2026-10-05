import csv
import io
import json
import os
import threading
import time
from datetime import datetime, timezone

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import mysql.connector
from flask import Flask, Response, jsonify, request
from kafka import KafkaConsumer
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle

app = Flask(__name__)
state = {"consumerThreadAlive": False, "connected": False, "lastError": None, "processed": 0}


def role():
    return request.headers.get("X-User-Role", "").removeprefix("ROLE_")


def officer_or_admin():
    return role() in {"BANK_OFFICER", "ADMIN", "AUDITOR"}


def utc_now():
    return datetime.now(timezone.utc).replace(tzinfo=None)


def db_connection():
    return mysql.connector.connect(
        host=os.getenv("REPORT_DB_HOST", "report-mysql"),
        port=int(os.getenv("REPORT_DB_PORT", "3306")),
        user=os.getenv("REPORT_DB_USER", "report_user"),
        password=os.getenv("REPORT_DB_PASSWORD", "report_demo_password"),
        database=os.getenv("REPORT_DB_NAME", "report_db"),
    )


def ensure_schema():
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS report_inbox (
              event_id VARCHAR(36) PRIMARY KEY,
              event_type VARCHAR(60) NOT NULL,
              received_at TIMESTAMP(6) NOT NULL
            )
        """)
        cursor.execute("""
            CREATE TABLE IF NOT EXISTS fd_account_read_model (
              fd_account_no VARCHAR(20) PRIMARY KEY,
              customer_id VARCHAR(80) NOT NULL,
              product_code VARCHAR(40) NOT NULL,
              currency VARCHAR(3) NOT NULL,
              principal_amount DECIMAL(18,3) NOT NULL DEFAULT 0,
              current_balance DECIMAL(18,3) NOT NULL DEFAULT 0,
              accrued_interest DECIMAL(18,6) NOT NULL DEFAULT 0,
              interest_rate DECIMAL(7,4) NOT NULL DEFAULT 0,
              tenure_months INT NOT NULL DEFAULT 0,
              status VARCHAR(40) NOT NULL,
              maturity_date DATE NULL,
              last_event_id VARCHAR(36) NOT NULL,
              last_event_at TIMESTAMP(6) NOT NULL,
              INDEX idx_report_customer (customer_id),
              INDEX idx_report_product_status (product_code, status)
            )
        """)
        connection.commit()
    finally:
        connection.close()


def lifecycle_status(event_type):
    return {
        "FD_OPENED": "ACTIVE",
        "FD_MATURED": "CLOSED",
        "FD_PREMATURELY_CLOSED": "PREMATURE_CLOSED",
        "FD_RENEWED": "RENEWED",
        "FD_CLOSED": "CLOSED",
    }.get(event_type, "ACTIVE")


def process_event(event):
    event_id = str(event.get("eventId", ""))
    event_type = str(event.get("eventType", ""))
    if not event_id or not event_type:
        raise ValueError("eventId and eventType are required")
    connection = db_connection()
    try:
        cursor = connection.cursor()
        cursor.execute("SELECT 1 FROM report_inbox WHERE event_id = %s", (event_id,))
        if cursor.fetchone():
            return
        if event.get("aggregateType") == "FD_ACCOUNT" and event.get("fdAccountNo"):
            principal = event.get("principalAmount") or event.get("amount") or 0
            balance = event.get("currentBalance")
            if balance is None:
                balance = principal
            cursor.execute("""
                INSERT INTO fd_account_read_model
                  (fd_account_no, customer_id, product_code, currency, principal_amount,
                   current_balance, accrued_interest, interest_rate, tenure_months, status, maturity_date,
                   last_event_id, last_event_at)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)
                ON DUPLICATE KEY UPDATE
                  customer_id = VALUES(customer_id), product_code = VALUES(product_code),
                  currency = VALUES(currency), principal_amount = VALUES(principal_amount),
                  current_balance = VALUES(current_balance), accrued_interest = VALUES(accrued_interest),
                  interest_rate = VALUES(interest_rate), tenure_months = VALUES(tenure_months),
                  status = VALUES(status), maturity_date = COALESCE(VALUES(maturity_date), maturity_date),
                  last_event_id = VALUES(last_event_id), last_event_at = VALUES(last_event_at)
            """, (
                event["fdAccountNo"], event.get("customerId", "UNKNOWN"),
                event.get("productCode", "UNKNOWN"), event.get("currency", "INR"),
                principal, balance, event.get("accruedInterest") or 0,
                event.get("interestRate") or 0, event.get("tenureMonths") or 0,
                event.get("status") or lifecycle_status(event_type), event.get("maturityDate"),
                event_id, utc_now(),
            ))
        cursor.execute(
            "INSERT INTO report_inbox (event_id, event_type, received_at) VALUES (%s, %s, %s)",
            (event_id, event_type, utc_now()),
        )
        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()


def consume_forever():
    state["consumerThreadAlive"] = True
    topic = os.getenv("FD_EVENTS_TOPIC", "fd.lifecycle.v1")
    while True:
        try:
            ensure_schema()
            consumer = KafkaConsumer(
                topic,
                bootstrap_servers=os.getenv("KAFKA_BOOTSTRAP_SERVERS", "kafka:9092").split(","),
                group_id=os.getenv("KAFKA_CONSUMER_GROUP", "fd-report-service-v1"),
                auto_offset_reset="earliest",
                enable_auto_commit=False,
                value_deserializer=lambda value: json.loads(value.decode("utf-8")),
            )
            state["connected"] = True
            state["lastError"] = None
            for message in consumer:
                process_event(message.value)
                consumer.commit()
                state["processed"] += 1
        except Exception as error:
            state["connected"] = False
            state["lastError"] = str(error)
            time.sleep(5)


def query_all(sql, params=()):
    connection = db_connection()
    try:
        cursor = connection.cursor(dictionary=True)
        cursor.execute(sql, params)
        return cursor.fetchall()
    finally:
        connection.close()


def summary_rows():
    return query_all("""
        SELECT product_code AS productCode, product_code AS productName,
               COUNT(*) AS totalAccounts,
               CAST(SUM(principal_amount) AS DOUBLE) AS totalPrincipal,
               CAST(SUM(accrued_interest) AS DOUBLE) AS totalInterestAccrued,
               SUM(status = 'ACTIVE') AS activeAccounts,
               SUM(status IN ('CLOSED','PREMATURE_CLOSED','RENEWED')) AS closedAccounts
          FROM fd_account_read_model
         GROUP BY product_code ORDER BY product_code
    """)


def portfolio_rows(customer_id):
    return query_all("""
        SELECT fd_account_no AS fdAccountNo, product_code AS productCode,
               CAST(principal_amount AS DOUBLE) AS principalAmount,
               CAST(interest_rate AS DOUBLE) AS interestRate,
               tenure_months AS tenureMonths, status, maturity_date AS maturityDate,
               CAST(accrued_interest AS DOUBLE) AS accruedInterest,
               CAST(current_balance + accrued_interest AS DOUBLE) AS projectedMaturityAmount
          FROM fd_account_read_model WHERE customer_id = %s ORDER BY fd_account_no
    """, (customer_id,))


@app.get("/health")
def health():
    try:
        ensure_schema()
        database = "UP"
    except Exception as error:
        database = "DOWN"
        state["lastError"] = str(error)
    disabled = os.getenv("REPORT_DISABLE_CONSUMER", "false").lower() == "true"
    status = "UP" if database == "UP" and (state["connected"] or disabled) else "DOWN"
    return jsonify({"status": status, "database": database, **state}), 200 if status == "UP" else 503


@app.get("/fd-summary")
def summary_json():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER, ADMIN or AUDITOR role required"), 403
    return jsonify(summary_rows())


@app.get("/fd-summary.csv")
def summary_csv():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER, ADMIN or AUDITOR role required"), 403
    rows = summary_rows()
    output = io.StringIO()
    fields = ["productCode", "productName", "totalAccounts", "totalPrincipal",
              "totalInterestAccrued", "activeAccounts", "closedAccounts"]
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
    return Response(output.getvalue(), mimetype="text/csv",
                    headers={"Content-Disposition": "attachment; filename=fd-summary.csv"})


@app.get("/export/csv")
def summary_csv_compatibility():
    return summary_csv()


@app.get("/customer-portfolio")
def customer_portfolio():
    if role() != "CUSTOMER":
        return jsonify(error="CUSTOMER role required"), 403
    customer_id = request.headers.get("X-Customer-Id", "").strip()
    if not customer_id:
        return jsonify(error="Authenticated customer ID is required"), 400
    return jsonify(portfolio_rows(customer_id))


@app.get("/customer-portfolio/<customer_id>")
def staff_customer_portfolio(customer_id):
    if not officer_or_admin():
        return jsonify(error="Staff role required"), 403
    return jsonify(portfolio_rows(customer_id))


@app.get("/customer-portfolio/export/csv")
def customer_portfolio_csv():
    if role() != "CUSTOMER":
        return jsonify(error="CUSTOMER role required"), 403
    customer_id = request.headers.get("X-Customer-Id", "").strip()
    rows = portfolio_rows(customer_id)
    output = io.StringIO()
    fields = ["fdAccountNo", "productCode", "principalAmount", "interestRate", "tenureMonths",
              "status", "maturityDate", "accruedInterest", "projectedMaturityAmount"]
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
    return Response(output.getvalue(), mimetype="text/csv",
                    headers={"Content-Disposition": "attachment; filename=fd_portfolio.csv"})


@app.get("/fd-summary.pdf")
def summary_pdf():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER, ADMIN or AUDITOR role required"), 403
    rows = summary_rows()
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=landscape(A4), title="Fixed Deposit Summary")
    data = [["Product", "Product name", "Accounts", "Principal", "Accrued", "Active", "Closed"]] + [[
        row["productCode"], row["productName"], row["totalAccounts"], str(row["totalPrincipal"]),
        str(row["totalInterestAccrued"]), row["activeAccounts"], row["closedAccounts"]] for row in rows]
    table = Table(data, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#173B57")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#B8C4CE")),
        ("ALIGN", (2, 1), (-1, -1), "RIGHT"),
    ]))
    doc.build([table])
    return Response(buffer.getvalue(), mimetype="application/pdf",
                    headers={"Content-Disposition": "attachment; filename=fd-summary.pdf"})


@app.get("/customer-portfolio.png")
def portfolio_chart():
    requested = request.args.get("customerId", "").strip()
    token_customer = request.headers.get("X-Customer-Id", "").strip()
    if role() == "CUSTOMER" and requested and requested != token_customer:
        return jsonify(error="Customers may only view their own portfolio"), 403
    if role() not in {"CUSTOMER", "BANK_OFFICER", "ADMIN", "AUDITOR"}:
        return jsonify(error="Authorized role required"), 403
    customer_id = token_customer if role() == "CUSTOMER" else requested
    if not customer_id:
        return jsonify(error="customerId is required"), 400
    rows = query_all("""
        SELECT fd_account_no, principal_amount, current_balance, accrued_interest
          FROM fd_account_read_model WHERE customer_id = %s ORDER BY fd_account_no
    """, (customer_id,))
    if not rows:
        return jsonify(error="No fixed deposits found for customer"), 404
    labels = [row["fd_account_no"] for row in rows]
    principal = [float(row["principal_amount"]) for row in rows]
    projected = [float(row["current_balance"] + row["accrued_interest"]) for row in rows]
    x = range(len(labels))
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.bar([i - 0.2 for i in x], principal, 0.4, label="Original principal")
    ax.bar([i + 0.2 for i in x], projected, 0.4, label="Current value")
    ax.set_xticks(list(x), labels, rotation=25, ha="right")
    ax.set_ylabel("Amount")
    ax.set_title(f"Fixed deposit portfolio: {customer_id}")
    ax.legend()
    fig.tight_layout()
    output = io.BytesIO()
    fig.savefig(output, format="png", dpi=140)
    plt.close(fig)
    return Response(output.getvalue(), mimetype="image/png")


def start_consumer():
    threading.Thread(target=consume_forever, name="kafka-report-consumer", daemon=True).start()


if os.getenv("REPORT_DISABLE_CONSUMER", "false").lower() != "true":
    start_consumer()
