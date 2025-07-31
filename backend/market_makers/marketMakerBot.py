import requests
import time
import random

class MarketMakerBot:
    """
    A bot that provides layered liquidity for a single player, creating a deep and stable market.
    """
    
    # --- THIS IS THE KEY CHANGE ---
    # The spread is now much tighter on the inner levels to encourage trading.
    ORDER_LEVELS = [
        (0.05, 5),   # Closest layer: 5 shares, $0.15 spread
        (0.15, 10),  # Middle layer: 10 shares, $0.40 spread
        (.4, 20),  # Farthest layer: 20 shares, $1.00 spread
    ]
    # --- END CHANGE ---

    def __init__(self, player_id: str, bot_user_token: str, api_base_url: str, ipo_price: float):
        self.player_id = player_id
        self.api_base_url = api_base_url
        self.session = requests.Session()
        self.session.headers.update({'Authorization': f'Bearer {bot_user_token}'})

        self.fair_value = self.get_last_trade_price() or ipo_price
        self.update_interval = 20

        print(f"[{self.player_id} Bot] Initialized. Initial Fair Value: ${self.fair_value:.2f}")

    def get_last_trade_price(self) -> float | None:
        try:
            response = self.session.get(f"{self.api_base_url}/market/history/{self.player_id}")
            response.raise_for_status()
            data = response.json()
            if data.get('current_price') is not None and data.get('current_price') > 0:
                return float(data['current_price'])
        except requests.exceptions.RequestException:
            return None
        return None

    def get_my_open_orders(self) -> list:
        try:
            response = self.session.get(f"{self.api_base_url}/api/orders")
            response.raise_for_status()
            return [order for order in response.json() if order.get('player_id') == self.player_id]
        except requests.exceptions.RequestException:
            return []

    def cancel_order(self, order_id: int):
        try:
            self.session.delete(f"{self.api_base_url}/api/orders/{order_id}")
        except requests.exceptions.RequestException:
            pass

    def place_order(self, side: str, price: float, quantity: int):
        order_data = {"player_id": self.player_id, "side": side, "price": price, "quantity": quantity}
        try:
            self.session.post(f"{self.api_base_url}/api/orders", json=order_data).raise_for_status()
        except requests.exceptions.RequestException:
            pass

    def run(self):
        """The main loop for the bot's continuous operation."""
        while True:
            try:
                last_price = self.get_last_trade_price()
                if last_price:
                    self.fair_value = last_price
                
                self.fair_value *= random.uniform(0.999, 1.001)

                open_orders = self.get_my_open_orders()
                if open_orders:
                    for order in open_orders:
                        self.cancel_order(order['order_id'])
                    time.sleep(1)

                for spread, quantity in self.ORDER_LEVELS:
                    buy_price = round(self.fair_value - spread, 2)
                    self.place_order('buy', buy_price, quantity)
                    
                    sell_price = round(self.fair_value + spread, 2)
                    self.place_order('sell', sell_price, quantity)
                    time.sleep(0.2)

            except Exception as e:
                print(f"[{self.player_id} Bot] An error occurred in the main loop: {e}")

            time.sleep(self.update_interval)