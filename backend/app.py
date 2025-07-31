import sqlite3
from flask import Flask, g
from flask_cors import CORS  # <-- 1. IMPORT CORS

from logic.Market import Market
from logic.user_logic.userManager import UserManager

MARKET_MAKER_ID = '00000000-0000-0000-0000-000000000000'

DATABASE = 'trading_app.db'

def get_db():
    if 'db' not in g:
        g.db = sqlite3.connect(DATABASE, detect_types=sqlite3.PARSE_DECLTYPES)
        g.db.row_factory = sqlite3.Row
    return g.db

def close_db(e=None):
    db = g.pop('db', None)
    if db is not None:
        db.close()

def create_app():
    app = Flask(__name__)
    CORS(app, resources={r"/*": {"origins": "http://localhost:4200"}})

    app.config['SECRET_KEY'] = 'a-very-hard-to-guess-secret-string-for-jwt'
    
    app.teardown_appcontext(close_db)

    app.market = Market(get_db)
    app.user_manager = UserManager(get_db)
    
    with app.app_context():
        app.market.load_open_orders()
    
    print("Market and UserManager have been initialized.")

    from routes.auth_routes import auth_bp
    from routes.portfolio_routes import portfolio_bp
    from routes.market_routes import market_bp
    from routes.leaderboard_routes import leaderboard_bp
    
    app.register_blueprint(auth_bp, url_prefix='/auth')
    
    # I noticed your portfolio routes had a /api prefix. I'll assume that's intentional.
    app.register_blueprint(portfolio_bp, url_prefix='/api')
    
    app.register_blueprint(market_bp, url_prefix='/market')

    app.register_blueprint(leaderboard_bp, url_prefix='/leaderboard')

    @app.route('/')
    def index():
        return "Welcome to the AthletiTrade API! The server is running."

    return app

if __name__ == '__main__':
    app = create_app()
    app.run(debug=True, port=5001)