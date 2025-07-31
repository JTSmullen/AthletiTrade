from logic.user_logic.portfolio import Portfolio
from werkzeug.security import check_password_hash

class User:
    """Represents a user in the trading system."""

    def __init__(self, user_id: str, username: str, password_hash: str, cash_balance: float = 10000.0):
        self.user_id = user_id
        self.username = username
        self.password_hash = password_hash
        
        # --- THIS IS THE FIX ---
        # Pass the user_id to the Portfolio so it knows who it belongs to.
        self.portfolio = Portfolio(user_id=user_id, cash_balance=cash_balance)
        # --- END FIX ---

    def check_password(self, password: str) -> bool:
        """Verifies a password against the stored hash."""
        return check_password_hash(self.password_hash, password)