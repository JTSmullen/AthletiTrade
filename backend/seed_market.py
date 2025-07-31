import sqlite3
import time
import uuid
import json
from werkzeug.security import generate_password_hash
from nba_api.stats.static import players
from nba_api.stats.endpoints import playergamelog
import pandas as pd
import requests
from datetime import datetime

# --- Configuration ---
DATABASE_FILE = 'trading_app.db'
MARKET_MAKER_ID = '00000000-0000-0000-0000-000000000000'
NUM_TRADING_BOTS = 10 # The number of trading bot users to create
BOT_STARTING_CASH = 100000.0 # How much cash each trading bot starts with

def get_most_recent_season():
    """
    Determines the string for the most recently completed NBA season.
    e.g., in July 2025, the last completed season was 2024-25.
    """
    now = datetime.now()
    # The NBA regular season ends in April and playoffs in June. August is a safe cutoff.
    if now.month < 8:
        # If before August, the season that just concluded started last calendar year.
        start_year = now.year - 1
    else:
        # If in or after August, a new season is starting, so the last completed one started last year.
        start_year = now.year -1
    
    end_year = str(start_year + 1)[-2:]
    return f"{start_year}-{end_year}"

def get_player_game_log(player_api_id, season_str):
    """Fetches the game log for a player for a given season using their numerical ID."""
    try:
        game_log = playergamelog.PlayerGameLog(player_id=player_api_id, season=season_str)
        df = game_log.get_data_frames()[0]
        return df
    except requests.exceptions.RequestException as e:
        print(f"    - API Error fetching game log for player {player_api_id}: {e}")
        return pd.DataFrame()
    except IndexError: # This happens when a player has no game logs for the season.
        return pd.DataFrame()

def get_player_starting_price_from_stats(player_stats, player_name):
    """Calculates the starting price for a player based on their stats."""
    if player_stats.empty:
        print(f"    - No stats found for '{player_name}'. Assigning default price.")
        return 10.0  # Assign a default base price for players with no recent stats

    weights = {
        "AST": 0.15,
        "REB": 0.12,
        "PLUS_MINUS": 0.08,
        "TOV": -0.05,
        "STL": 0.10,
        "BLK": 0.09,
        "PTS": 0.20,
        "FGA": -0.03,
        "FG3_PCT": 0.10,
        "FG_PCT": 0.04,
    }

    weighted_sum = 0
    for stat, weight in weights.items():
        if stat in player_stats.columns and pd.api.types.is_numeric_dtype(player_stats[stat]):
            # For percentages, we multiply by 100 to give them a more impactful score
            if stat.endswith("_PCT"):
                weighted_sum += player_stats[stat].mean() * weight * 100
            else:
                weighted_sum += player_stats[stat].mean() * weight
    
    # Return a positive price, rounded to 2 decimal places, with a minimum of 10.0
    return max(1.25, round(weighted_sum, 2))

def get_nba_players_for_seeding():
    """
    Fetches all active NBA players and calculates their IPO price and a tiered float.
    The player's full name is used as the ID for the market.
    """
    print("Fetching all active NBA players...")
    active_players = players.get_active_players()
    season_to_fetch = get_most_recent_season()
    print(f"Using season '{season_to_fetch}' for player stat calculations.")
    
    players_to_seed = []
    
    for player in active_players:
        player_api_id = player['id']
        player_name = player['full_name']
        print(f"Processing player: {player_name}")
        
        player_stats = get_player_game_log(player_api_id, season_to_fetch)
        ipo_price = get_player_starting_price_from_stats(player_stats, player_name)
        
        # --- DYNAMIC FLOAT LOGIC (TIER-BASED QUANTITY) ---
        player_float = 0
        if ipo_price > 80:      # Superstar Tier
            player_float = 1000
        elif ipo_price > 50:    # Star Tier
            player_float = 750
        elif ipo_price > 25:    # Role Player Tier
            player_float = 500
        else:                   # Bench/Prospect Tier
            player_float = 250
        # ----------------------------------------------------

        players_to_seed.append({
            "player_id": player_name, # Use the player's full name as the ID
            "ipo_price": ipo_price,
            "float": player_float
        })
        
        # Add a small delay to avoid overwhelming the API
        time.sleep(0.6)
        if len(players_to_seed) >= 5: # For testing, only process the first 5 players
            break


    return players_to_seed

def create_trading_bot_users(db: sqlite3.Connection):
    """Creates a pool of trading bot users and saves their credentials."""
    print(f"\nCreating {NUM_TRADING_BOTS} trading bot users...")
    cursor = db.cursor()
    bot_credentials = []

    for i in range(1, NUM_TRADING_BOTS + 1):
        username = f"bot_trader_{i}"
        password = str(uuid.uuid4()) # A random, secure password
        user_id = str(uuid.uuid4())
        password_hash = generate_password_hash(password)

        cursor.execute("SELECT user_id FROM users WHERE username = ?", (username,))
        if cursor.fetchone():
            print(f"User '{username}' already exists. Skipping.")
            continue
        
        cursor.execute(
            "INSERT INTO users (user_id, username, password_hash, cash_balance) VALUES (?, ?, ?, ?)",
            (user_id, username, password_hash, BOT_STARTING_CASH)
        )
        bot_credentials.append({"username": username, "password": password})
    
    db.commit()

    # Save the credentials so the runner script can log them in
    with open('bot_credentials.json', 'w') as f:
        json.dump(bot_credentials, f, indent=2)
    
    print(f"{len(bot_credentials)} trading bots created and credentials saved to bot_credentials.json.")

def get_next_order_id(cursor: sqlite3.Cursor) -> int:
    """Gets a new unique order ID from the database."""
    max_id_row = cursor.execute(
        "SELECT MAX(order_id) FROM (SELECT order_id FROM orders UNION ALL SELECT taker_order_id FROM trades UNION ALL SELECT maker_order_id FROM trades)"
    ).fetchone()
    max_id = max_id_row[0] if max_id_row[0] is not None else 0
    return max_id + 1

def create_market_maker(db: sqlite3.Connection):
    """Creates the special market maker user if it doesn't exist."""
    cursor = db.cursor()
    cursor.execute("SELECT user_id FROM users WHERE user_id = ?", (MARKET_MAKER_ID,))
    if cursor.fetchone():
        print("Market Maker user already exists.")
        return

    print("Creating Market Maker user...")
    password_hash = generate_password_hash(str(uuid.uuid4()))
    cursor.execute(
        "INSERT INTO users (user_id, username, password_hash, cash_balance) VALUES (?, ?, ?, ?)",
        (MARKET_MAKER_ID, 'market_maker', password_hash, 9999999999)
    )
    db.commit()
    print("Market Maker user created successfully.")

def seed_player_orders(db: sqlite3.Connection, players_to_seed):
    """Inserts initial sell orders for players from the market maker."""
    cursor = db.cursor()
    print("\nSeeding initial player offerings...")

    for player in players_to_seed:
        player_id = player['player_id'] # This is the player's name
        
        cursor.execute("SELECT COUNT(*) FROM orders WHERE player_id = ? AND user_id = ?", (player_id, MARKET_MAKER_ID))
        if cursor.fetchone()[0] > 0:
            print(f"'{player_id}' already has a market maker sell order. Skipping.")
            continue

        ipo_price = player['ipo_price']
        quantity = player['float']
        
        print(f"Creating IPO sell order for '{player_id}': {quantity} shares at ${ipo_price}")
        
        # --- THIS IS THE CHANGE ---
        # We no longer provide an order_id. The database will handle it automatically.
        cursor.execute(
            "INSERT INTO orders (user_id, player_id, side, price, quantity, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
            (MARKET_MAKER_ID, player_id, 'sell', ipo_price, quantity, time.time())
        )
    
    db.commit()
    print("\nMarket seeding complete.")

if __name__ == '__main__':
    # The new, complete setup process
    players_to_seed = get_nba_players_for_seeding()
    
    conn = sqlite3.connect(DATABASE_FILE)
    try:
        create_market_maker(conn)
        create_trading_bot_users(conn) # New step
        seed_player_orders(conn, players_to_seed)
    finally:
        conn.close()

    # Save the IPO data for the bots
    with open('ipo_prices.json', 'w') as f:
        json.dump(players_to_seed, f, indent=2)
    print("\nMarket setup complete. IPO prices saved to ipo_prices.json.")