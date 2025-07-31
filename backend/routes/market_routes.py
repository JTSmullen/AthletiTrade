from flask import Blueprint, jsonify, current_app, request
from app import MARKET_MAKER_ID

market_bp = Blueprint('market', __name__)

@market_bp.route('/orderbooks/<string:player_id>', methods=['GET'])
def get_order_book(player_id: str):
    """Endpoint to get the live order book for a specific player."""
    market = current_app.market
    book = market.get_order_book(player_id)
    return jsonify(book.get_book_display())

@market_bp.route('/history/<string:player_id>', methods=['GET'])
def get_history(player_id: str):
    """Endpoint to get historical price and volume data for a player."""
    market = current_app.market
    # The get_historical_data method will create the tracker if it doesn't exist
    history_tracker = market.get_historical_data(player_id)
    return jsonify(history_tracker.get_summary())

@market_bp.route('/players', methods=['GET'])
def get_all_players():
    """Returns a list of all players with active markets."""
    market = current_app.market
    player_ids = list(market.order_books.keys())
    return jsonify({"players": player_ids})

# --- THIS IS THE NEW ENDPOINT ---
@market_bp.route('/players/search', methods=['GET'])
def search_players():
    """
    Searches for players based on a query string.
    Example: /market/players/search?q=ron
    """
    search_term = request.args.get('q', '').strip().lower()
    if not search_term:
        return jsonify([]) # Return empty list if no search term

    market = current_app.market
    all_player_ids = list(market.order_books.keys())

    # Find all players whose names contain the search term (case-insensitive)
    matches = [
        player_id for player_id in all_player_ids 
        if search_term in player_id.lower()
    ]
    
    # Return up to 10 matches
    return jsonify(matches[:10])

@market_bp.route('/player_info/<string:player_id>', methods=['GET'])
def get_player_info(player_id):
    """
    Gets key information for a player, including their IPO price.
    """
    db = current_app.db
    
    # The IPO price is the price of the initial sell order from the market maker
    ipo_order = db.execute(
        "SELECT price FROM orders WHERE player_id = ? AND user_id = ? AND side = 'sell'",
        (player_id, MARKET_MAKER_ID)
    ).fetchone()

    if not ipo_order:
        return jsonify({"error": "Player not found or has no IPO price"}), 404

    return jsonify({
        "player_id": player_id,
        "ipo_price": ipo_order['price']
    })