"""
Generates a summary report of FD accounts grouped by product.
Exports to CSV and PDF.
"""
import os
import mysql.connector
import pandas as pd
from dotenv import load_dotenv
from reportlab.lib.pagesizes import letter
from reportlab.platypus import SimpleDocTemplate, Table, TableStyle
from reportlab.lib import colors

load_dotenv()

def get_db_connection():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "3306"),
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASSWORD", "root123"),
        database=os.getenv("DB_NAME", "fd_bank_db")
    )

def generate_report():
    try:
        conn = get_db_connection()
        query = """
            SELECT 
                p.product_code, 
                p.product_name, 
                COUNT(a.id) as account_count, 
                SUM(a.principal_amount) as total_principal, 
                SUM(a.accrued_interest) as total_accrued_interest
            FROM fd_accounts a
            JOIN fd_products p ON a.product_id = p.id
            GROUP BY p.product_code, p.product_name
        """
        df = pd.DataFrame(pd.read_sql(query, conn))
        
        # Export to CSV
        csv_file = "fd_summary_report.csv"
        df.to_csv(csv_file, index=False)
        print(f"Exported to {csv_file}")
        
        # Print summary
        print("Summary:")
        print(df)
        
        # Export to PDF
        pdf_file = "fd_summary_report.pdf"
        doc = SimpleDocTemplate(pdf_file, pagesize=letter)
        elements = []
        
        data = [df.columns.tolist()] + df.values.tolist()
        t = Table(data)
        t.setStyle(TableStyle([
            ('BACKGROUND', (0, 0), (-1, 0), colors.grey),
            ('TEXTCOLOR', (0, 0), (-1, 0), colors.whitesmoke),
            ('ALIGN', (0, 0), (-1, -1), 'CENTER'),
            ('FONTNAME', (0, 0), (-1, 0), 'Helvetica-Bold'),
            ('BOTTOMPADDING', (0, 0), (-1, 0), 12),
            ('BACKGROUND', (0, 1), (-1, -1), colors.beige),
            ('GRID', (0, 0), (-1, -1), 1, colors.black)
        ]))
        elements.append(t)
        doc.build(elements)
        print(f"Exported to {pdf_file}")
        
    except Exception as e:
        print(f"Error generating report: {e}")
    finally:
        if 'conn' in locals() and conn.is_connected():
            conn.close()

if __name__ == "__main__":
    generate_report()
