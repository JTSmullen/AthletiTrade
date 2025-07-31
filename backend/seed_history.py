import sqlite3
import time
from datetime import datetime, timedelta
import random

DATABASE = 'trading_app.db'

def seed_player_history():
    conn = sqlite3.connect(DATABASE)
    cursor = conn.cursor()    # Get all unique player_ids from the orders table and their IPO prices
    cursor.execute("SELECT player_id, price FROM orders WHERE user_id = '00000000-0000-0000-0000-000000000000'")
    db_players_with_ipo = cursor.fetchall()

    if not db_players_with_ipo:
        print("No players found in the 'orders' table with market maker IPO prices. Please run seed_market.py first.")
        return

    players_to_seed_data = {row[0]: row[1] for row in db_players_with_ipo}
    players_to_seed = list(players_to_seed_data.keys())

    
    print("Seeding historical data for players...")    # Get all unique player_ids from the orders table
    cursor.execute("SELECT DISTINCT player_id FROM orders")
    db_players = cursor.fetchall()
    
    if not db_players:
        print("No players found in the 'orders' table. Please run seed_market.py first.")
        return

    players_to_seed = [row[0] for row in db_players]
    


    for player in players_to_seed:
        # Delete any old data for this player to prevent duplicates
        cursor.execute("DELETE FROM player_price_history WHERE player_id = ?", (player,))

        # Start with a base price
        current_price = round(random.uniform(90.0, 110.0), 2)        # Use the IPO price as the starting point for history generation
        current_price = players_to_seed_data.get(player, 100.0) # Default to 100 if not found

        
        # Generate data for the last 100 hours
        for i in range(100):
            # Go back in time by i hours
            timestamp_dt = datetime.now() - timedelta(hours=i)
            # Get the Unix timestamp for the start of that hour
            timestamp_bucket = int(timestamp_dt.timestamp() - (timestamp_dt.timestamp() % 3600))
            
            volume = random.randint(50, 200)
            
            # Insert the seed data
            cursor.execute(
                """
                INSERT INTO player_price_history 
                (player_id, timestamp, granularity, open_price, high_price, low_price, close_price, volume)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """,
                (
                    player,
                    timestamp_bucket,
                    '1h', # Hardcoding '1h' granularity
                    current_price,
                    current_price * 1.02, # Fake high
                    current_price * 0.98, # Fake low
                    current_price,
                    volume
                )
            )
            # Make the price walk randomly for the next data point
            current_price += round(random.uniform(-0.5, 0.5), 2)

    conn.commit()
    conn.close()
    print(f"Successfully seeded data for {len(players_to_seed)} players.")

if __name__ == '__main__':
    seed_player_history()