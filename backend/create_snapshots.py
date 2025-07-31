import sqlite3
import time
from app import create_app, get_db

def take_portfolio_snapshots():
    """
    Calculates the current total portfolio value for every user and
    saves it as a snapshot in the portfolio_history table.
    """
    app = create_app()
    with app.app_context():
        db = get_db()
        market = app.market
        user_manager = app.user_manager

        # 1. Get all users
        users = user_manager.get_all_users()
        if not users:
            print("No users found to snapshot.")
            return

        current_timestamp = int(time.time())
        print(f"Starting portfolio snapshot for {len(users)} users at {current_timestamp}...")

        for user in users:
            # 2a. Get cash balance
            cash_balance = user.portfolio.cash_balance
            
            # 2b & 2c. Get holdings and initialize value
            holdings = user.portfolio.holdings
            total_holdings_value = 0

            # 2d. Loop through each holding to calculate its current market value
            for player_id, holding_data in holdings.items():
                quantity = holding_data['quantity']
                
                # 2d-i. Get the most recent price for this player
                last_price = market.get_last_price(player_id)
                if last_price is None:
                    # If no trades, use the average cost as a fallback
                    last_price = holding_data['avg_cost']

                # 2d-ii & 2d-iii. Calculate and add to total
                market_value = quantity * last_price
                total_holdings_value += market_value
            
            # 2e. Calculate the final total portfolio value
            final_total_value = cash_balance + total_holdings_value

            # 3. Save the snapshot to the database
            db.execute(
                """
                INSERT INTO portfolio_history (user_id, timestamp, total_value)
                VALUES (?, ?, ?)
                """,
                (user.user_id, current_timestamp, final_total_value)
            )
            print(f"  - Saved snapshot for {user.username}: ${final_total_value:,.2f}")

        db.commit()
        print("Portfolio snapshot process completed successfully.")


if __name__ == '__main__':
    # This allows you to run the script directly from your terminal
    # to populate the history for all users right now.
    take_portfolio_snapshots()