from logic.market_logic.orderbook.orderBook import OrderBook
from logic.market_logic.historicalData.historicalData import HistoricalData
from datetime import datetime

class Market:
    """Manages all order books, historical data, and orchestrates trade execution."""

    def __init__(self, get_db):
        self.order_books: dict[str, OrderBook] = {}
        self.historical_data: dict[str, HistoricalData] = {}
        self.get_db = get_db

    def load_open_orders(self):
        """Finds all unique players with open orders and initializes their order books."""
        print("Loading open orders from database...")
        db = self.get_db()
        # Find all distinct player_ids that have open orders.
        player_id_rows = db.execute('SELECT DISTINCT player_id FROM orders').fetchall()
        
        for row in player_id_rows:
            player_id = row['player_id']
            # This will initialize the OrderBook, which in turn loads its own orders.
            self.get_order_book(player_id)
            
        print(f"Initialized order books for {len(self.order_books)} players.")

    def get_order_book(self, player_id: str) -> OrderBook:
        """Gets or creates an order book for a player."""
        if player_id not in self.order_books:
            # Pass the database session getter to the OrderBook constructor.
            self.order_books[player_id] = OrderBook(player_id, self.get_db)
        return self.order_books[player_id]

    def get_historical_data(self, player_id: str) -> HistoricalData:
        """Gets or creates a historical data tracker for a player."""
        if player_id not in self.historical_data:
            # Pass the database session getter to the HistoricalData constructor.
            self.historical_data[player_id] = HistoricalData(player_id, self.get_db)
        return self.historical_data[player_id]

    # --- THIS IS THE MISSING METHOD ---
    def place_order(self, player_id: str, user_id: str, side: str, price: float, quantity: int, user_manager) -> tuple[int | None, list[dict]]:
        """
        Main entry point to place an order, execute trades, and update all systems.
        This is the central orchestration point for a trade.
        """
        book = self.get_order_book(player_id)
        history_tracker = self.get_historical_data(player_id)

        # The OrderBook now handles all matching and returns trades and any new order ID
        trades, new_order_id = book.process_new_order(user_id, side, price, quantity)
        
        if trades:
            trade_time = datetime.now()
            for trade in trades:
                # Update historical data with the new trade
                history_tracker.record_trade(
                    price=trade['price'],
                    quantity=trade['quantity'],
                    trade_time=trade_time
                )
                
                # Update user portfolios
                trade['player_id'] = player_id
                user_manager.process_trade_for_users(trade)
        
        return new_order_id, trades
    # --- END OF MISSING METHOD ---
    
    def get_last_price(self, player_id: str) -> float | None:
        """
        Gets the last traded price for a player directly from the database.
        """
        db = self.get_db()
        row = db.execute(
            "SELECT price FROM trades WHERE player_id = ? ORDER BY timestamp DESC LIMIT 1",
            (player_id,)
        ).fetchone()
        
        return row['price'] if row else None