from werkzeug.security import generate_password_hash
from .user import User
import uuid

class UserManager:
    def __init__(self, get_db):
        self.get_db = get_db

    def create_user(self, username, password) -> User:
        """Creates a new user, generates a UUID for them, and saves to the DB."""
        db = self.get_db()
        
        # 1. Generate a new, unique user_id
        user_id = str(uuid.uuid4())
        password_hash = generate_password_hash(password)

        # 2. Create the User object, now providing the required user_id
        new_user = User(
            user_id=user_id,
            username=username,
            password_hash=password_hash
        )

        # 3. Save the new user (including the user_id) to the database
        db.execute(
            "INSERT INTO users (user_id, username, password_hash, cash_balance) VALUES (?, ?, ?, ?)",
            (user_id, username, password_hash, new_user.portfolio.cash_balance)
        )
        db.commit()

        return new_user
        
    def get_all_users(self) -> list[User]:
        """
        Retrieves all users from the database, excluding any system accounts
        like the market maker.

        Returns a list of complete User objects.
        """
        db = self.get_db()
        
        # We select all columns (*) to ensure the _row_to_user helper
        # has all the data it needs to construct a full User object.
        user_rows = db.execute(
            "SELECT * FROM users WHERE username != 'market_maker'"
        ).fetchall()
        
        # Use the existing helper method to convert each database row into a User object
        users = [self._row_to_user(row) for row in user_rows]
        
        return users

    def get_by_username(self, username):
        db = self.get_db()
        user_row = db.execute('SELECT * FROM users WHERE username = ?', (username,)).fetchone()
        return self._row_to_user(user_row) if user_row else None

    def get_by_id(self, user_id):
        db = self.get_db()
        user_row = db.execute('SELECT * FROM users WHERE user_id = ?', (user_id,)).fetchone()
        return self._row_to_user(user_row) if user_row else None

    def _row_to_user(self, row):
        user = User(
            username=row['username'],
            password_hash=row['password_hash'],
            user_id=row['user_id']
        )
        user.portfolio.cash_balance = row['cash_balance']
        
        holdings_rows = self.get_db().execute(
            'SELECT * FROM holdings WHERE user_id = ?', (user.user_id,)
        ).fetchall()
        
        for h_row in holdings_rows:
            user.portfolio.holdings[h_row['player_id']] = {
                'quantity': h_row['quantity'],
                'avg_cost': h_row['avg_cost']
            }
        return user

    def process_trade_for_users(self, trade: dict):
        db = self.get_db()
        player_id = trade['player_id']
        print(f"Player ID (process_trade_for_users): {player_id}")
        price = trade['price']
        quantity = trade['quantity']

        buyer_user_id = ""
        seller_user_id = ""

        if trade['taker_order_side'] == 'buy':
            buyer_user_id = trade['taker_user_id']
            seller_user_id = trade['maker_user_id']
        else:
            buyer_user_id = trade['maker_user_id']
            seller_user_id = trade['taker_user_id']

        try:
            buyer = self.get_by_id(buyer_user_id)
            if not buyer: raise Exception(f"Buyer with ID {buyer_user_id} not found")
            
            buyer_trade_cost = price * quantity
            db.execute(
                "UPDATE users SET cash_balance = cash_balance - ? WHERE user_id = ?",
                (buyer_trade_cost, buyer_user_id)
            )

            current_qty = buyer.portfolio.holdings.get(player_id, {}).get('quantity', 0)
            current_avg_cost = buyer.portfolio.holdings.get(player_id, {}).get('avg_cost', 0.0)
            new_qty = current_qty + quantity
            new_avg_cost = ((current_qty * current_avg_cost) + buyer_trade_cost) / new_qty
            
            db.execute(
                """
                INSERT INTO holdings (user_id, player_id, quantity, avg_cost) VALUES (?, ?, ?, ?)
                ON CONFLICT(user_id, player_id) DO UPDATE SET
                quantity = excluded.quantity,
                avg_cost = excluded.avg_cost
                """,
                (buyer_user_id, player_id, new_qty, new_avg_cost)
            )

            seller = self.get_by_id(seller_user_id)
            if not seller: raise Exception(f"Seller with ID {seller_user_id} not found")
            
            seller_trade_proceeds = price * quantity
            db.execute(
                "UPDATE users SET cash_balance = cash_balance + ? WHERE user_id = ?",
                (seller_trade_proceeds, seller_user_id)
            )
            
            db.execute(
                "UPDATE holdings SET quantity = quantity - ? WHERE user_id = ? AND player_id = ?",
                (quantity, seller_user_id, player_id)
            )
            
            db.execute(
                "DELETE FROM holdings WHERE user_id = ? AND player_id = ? AND quantity <= 0",
                (seller_user_id, player_id)
            )
            
            db.commit()

        except Exception as e:
            db.rollback()
            print(f"CRITICAL ERROR during portfolio update transaction: {e}. Transaction rolled back.")

    def get_leaderboard_data(self, market):
        """Calculates total portfolio value for all users for the leaderboard."""
        db = self.get_db()
        # Corrected the query to select all columns (*)
        all_users = db.execute("SELECT * FROM users WHERE username != 'market_maker'").fetchall()
        
        leaderboard = []
        for user_row in all_users:
            # Now the _row_to_user function will receive all the data it needs
            user = self._row_to_user(user_row)
            
            holdings_value = 0
            if user.portfolio.holdings: # Added a check for empty holdings
                for player_id, data in user.portfolio.holdings.items():
                    last_price = market.get_last_price(player_id) or data['avg_cost']
                    holdings_value += data['quantity'] * last_price
                
            total_value = user.portfolio.cash_balance + holdings_value
            leaderboard.append({
                "username": user.username,
                "total_value": round(total_value, 2)
            })
            
        return sorted(leaderboard, key=lambda x: x['total_value'], reverse=True)[:20]
    
    def check_buying_power(self, user, new_order_cost: float) -> bool:
        """
        Checks if a user has sufficient funds for a new buy order, considering
        both their cash balance and the cash committed to existing open buy orders.

        Returns True if they have enough funds, False otherwise.
        """
        db = self.get_db()
        cash_balance = user.portfolio.cash_balance
        
        # Find all open buy orders for this user
        open_buy_orders = db.execute(
            "SELECT price, quantity FROM orders WHERE user_id = ? AND side = 'buy'",
            (user.user_id,)
        ).fetchall()
        
        # Calculate the total cash already committed to those open orders
        committed_cash = 0
        if open_buy_orders:
            committed_cash = sum(row['price'] * row['quantity'] for row in open_buy_orders)
            
        # Calculate the user's true, available buying power
        effective_buying_power = cash_balance - committed_cash
        
        print(f"Validation for {user.username}:")
        print(f"  Cash Balance: ${cash_balance:.2f}")
        print(f"  Committed in Open Orders: ${committed_cash:.2f}")
        print(f"  Effective Buying Power: ${effective_buying_power:.2f}")
        print(f"  Cost of New Order: ${new_order_cost:.2f}")

        return new_order_cost <= effective_buying_power
    
    def check_available_shares(self, user, player_id: str, quantity_to_sell: int) -> bool:
        """
        Checks if a user has sufficient shares for a new sell order, considering
        both their current holdings and the shares committed to existing open sell orders.

        Returns True if they have enough shares, False otherwise.
        """
        db = self.get_db()
        
        # Get the total number of shares the user currently owns
        total_shares = user.portfolio.holdings.get(player_id, {}).get('quantity', 0)
        
        # Find the total quantity of shares already committed to open sell orders
        open_sell_orders = db.execute(
            "SELECT SUM(quantity) as committed_shares FROM orders WHERE user_id = ? AND player_id = ? AND side = 'sell'",
            (user.user_id, player_id)
        ).fetchone()
        
        committed_shares = 0
        if open_sell_orders and open_sell_orders['committed_shares']:
            committed_shares = open_sell_orders['committed_shares']
            
        # Calculate the user's true, available shares
        available_shares = total_shares - committed_shares
        
        print(f"Validation for {user.username} selling {player_id}:")
        print(f"  Total Shares Owned: {total_shares}")
        print(f"  Committed in Open Orders: {committed_shares}")
        print(f"  Effective Available Shares: {available_shares}")
        print(f"  Attempting to Sell: {quantity_to_sell}")

        return quantity_to_sell <= available_shares