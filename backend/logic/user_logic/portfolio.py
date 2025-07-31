class Portfolio:
    """Manages a user's cash and holdings."""

    def __init__(self, user_id: str, cash_balance: float):
        # --- FIX 1: Store the user_id ---
        self.user_id = user_id
        self.cash_balance = cash_balance
        self.holdings: dict[str, dict] = {}

    def get_summary(self, market):
        """
        Calculates the current portfolio summary and fetches historical data.
        """
        holdings_summary = []
        holdings_value = 0
        
        for player_id, data in self.holdings.items():
            last_price = market.get_last_price(player_id) or data['avg_cost']
            market_value = data['quantity'] * last_price
            holdings_value += market_value
            holdings_summary.append({
                "player_id": player_id,
                "quantity": data['quantity'],
                "avg_cost": data['avg_cost'],
                "market_value": round(market_value, 2)
            })

        total_value = self.cash_balance + holdings_value

        # --- FIX 2: Use the stored user_id in the query ---
        db = market.get_db()
        history_rows = db.execute(
            """
            SELECT timestamp, total_value FROM portfolio_history
            WHERE user_id = ? 
            ORDER BY timestamp ASC
            LIMIT 100
            """,
            (self.user_id,) # Use the user_id stored in the portfolio
        ).fetchall()
        
        history_data = [
            {"time": row['timestamp'], "value": row['total_value']} 
            for row in history_rows
        ]
        # --- END FIX ---

        return {
            "cash_balance": round(self.cash_balance, 2),
            "total_value": round(total_value, 2),
            "holdings": holdings_summary,
            "history": history_data
        }