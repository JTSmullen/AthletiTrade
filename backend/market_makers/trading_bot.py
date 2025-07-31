import requests
import time
import random

class TradingBot:
    """
    A bot that simulates a regular user by placing random orders. 
    It creates volume and causes the price to fluctuate. This bot is stateless.
    """
    def __init__(self, player_id: str, bot_user_token: str, api_base_url: str, bot_username: str):
        self.player_id = player_id
        self.api_base_url = api_base_url
        self.username = bot_username
        self.session = requests.Session()
        self.session.headers.update({'Authorization': f'Bearer {bot_user_token}'})

        self.min_wait = 20
        self.max_wait = 70
        
        # This bot has a 25% chance of placing an aggressive order that will definitely trade.
        self.TRADE_AGGRESSION = 0.25

        print(f"[{self.username} -> {self.player_id}] Trading Bot (Noise) initialized.")

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
            pass # Expected failure if funds/shares are insufficient.

    def run(self):
        """The main loop for the bot's trading activity."""
        time.sleep(random.uniform(5, self.min_wait))

        while True:
            try:
                order_book = self.get_order_book()
                if not order_book or not order_book.get('bids') or not order_book.get('asks'):
                    time.sleep(self.min_wait)
                    continue

                best_bid = float(max(order_book['bids'].keys()))
                best_ask = float(min(order_book['asks'].keys()))

                # Aggressive trading logic
                if random.random() < self.TRADE_AGGRESSION:
                    # AGGRESSIVE: Place an order that crosses the spread to force a trade.
                    if random.random() > 0.5:
                        side = 'buy'
                        price = best_ask
                    else:
                        side = 'sell'
                        price = best_bid
                    print(f"[{self.username}] Placing AGGRESSIVE {side} order for {self.player_id}!")
                else:
                    # PASSIVE: Place an order inside the spread.
                    if random.random() > 0.5:
                        side = 'buy'
                        price = round(best_bid * random.uniform(1.0005, 1.0015), 2)
                    else:
                        side = 'sell'
                        price = round(best_ask * random.uniform(0.9985, 0.9995), 2)

                quantity = random.randint(1, 3)
                self.place_order(side, price, quantity)

            except Exception as e:
                print(f"[{self.username}] An error occurred in the trading loop for {self.player_id}: {e}")

            time.sleep(random.uniform(self.min_wait, self.max_wait))