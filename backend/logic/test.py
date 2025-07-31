# test.py
# This script demonstrates the full, integrated trading system.

from pprint import pprint
from Market import Market
from user_logic.userManager import UserManager

if __name__ == "__main__":
    # 1. SETUP: Initialize managers and create users
    # =================================================================
    print("--- 1. System Setup ---")
    market = Market()
    user_manager = UserManager()

    # Create our traders
    alice = user_manager.create_user("alice", "pass123")
    bob = user_manager.create_user("bob", "pass123")
    charlie = user_manager.create_user("charlie", "pass123")
    
    player_id = "Lionel Messi"
    
    # Manually give charlie some shares to sell for this test
    charlie.portfolio.holdings[player_id] = {'quantity': 50, 'avg_cost': 90.0}
    charlie.portfolio.cash_balance -= 50 * 90.0 # Adjust his cash to reflect the purchase
    
    print("\nInitial Portfolios:")
    pprint({"user": alice.username, "portfolio": alice.portfolio.get_summary(market)})
    pprint({"user": bob.username, "portfolio": bob.portfolio.get_summary(market)})
    pprint({"user": charlie.username, "portfolio": charlie.portfolio.get_summary(market)})
    print("-" * 25)

    # 2. POPULATE THE ORDER BOOK (Makers)
    # =================================================================
    print("\n--- 2. Placing Resting Orders on the Book ---")
    
    # Alice and Bob want to buy
    market.place_order(player_id, alice.user_id, "buy", 99.0, 10, user_manager)
    market.place_order(player_id, bob.user_id, "buy", 98.5, 5, user_manager)
    
    # Charlie wants to sell some of his shares
    market.place_order(player_id, charlie.user_id, "sell", 101.0, 15, user_manager)
    
    print("\nCurrent Order Book:")
    pprint(market.get_order_book(player_id).get_book_display())
    print("-" * 25)

    # 3. EXECUTE A TRADE (Taker)
    # =================================================================
    print("\n--- 3. A New Trader Executes a Trade ---")
    
    # Zara enters the market and wants to buy immediately
    zara = user_manager.create_user("zara", "pass123")
    
    print("\nPortfolios BEFORE the trade:")
    pprint({"user": zara.username, "portfolio": zara.portfolio.get_summary(market)})
    pprint({"user": charlie.username, "portfolio": charlie.portfolio.get_summary(market)})

    print(f"\nZara places a BUY order for 8 shares at ${charlie.username}'s ask price of $101.0...")
    
    # Zara's buy order will hit Charlie's resting sell order
    _, trades = market.place_order(player_id, zara.user_id, "buy", 101.0, 8, user_manager)
    
    print("\nTrade Executed:")
    pprint(trades)
    print("-" * 25)

    # 4. VERIFICATION
    # =================================================================
    print("\n--- 4. Verifying Final State ---")

    print("\nFinal Order Book (Charlie's sell order should be partially filled):")
    pprint(market.get_order_book(player_id).get_book_display())

    print("\nFinal Historical Data (Should show one trade of 8 shares at $101.0):")
    pprint(market.get_historical_data(player_id).get_summary())

    print("\nPortfolios AFTER the trade:")
    print(">>> Zara (Taker/Buyer): Should have -808 cash and +8 shares.")
    pprint({"user": zara.username, "portfolio": zara.portfolio.get_summary(market)})
    
    print("\n>>> Charlie (Maker/Seller): Should have +808 cash and -8 shares.")
    pprint({"user": charlie.username, "portfolio": charlie.portfolio.get_summary(market)})