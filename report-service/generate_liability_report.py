"""
Generates liability report showing total liabilities over time.
"""
import os
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

def generate_report():
    try:
        conn = get_db_connection()
        query = """
            SELECT 
                DATE_FORMAT(created_at, '%Y-%m') as month, 
                SUM(principal_amount + accrued_interest) as total_liability 
            FROM fd_accounts 
            GROUP BY month 
            ORDER BY month
        """
        df = pd.DataFrame(pd.read_sql(query, conn))
        
        if df.empty:
            print("No data found for liability report.")
            return
            
        csv_file = "liability_growth_report.csv"
        df.to_csv(csv_file, index=False)
        print(f"Exported to {csv_file}")
        
        plt.figure(figsize=(10, 6))
        plt.plot(df['month'], df['total_liability'], marker='o')
        plt.title('Liability Growth Over Time')
        plt.xlabel('Month')
        plt.ylabel('Total Liability')
        plt.xticks(rotation=45)
        plt.grid(True)
        plt.tight_layout()
        plt.savefig('liability_growth_chart.png')
        plt.close()
        print("Saved liability_growth_chart.png")
        
    except Exception as e:
        print(f"Error generating report: {e}")
    finally:
        if 'conn' in locals() and conn.is_connected():
            conn.close()

if __name__ == "__main__":
    generate_report()
