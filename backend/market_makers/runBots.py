import requests
import threading
import time
import json
import random
from marketMakerBot import MarketMakerBot
from trading_bot import TradingBot
from portfolioBot import PortfolioBot

# --- Configuration ---
API_BASE_URL = "http://127.0.0.1:5001"
MARKET_MAKER_UUID = "00000000-0000-0000-0000-000000000000"
BOTS_PER_PLAYER = 4 # How many trading bots (total) should be active on each player's market

def get_auth_token(username, password):
    """Logs in a user to get a standard JWT."""
    try:
        response = requests.post(f"{API_BASE_URL}/auth/login", json={"username": username, "password": password})
        response.raise_for_status()
        return response.json()['token']
    except requests.RequestException as e:
        print(f"Could not get auth token for {username}. Error: {e}")
        return None

def get_bot_token_for_market_maker():
    """Gets a permanent token for the market maker bot."""
    try:
        response = requests.post(f"{API_BASE_URL}/auth/generate_bot_token", json={"user_id": MARKET_MAKER_UUID})
        response.raise_for_status()
        return response.json()['token']
    except requests.RequestException:
        print(f"FATAL: Could not get auth token for Market Maker. Is the server running?")
        exit()

if __name__ == '__main__':
    print("--- Starting AthletiTrade Simulation Runner ---")
    
    # 1. Get the token for the Market Maker.
    mm_token = get_bot_token_for_market_maker()
    print("Successfully authenticated Market Maker.")

    # 2. Log in all trading bots.
    try:
        with open('bot_credentials.json', 'r') as f:
            bot_credentials = json.load(f)
    except FileNotFoundError:
        print("FATAL: bot_credentials.json not found. Please run the setup script first.")
        exit()

    bot_tokens = {}
    print(f"Logging in {len(bot_credentials)} trading bots...")
    for creds in bot_credentials:
        token = get_auth_token(creds['username'], creds['password'])
        if token:
            bot_tokens[creds['username']] = token
    print(f"Successfully logged in {len(bot_tokens)} trading bots.")
    
    # 3. Load the IPO data to know which players to manage.
    try:
        with open('ipo_prices.json', 'r') as f:
            players_to_manage = json.load(f)
    except FileNotFoundError:
        print("FATAL: ipo_prices.json not found. Please run the setup script first.")
        exit()

    # 4. Launch all bots.
    all_bots = []
    trading_bot_users = list(bot_tokens.items())

    for player_data in players_to_manage:
        player_id = player_data['player_id']
        ipo_price = player_data['ipo_price']

        # a) Launch one MarketMakerBot for this player.
        mm_bot = MarketMakerBot(player_id, mm_token, API_BASE_URL, ipo_price)
        all_bots.append(mm_bot)

        # b) Launch a mix of TradingBots and PortfolioBots for this player.
        for _ in range(BOTS_PER_PLAYER):
            bot_username, bot_token = random.choice(trading_bot_users)
            
            # 50% chance of being a smart PortfolioBot, 50% chance of being a simple TradingBot
            if random.random() > 0.5:
                bot = PortfolioBot(player_id, bot_token, API_BASE_URL, bot_username)
            else:
                bot = TradingBot(player_id, bot_token, API_BASE_URL, bot_username)
            all_bots.append(bot)
    
    # 5. Start all bot threads.
    print(f"\n--- Launching {len(all_bots)} total bots... ---")
    for bot in all_bots:
        thread = threading.Thread(target=bot.run, daemon=True)
        thread.start()
        time.sleep(0.05) # Stagger startups slightly

    print(f"\n--- All bots are running. Press Ctrl+C to stop. ---")
    while True:
        time.sleep(1)