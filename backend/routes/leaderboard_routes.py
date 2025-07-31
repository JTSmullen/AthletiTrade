from flask import Blueprint, jsonify, current_app

leaderboard_bp = Blueprint('leaderboard', __name__)

@leaderboard_bp.route('/leaderboard', methods=['GET'])
def get_leaderboard():
    """Endpoint to get the top users by portfolio value."""
    user_manager = current_app.user_manager
    market = current_app.market
    data = user_manager.get_leaderboard_data(market)
    return jsonify(data)