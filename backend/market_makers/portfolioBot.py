import requests
import time
import random

class PortfolioBot:
    """
    A more advanced bot that is aware of its own portfolio.
    Its goal is to buy low and sell high based on its current position.
    """
    def __init__(self, player_id: str, bot_user_token: str, api_base_url: str, bot_username: str):
        self.player_id = player_id
        self.api_base_url = api_base_url
        self.username = bot_username
        self.session = requests.Session()
        self.session.headers.update({'Authorization': f'Bearer {bot_user_token}'})

        # Internal state of the bot's portfolio for this player
        self.cash: float = 0
        self.shares: int = 0
        self.avg_cost: float = 0
        
        self.min_wait = 30  # seconds
        self.max_wait = 90 # seconds
        print(f"[{self.username} -> {self.player_id}] Portfolio Bot initialized.")

    def update_portfolio(self):
        """Fetches the bot's current portfolio from the API to update its internal state."""
        try:
            response = self.session.get(f"{self.api_base_url}/api/portfolio")
            response.raise_for_status()
            portfolio_data = response.json()
            
            self.cash = portfolio_data.get('cash_balance', 0)
            
            # Find the specific holding for the player this bot manages
            holding = next((h for h in portfolio_data.get('holdings', []) if h['player_id'] == self.player_id), None)
            
            if holding:
                self.shares = holding.get('quantity', 0)
                self.avg_cost = holding.get('avg_cost', 0)
            else:
                self.shares = 0
                self.avg_cost = 0
                
        except requests.exceptions.RequestException:
            # If the API call fails, we just keep our old state and try again next loop.
            pass

    def get_order_book(self) -> dict | None:
        """Fetches the current aggregated order book for the player."""
        try:
            response = self.session.get(f"{self.api_base_url}/market/orderbooks/{self.player_id}")
            response.raise_for_status()
            return response.json()
        except requests.exceptions.RequestException:
            return None

    def place_order(self, side: str, price: float, quantity: int):
        """Places a single limit order."""
        order_data = { "player_id": self.player_id, "side": side, "price": price, "quantity": quantity }
        try:
            response = self.session.post(f"{self.api_base_url}/api/orders", json=order_data)
            response.raise_for_status()
            print(f"[{self.username}] Placed {side} order for {quantity} {self.player_id} @ ${price:.2f}")
        except requests.exceptions.RequestException:
            # Expected if funds/shares are insufficient.
            pass

    def run(self):
        """The main decision-making loop for the Portfolio Bot."""
        # Start with a random delay to prevent all bots from acting at once.
        time.sleep(random.uniform(1, self.min_wait))
        
        while True:
            try:
                # 1. Always get the latest portfolio state before making a decision.
                self.update_portfolio()

                order_book = self.get_order_book()
                if not order_book or not order_book.get('bids') or not order_book.get('asks'):
                    time.sleep(self.min_wait)
                    continue

                best_bid = float(max(order_book['bids'].keys()))
                best_ask = float(min(order_book['asks'].keys()))

                # 2. The core logic: decide what to do based on current shares.
                if self.shares > 0:
                    # --- I OWN SHARES: I should consider selling. ---
                    # Set a profit target (e.g., 5% above my average cost)
                    profit_price = round(self.avg_cost * 1.05, 2)
                    
                    # If the market is willing to buy for more than my target, sell immediately.
                    if best_bid > profit_price:
                        print(f"[{self.username}] Taking profit! Selling {self.shares} {self.player_id} at market price ${best_bid}")
                        self.place_order('sell', best_bid, self.shares)
                    else:
                        # Otherwise, place a limit sell order at my profit target price.
                        self.place_order('sell', profit_price, self.shares)

                else:
                    # --- I OWN NO SHARES: I am looking to buy. ---
                    # Place a buy order somewhere between the best bid and the best ask.
                    target_buy_price = round(best_bid + (best_ask - best_bid) * 0.25, 2)
                    
                    # Don't spend more than 20% of my available cash on a single trade.
                    spend_limit = self.cash * 0.20
                    quantity = int(spend_limit / target_buy_price)

                    if quantity > 0:
                        self.place_order('buy', target_buy_price, quantity)

            except Exception as e:
                print(f"[{self.username}] An error occurred in the portfolio bot loop for {self.player_id}: {e}")

            # Wait for a random interval before the next action.
            time.sleep(random.uniform(self.min_wait, self.max_wait))