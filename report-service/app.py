import csv
import io
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import mysql.connector
from flask import Flask, Response, jsonify, request
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle

app = Flask(__name__)


def role():
    return request.headers.get("X-User-Role", "").removeprefix("ROLE_")


def officer_or_admin():
    return role() in {"BANK_OFFICER", "ADMIN"}


def connection():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=int(os.getenv("DB_PORT", "3306")),
        user=os.getenv("DB_USER", "fd_user"),
        password=os.getenv("DB_PASSWORD", "fd_demo_password"),
        database=os.getenv("DB_NAME", "fd_bank_db"),
    )


def query(sql, params=()):
    conn = connection()
    try:
        cursor = conn.cursor(dictionary=True)
        cursor.execute(sql, params)
        return cursor.fetchall()
    finally:
        conn.close()


SUMMARY_SQL = """
SELECT p.product_code, p.product_name, COUNT(a.fd_account_no) AS account_count,
       COALESCE(SUM(a.principal_amount), 0) AS total_principal,
       COALESCE(SUM(a.accrued_interest), 0) AS total_accrued_interest
FROM products p
LEFT JOIN fd_accounts a ON a.product_code = p.product_code
GROUP BY p.product_code, p.product_name
ORDER BY p.product_code
"""


@app.get("/health")
def health():
    try:
        query("SELECT 1 AS ok")
        return jsonify(status="UP", database="UP")
    except Exception as exc:
        return jsonify(status="DOWN", database="DOWN", error=str(exc)), 503


@app.get("/fd-summary.csv")
def summary_csv():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER or ADMIN role required"), 403
    rows = query(SUMMARY_SQL)
    output = io.StringIO()
    fields = ["product_code", "product_name", "account_count", "total_principal", "total_accrued_interest"]
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
    return Response(output.getvalue(), mimetype="text/csv",
                    headers={"Content-Disposition": "attachment; filename=fd-summary.csv"})


@app.get("/fd-summary.pdf")
def summary_pdf():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER or ADMIN role required"), 403
    rows = query(SUMMARY_SQL)
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=landscape(A4), title="Fixed Deposit Summary")
    headers = ["Product code", "Product name", "Accounts", "Principal", "Accrued interest"]
    data = [headers] + [[r["product_code"], r["product_name"], r["account_count"],
                         str(r["total_principal"]), str(r["total_accrued_interest"])] for r in rows]
    table = Table(data, repeatRows=1)
    table.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#173B57")),
        ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
        ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold"),
        ("GRID", (0, 0), (-1, -1), 0.5, colors.HexColor("#B8C4CE")),
        ("ALIGN", (2, 1), (-1, -1), "RIGHT"),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#EDF3F6")]),
    ]))
    doc.build([table])
    return Response(buffer.getvalue(), mimetype="application/pdf",
                    headers={"Content-Disposition": "attachment; filename=fd-summary.pdf"})


@app.get("/customer-portfolio.png")
def portfolio_chart():
    requested_customer_id = request.args.get("customerId", "").strip()
    token_customer_id = request.headers.get("X-Customer-Id", "").strip()
    customer_id = requested_customer_id if officer_or_admin() else token_customer_id
    if role() == "CUSTOMER" and requested_customer_id and requested_customer_id != token_customer_id:
        return jsonify(error="Customers may only view their own portfolio"), 403
    if role() not in {"CUSTOMER", "BANK_OFFICER", "ADMIN"}:
        return jsonify(error="Authorized role required"), 403
    if not customer_id:
        return jsonify(error="customerId is required"), 400
    rows = query("""
        SELECT fd_account_no, principal_amount, accrued_interest
        FROM fd_accounts WHERE customer_id = %s ORDER BY created_at
    """, (customer_id,))
    if not rows:
        return jsonify(error="No fixed deposits found for customer"), 404
    labels = [r["fd_account_no"] for r in rows]
    principal = [float(r["principal_amount"]) for r in rows]
    projected = [float(r["principal_amount"] + r["accrued_interest"]) for r in rows]
    x = range(len(labels))
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.bar([i - 0.2 for i in x], principal, 0.4, label="Principal")
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
