import sqlite3

def init_db():
    conn = sqlite3.connect('trading_app.db')
    cursor = conn.cursor()

    # --- Core Transactional Tables ---

    cursor.execute('''
    CREATE TABLE IF NOT EXISTS users (
        user_id TEXT PRIMARY KEY,
        username TEXT UNIQUE NOT NULL,
        password_hash TEXT NOT NULL,
        cash_balance REAL NOT NULL
    )''')

    cursor.execute('''
    CREATE TABLE IF NOT EXISTS holdings (
        user_id TEXT NOT NULL,
        player_id TEXT NOT NULL,
        quantity INTEGER NOT NULL,
        avg_cost REAL NOT NULL,
        PRIMARY KEY (user_id, player_id),
        FOREIGN KEY (user_id) REFERENCES users (user_id)
    )''')
    
    cursor.execute('''
    CREATE TABLE IF NOT EXISTS orders (
        order_id INTEGER PRIMARY KEY AUTOINCREMENT, 
        user_id TEXT NOT NULL,
        player_id TEXT NOT NULL,
        side TEXT NOT NULL,
        price REAL NOT NULL,
        quantity INTEGER NOT NULL,
        timestamp REAL NOT NULL,
        FOREIGN KEY (user_id) REFERENCES users (user_id)
    )''')

    cursor.execute('''
    CREATE TABLE IF NOT EXISTS trades (
        trade_id INTEGER PRIMARY KEY AUTOINCREMENT,
        player_id TEXT NOT NULL,
        price REAL NOT NULL,
        quantity INTEGER NOT NULL,
        timestamp REAL NOT NULL,
        taker_order_id INTEGER NOT NULL,
        maker_order_id INTEGER NOT NULL,
        taker_user_id TEXT NOT NULL,
        maker_user_id TEXT NOT NULL,
        taker_order_side TEXT NOT NULL,
        
        FOREIGN KEY (taker_user_id) REFERENCES users (user_id),
        FOREIGN KEY (maker_user_id) REFERENCES users (user_id)
    )''')

    # --- New Tables for Historical Data (for fast chart lookups) ---

    print("Creating new historical tables...")

    cursor.execute('''
    CREATE TABLE IF NOT EXISTS player_price_history (
        player_id TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        granularity TEXT NOT NULL,
        open_price REAL NOT NULL,
        high_price REAL NOT NULL,
        low_price REAL NOT NULL,
        close_price REAL NOT NULL,
        volume INTEGER NOT NULL,
        PRIMARY KEY (player_id, granularity, timestamp)
    )''')

    cursor.execute('''
    CREATE TABLE IF NOT EXISTS portfolio_history (
        user_id TEXT NOT NULL,
        timestamp INTEGER NOT NULL,
        total_value REAL NOT NULL,
        PRIMARY KEY (user_id, timestamp),
        FOREIGN KEY (user_id) REFERENCES users (user_id) ON DELETE CASCADE
    )''')

    conn.commit()
    conn.close()
    print("Database initialized successfully.")

if __name__ == '__main__':
    init_db()