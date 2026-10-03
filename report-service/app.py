import csv
import io
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import requests
from flask import Flask, Response, jsonify, request
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4, landscape
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle

app = Flask(__name__)


def role():
    return request.headers.get("X-User-Role", "").removeprefix("ROLE_")


def officer_or_admin():
    return role() in {"BANK_OFFICER", "ADMIN"}


def fd_service_get(path):
    authorization = request.headers.get("Authorization")
    headers = {"Authorization": authorization} if authorization else {}
    response = requests.get(
        f'{os.getenv("FD_SERVICE_URI", "http://fd-service:8080")}{path}',
        headers=headers,
        timeout=15,
    )
    if response.status_code >= 400:
        return None, (jsonify(error="FD service request failed", status=response.status_code), response.status_code)
    return response.json(), None


@app.get("/health")
def health():
    try:
        response = requests.get(
            f'{os.getenv("FD_SERVICE_URI", "http://fd-service:8080")}/actuator/health',
            timeout=5,
        )
        response.raise_for_status()
        return jsonify(status="UP", fdService="UP")
    except Exception as exc:
        return jsonify(status="DOWN", fdService="DOWN", error=str(exc)), 503


@app.get("/fd-summary.csv")
def summary_csv():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER or ADMIN role required"), 403
    rows, error = fd_service_get("/api/report/fd-summary")
    if error:
        return error
    output = io.StringIO()
    fields = [
        "productCode", "productName", "totalAccounts", "totalPrincipal",
        "totalInterestAccrued", "activeAccounts", "closedAccounts",
    ]
    writer = csv.DictWriter(output, fieldnames=fields)
    writer.writeheader()
    writer.writerows(rows)
    return Response(
        output.getvalue(), mimetype="text/csv",
        headers={"Content-Disposition": "attachment; filename=fd-summary.csv"},
    )


@app.get("/fd-summary.pdf")
def summary_pdf():
    if not officer_or_admin():
        return jsonify(error="BANK_OFFICER or ADMIN role required"), 403
    rows, error = fd_service_get("/api/report/fd-summary")
    if error:
        return error
    buffer = io.BytesIO()
    doc = SimpleDocTemplate(buffer, pagesize=landscape(A4), title="Fixed Deposit Summary")
    headers = ["Product", "Product name", "Accounts", "Principal", "Accrued", "Active", "Closed"]
    data = [headers] + [[
        row["productCode"], row["productName"], row["totalAccounts"],
        str(row["totalPrincipal"]), str(row["totalInterestAccrued"]),
        row["activeAccounts"], row["closedAccounts"],
    ] for row in rows]
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
    return Response(
        buffer.getvalue(), mimetype="application/pdf",
        headers={"Content-Disposition": "attachment; filename=fd-summary.pdf"},
    )


@app.get("/customer-portfolio.png")
def portfolio_chart():
    requested_customer_id = request.args.get("customerId", "").strip()
    token_customer_id = request.headers.get("X-Customer-Id", "").strip()
    if role() == "CUSTOMER" and requested_customer_id and requested_customer_id != token_customer_id:
        return jsonify(error="Customers may only view their own portfolio"), 403
    if role() not in {"CUSTOMER", "BANK_OFFICER", "ADMIN"}:
        return jsonify(error="Authorized role required"), 403
    if role() == "CUSTOMER":
        portfolio_path = "/api/report/customer-portfolio"
        customer_id = token_customer_id
    else:
        if not requested_customer_id:
            return jsonify(error="customerId is required for officer/admin portfolio reports"), 400
        portfolio_path = f"/api/report/customer-portfolio/{requested_customer_id}"
        customer_id = requested_customer_id

    rows, error = fd_service_get(portfolio_path)
    if error:
        return error
    if not rows:
        return jsonify(error="No fixed deposits found for customer"), 404
    labels = [row["fdAccountNo"] for row in rows]
    principal = [float(row["principalAmount"]) for row in rows]
    projected = [float(row["projectedMaturityAmount"]) for row in rows]
    x = range(len(labels))
    fig, ax = plt.subplots(figsize=(10, 5))
    ax.bar([i - 0.2 for i in x], principal, 0.4, label="Principal")
    ax.bar([i + 0.2 for i in x], projected, 0.4, label="Projected maturity")
    ax.set_xticks(list(x), labels, rotation=25, ha="right")
    ax.set_ylabel("Amount")
    ax.set_title(f"Fixed deposit portfolio: {customer_id}")
    ax.legend()
    fig.tight_layout()
    output = io.BytesIO()
    fig.savefig(output, format="png", dpi=140)
    plt.close(fig)
    return Response(output.getvalue(), mimetype="image/png")
