from flask import Blueprint, request, jsonify, current_app
from .auth_routes import token_required
from app import get_db
from app import MARKET_MAKER_ID

portfolio_bp = Blueprint('portfolio', __name__)

@portfolio_bp.route('/portfolio', methods=['GET'])
@token_required
def get_portfolio(current_user):
    """Gets the logged-in user's portfolio summary."""
    market = current_app.market
    return jsonify(current_user.portfolio.get_summary(market))


@portfolio_bp.route('/orders', methods=['POST'])
@token_required
def place_user_order(current_user):
    """Places a new order for the logged-in user after validating funds/shares."""
    data = request.get_json()
    required_fields = ["player_id", "side", "price", "quantity"]
    if not all(field in data for field in required_fields):
        return jsonify({"error": f"Missing fields. Required: {required_fields}"}), 400

    try:
        player_id = data['player_id']
        side = data['side']
        price = float(data['price'])
        quantity = int(data['quantity'])
    except (ValueError, TypeError):
        return jsonify({"error": "Invalid data type for price or quantity"}), 400
    
    user_manager = current_app.user_manager
    market = current_app.market

    # --- THIS IS THE NEW, CORRECTED VALIDATION LOGIC ---
    if side == 'buy':
        cost = price * quantity
        if not user_manager.check_buying_power(current_user, cost):
            return jsonify({"error": "Insufficient buying power when considering open orders."}), 403
    
    elif side == 'sell':
        # Exempt the market maker from this check
        if current_user.user_id != MARKET_MAKER_ID:
            # Use our new method to check the true available shares
            if not user_manager.check_available_shares(current_user, player_id, quantity):
                return jsonify({"error": "Insufficient shares when considering open orders."}), 403
    
    else:
        return jsonify({"error": "Side must be 'buy' or 'sell'"}), 400
    # --- END VALIDATION LOGIC ---
            
    order_id, trades = market.place_order(
        player_id, current_user.user_id, side, price, quantity, user_manager
    )

    return jsonify({
        "message": "Order processed successfully",
        "order_id": order_id,
        "trades_executed": trades
    }), 201

    return jsonify({
        "message": "Order processed successfully",
        "order_id": order_id,
        "trades_executed": trades
    }), 201


@portfolio_bp.route('/orders', methods=['GET'])
@token_required
def get_open_orders(current_user):
    """Gets a list of the current user's open orders across all markets."""
    market = current_app.market
    user_orders = []
    for book in market.order_books.values():
        for order in book.orders.values():
            if order.user_id == current_user.user_id:
                user_orders.append(vars(order))
    return jsonify(user_orders)


@portfolio_bp.route('/orders/<int:order_id>', methods=['DELETE'])
@token_required
def cancel_user_order(current_user, order_id: int):
    """Cancels one of the user's open orders."""
    db = get_db()
    market = current_app.market

    # 1. Find the order in the database (the single source of truth)
    order_to_cancel_row = db.execute(
        "SELECT * FROM orders WHERE order_id = ?", (order_id,)
    ).fetchone()

    # 2. Check if the order exists
    if not order_to_cancel_row:
        return jsonify({"error": "Order not found or already filled/cancelled"}), 404

    # 3. Check if the current user owns this order
    if order_to_cancel_row['user_id'] != current_user.user_id:
        return jsonify({"error": "Permission denied. You do not own this order."}), 403

    # 4. Get the player_id to find the correct in-memory OrderBook
    player_id = order_to_cancel_row['player_id']
    book = market.get_order_book(player_id)

    # 5. Tell the book to cancel the order (which handles DB and in-memory state)
    success = book.cancel_order(order_id)
    
    if success:
        return jsonify({"message": "Order cancelled successfully"}), 200
    else:
        # This might happen in a rare race condition, but it's good to handle
        return jsonify({"error": "Failed to cancel order in the order book"}), 500

@portfolio_bp.route('/portfolio/history/<string:player_id>', methods=['GET'])
@token_required
def get_user_player_trade_history(current_user, player_id):
    """
    Calculates lifetime trading stats for a specific user and player using
    robust queries that account for both maker and taker trades.
    """
    db = get_db()
    user_id = current_user.user_id

    # --- THIS IS THE FIX ---

    # Calculate total money spent on BUYING this player.
    # This happens when the user is the taker of a 'buy' or the maker of a 'sell'.
    buy_stats = db.execute(
        """
        SELECT SUM(price * quantity) as total_cost
        FROM trades
        WHERE
            player_id = ? AND
            (
                (taker_user_id = ? AND taker_order_side = 'buy') OR
                (maker_user_id = ? AND taker_order_side = 'sell')
            )
        """,
        (player_id, user_id, user_id)
    ).fetchone()

    # Calculate total money received from SELLING this player.
    # This happens when the user is the taker of a 'sell' or the maker of a 'buy'.
    sell_stats = db.execute(
        """
        SELECT SUM(price * quantity) as total_proceeds
        FROM trades
        WHERE
            player_id = ? AND
            (
                (taker_user_id = ? AND taker_order_side = 'sell') OR
                (maker_user_id = ? AND taker_order_side = 'buy')
            )
        """,
        (player_id, user_id, user_id)
    ).fetchone()

    # --- END FIX ---

    total_cost = buy_stats['total_cost'] if buy_stats and buy_stats['total_cost'] else 0
    total_proceeds = sell_stats['total_proceeds'] if sell_stats and sell_stats['total_proceeds'] else 0
    
    return jsonify({
        "total_cost": total_cost,
        "total_proceeds": total_proceeds
    }), 200