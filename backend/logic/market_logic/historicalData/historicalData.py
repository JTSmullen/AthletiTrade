import time
from datetime import datetime
import sqlite3

class HistoricalData:
    """
    Manages the persistent storage and retrieval of historical price data for a single player.
    This class interacts directly with the database.
    """
    def __init__(self, player_id: str, get_db_session):
        self.player_id = player_id
        self.get_db = get_db_session

    def record_trade(self, price: float, quantity: int, trade_time: datetime):
        """
        Records a new trade by updating the aggregated historical data in the database.
        This uses an 'UPSERT' (INSERT ON CONFLICT) operation for efficiency.
        
        For simplicity, this example only updates the '1h' granularity.
        """
        db = self.get_db()
        
        # Calculate the start of the hour for the given trade_time
        timestamp_bucket = int(trade_time.timestamp() - (trade_time.timestamp() % 3600))
        granularity = '1h'

        # This query will either INSERT a new row or UPDATE an existing one.
        # 'excluded' is a special keyword in SQLite that refers to the data
        # from the row that would have been inserted if the conflict didn't occur.
        db.execute(
            """
            INSERT INTO player_price_history (
                player_id, timestamp, granularity, open_price, high_price, low_price, close_price, volume
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(player_id, granularity, timestamp) DO UPDATE SET
                high_price = MAX(high_price, excluded.high_price),
                low_price = MIN(low_price, excluded.low_price),
                close_price = excluded.close_price,
                volume = volume + excluded.volume
            """,
            (
                self.player_id, timestamp_bucket, granularity,
                price, price, price, price, quantity  # Values for a new row
            )
        )
        db.commit()


    def get_summary(self) -> dict:
        """
        Queries the database to get the current price and historical data formatted for the frontend.
        """
        db = self.get_db()

        # --- Get Current Price ---
        # The most accurate current price is the last trade from the `trades` table.
        last_trade_row = db.execute(
            "SELECT price FROM trades WHERE player_id = ? ORDER BY timestamp DESC LIMIT 1",
            (self.player_id,)
        ).fetchone()
        current_price = last_trade_row['price'] if last_trade_row else 0

        # --- Get Historical Prices for the Chart ---
        # Fetch the last 100 hours of closing prices for the chart.
        history_rows = db.execute(
            """
            SELECT timestamp, close_price FROM player_price_history
            WHERE player_id = ? AND granularity = '1h'
            ORDER BY timestamp ASC
            LIMIT 100
            """,
            (self.player_id,)
        ).fetchall()

        # Format the data exactly as the frontend expects
        prices = [
            {"time": row['timestamp'], "price": row['close_price']}
            for row in history_rows
        ]

        return {
            "current_price": current_price,
            "prices": prices
        }