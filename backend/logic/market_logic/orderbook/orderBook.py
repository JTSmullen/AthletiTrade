import heapq
import time
import sqlite3
from collections import defaultdict
from logic.market_logic.orderbook.order import Order

class OrderBook:
    """
    Manages the order book for a single player, backed by a persistent database.
    """
    def __init__(self, player_id: str, get_db_session):
        self.player_id = player_id
        self.get_db = get_db_session
        self.bids: list[tuple[float, float, int]] = []
        self.asks: list[tuple[float, float, int]] = []
        self.orders: dict[int, Order] = {}
        self._load_open_orders_from_db()

    def _load_open_orders_from_db(self):
        """Loads all open orders for THIS player from the DB on startup."""
        db = self.get_db()
        rows = db.execute(
            "SELECT * FROM orders WHERE player_id = ? ORDER BY timestamp ASC",
            (self.player_id,)
        ).fetchall()

        for row in rows:
            # --- FIX ---
            # Now we pass the player_id to the Order constructor
            order = Order(
                user_id=row['user_id'],
                player_id=row['player_id'], # Use player_id from the database row
                side=row['side'],
                price=row['price'],
                quantity=row['quantity'],
                order_id=row['order_id'],
                timestamp=row['timestamp']
            )
            self.orders[order.order_id] = order
            self._add_order_to_heap(order)
        
        print(f"Loaded {len(self.orders)} open orders for player {self.player_id}.")

    def _add_order_to_heap(self, order: Order):
        heap_entry = (order.price, order.timestamp, order.order_id)
        if order.side == 'buy':
            heapq.heappush(self.bids, (-heap_entry[0], heap_entry[1], heap_entry[2]))
        else:
            heapq.heappush(self.asks, heap_entry)

    def process_new_order(self, user_id: str, side: str, price: float, quantity: int) -> tuple[list[dict], int | None]:
        db = self.get_db()
        
        # --- FIX ---
        # Provide the player_id when creating the temporary incoming order object
        incoming_order = Order(
            user_id=user_id,
            player_id=self.player_id,
            side=side,
            price=price,
            quantity=quantity
        )

        trades = self._match_engine(incoming_order, db)
        
        new_order_id = None
        if incoming_order.quantity > 0:
            cursor = db.execute(
                "INSERT INTO orders (user_id, player_id, side, price, quantity, timestamp) VALUES (?, ?, ?, ?, ?, ?)",
                (
                    incoming_order.user_id, incoming_order.player_id, incoming_order.side,
                    incoming_order.price, incoming_order.quantity, incoming_order.timestamp
                )
            )
            new_order_id = cursor.lastrowid
            incoming_order.order_id = new_order_id
            
            self.orders[new_order_id] = incoming_order
            self._add_order_to_heap(incoming_order)
        
        db.commit()
        return trades, new_order_id

# In your OrderBook class in logic/market_logic/orderbook/orderBook.py

    def _match_engine(self, incoming_order: Order, db: sqlite3.Connection) -> list[dict]:
        """
        The core matching engine. Compares an incoming order against the in-memory heaps
        and executes trades. All database and in-memory modifications happen here.
        """
        trades = []
        trade_timestamp = time.time()
        
        # --- LOGIC FOR A BUY ORDER ---
        if incoming_order.side == 'buy':
            while self.asks and incoming_order.quantity > 0 and incoming_order.price >= self.asks[0][0]:
                best_ask_price, _, existing_order_id = self.asks[0]
                existing_order = self.orders.get(existing_order_id)

                if not existing_order or existing_order.quantity == 0:
                    heapq.heappop(self.asks)
                    continue

                trade_quantity = min(incoming_order.quantity, existing_order.quantity)
                
                trade = { "price": best_ask_price, "quantity": trade_quantity, "maker_order_id": existing_order.order_id, "maker_user_id": existing_order.user_id, "taker_user_id": incoming_order.user_id, "taker_order_side": "buy" }
                trades.append(trade)
                
                db.execute("INSERT INTO trades (player_id, price, quantity, timestamp, taker_order_id, maker_order_id, taker_user_id, maker_user_id, taker_order_side) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", (self.player_id, best_ask_price, trade_quantity, trade_timestamp, 0, existing_order.order_id, incoming_order.user_id, existing_order.user_id, "buy"))
                
                incoming_order.quantity -= trade_quantity
                existing_order.quantity -= trade_quantity
                
                if existing_order.quantity == 0:
                    db.execute("DELETE FROM orders WHERE order_id = ?", (existing_order.order_id,))
                    heapq.heappop(self.asks)
                    del self.orders[existing_order.order_id]
                else:
                    db.execute("UPDATE orders SET quantity = ? WHERE order_id = ?", (existing_order.quantity, existing_order.order_id))

        # --- LOGIC FOR A SELL ORDER (CORRECTED) ---
        elif incoming_order.side == 'sell':
            while self.bids and incoming_order.quantity > 0 and incoming_order.price <= -self.bids[0][0]:
                best_bid_price_neg, _, existing_order_id = self.bids[0]
                existing_order = self.orders.get(existing_order_id)

                if not existing_order or existing_order.quantity == 0:
                    heapq.heappop(self.bids)
                    continue
                
                trade_quantity = min(incoming_order.quantity, existing_order.quantity)
                trade_price = -best_bid_price_neg # The actual price is positive

                trade = { "price": trade_price, "quantity": trade_quantity, "maker_order_id": existing_order.order_id, "maker_user_id": existing_order.user_id, "taker_user_id": incoming_order.user_id, "taker_order_side": "sell" }
                trades.append(trade)
                
                # --- THIS IS THE FIX ---
                # The INSERT statement now correctly uses `trade_price` instead of the non-existent `best_ask_price`.
                db.execute(
                    "INSERT INTO trades (player_id, price, quantity, timestamp, taker_order_id, maker_order_id, taker_user_id, maker_user_id, taker_order_side) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    (self.player_id, trade_price, trade_quantity, trade_timestamp, 0, existing_order.order_id, incoming_order.user_id, existing_order.user_id, "sell")
                )
                # --- END FIX ---
                
                incoming_order.quantity -= trade_quantity
                existing_order.quantity -= trade_quantity

                if existing_order.quantity == 0:
                    db.execute("DELETE FROM orders WHERE order_id = ?", (existing_order.order_id,))
                    heapq.heappop(self.bids)
                    del self.orders[existing_order.order_id]
                else:
                    db.execute("UPDATE orders SET quantity = ? WHERE order_id = ?", (existing_order.quantity, existing_order.order_id))
        
        return trades

    def cancel_order(self, order_id: int) -> bool:
        db = self.get_db()
        if order_id in self.orders:
            self.orders[order_id].quantity = 0 
            cursor = db.execute("DELETE FROM orders WHERE order_id = ?", (order_id,))
            db.commit()
            return cursor.rowcount > 0
        return False

    def get_book_display(self) -> dict:
        db = self.get_db()
        bids_rows = db.execute("SELECT price, SUM(quantity) as total_quantity FROM orders WHERE player_id = ? AND side = 'buy' GROUP BY price ORDER BY price DESC", (self.player_id,)).fetchall()
        asks_rows = db.execute("SELECT price, SUM(quantity) as total_quantity FROM orders WHERE player_id = ? AND side = 'sell' GROUP BY price ORDER BY price ASC", (self.player_id,)).fetchall()
        bids_display = {row['price']: row['total_quantity'] for row in bids_rows}
        asks_display = {row['price']: row['total_quantity'] for row in asks_rows}
        return {"bids": bids_display, "asks": asks_display}