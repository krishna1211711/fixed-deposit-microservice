"""
Generates portfolio charts for a given customer.
Includes bar chart for principal vs projected maturity and pie chart for FD distribution.
"""
import os
import sys
import mysql.connector
import pandas as pd
import matplotlib.pyplot as plt
from dotenv import load_dotenv

load_dotenv()

def get_db_connection():
    return mysql.connector.connect(
        host=os.getenv("DB_HOST", "localhost"),
        port=os.getenv("DB_PORT", "3306"),
        user=os.getenv("DB_USER", "root"),
        password=os.getenv("DB_PASSWORD", "root123"),
        database=os.getenv("DB_NAME", "fd_bank_db")
    )

def generate_charts(customer_id):
    try:
        conn = get_db_connection()
        query = f"""
            SELECT a.account_number, a.principal_amount, a.maturity_amount, p.product_name 
            FROM fd_accounts a
            JOIN fd_products p ON a.product_id = p.id
            WHERE a.customer_id = {customer_id}
        """
        df = pd.DataFrame(pd.read_sql(query, conn))
        
        if df.empty:
            print(f"No FDs found for customer_id: {customer_id}")
            return
            
        # Bar Chart
        plt.figure(figsize=(10, 6))
        x = range(len(df))
        plt.bar(x, df['principal_amount'], width=0.4, label='Principal', align='center')
        plt.bar([i + 0.4 for i in x], df['maturity_amount'], width=0.4, label='Maturity Amount', align='center')
        plt.xticks([i + 0.2 for i in x], df['account_number'], rotation=45)
        plt.title('Principal vs Projected Maturity')
        plt.xlabel('Account Number')
        plt.ylabel('Amount')
        plt.legend()
        plt.tight_layout()
        plt.savefig('portfolio_growth_chart.png')
        plt.close()
        print("Saved portfolio_growth_chart.png")
        
        # Pie Chart
        dist_df = df.groupby('product_name')['principal_amount'].sum()
        plt.figure(figsize=(8, 8))
        plt.pie(dist_df, labels=dist_df.index, autopct='%1.1f%%', startangle=140)
        plt.title('FD Distribution by Product Type')
        plt.savefig('fd_distribution_chart.png')
        plt.close()
        print("Saved fd_distribution_chart.png")
        
    except Exception as e:
        print(f"Error generating charts: {e}")
    finally:
        if 'conn' in locals() and conn.is_connected():
            conn.close()

if __name__ == "__main__":
    cust_id = sys.argv[1] if len(sys.argv) > 1 else 1
    generate_charts(cust_id)
