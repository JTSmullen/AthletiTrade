import sqlite3

DATABASE = 'trading_app.db'

print("\n--- Checking contents of the 'portfolio_history' table ---")

try:
    conn = sqlite3.connect(DATABASE)
    conn.row_factory = sqlite3.Row # This allows accessing columns by name
    cursor = conn.cursor()

    cursor.execute("SELECT * FROM portfolio_history")
    rows = cursor.fetchall()

    if not rows:
        print("!!! The portfolio_history table is EMPTY. This is the problem. !!!")
        print("-> Please run 'python create_snapshots.py' again.")
    else:
        print(f"Found {len(rows)} snapshot(s). The data exists in the database.")
        print(f"{'user_id':<36} | {'timestamp':<12} | {'total_value':<15}")
        print("-" * 70)
        for row in rows:
            print(f"{row['user_id']:<36} | {row['timestamp']:<12} | ${row['total_value']:<15,.2f}")

    conn.close()

except Exception as e:
    print(f"An error occurred: {e}")