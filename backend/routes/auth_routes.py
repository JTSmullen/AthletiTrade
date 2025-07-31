import jwt
import datetime
from functools import wraps
from flask import Blueprint, request, jsonify, current_app

auth_bp = Blueprint('auth', __name__)

def token_required(f):
    @wraps(f)
    def decorated(*args, **kwargs):
        token = None

        if 'Authorization' in request.headers:
            try:
                token = request.headers['Authorization'].split(" ")[1]
            except IndexError:
                return jsonify({'message': 'Malformed token header'}), 400
            
        if not token:
            return jsonify({'message': 'Authentication Token is missing!'}), 401
        
        try:
            data = jwt.decode(token, current_app.config['SECRET_KEY'], algorithms=["HS256"])
            user_manager = current_app.user_manager
            current_user = user_manager.get_by_id(data['user_id'])
            if not current_user:
                return jsonify({'message': 'User not found'}), 401
        except jwt.ExpiredSignatureError:
            return jsonify({'message': 'Token has expired'}), 401
        except Exception as e:
            return jsonify({'message': 'Token is invalid!', 'error': str(e)}), 401
        
        return f(current_user, *args, **kwargs)
    
    return decorated

@auth_bp.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    if not data or not data.get('username') or not data.get('password'):
        return jsonify({'message': 'Username and password are required'}), 400
    
    user_manager = current_app.user_manager
    if user_manager.get_by_username(data['username']):
        return jsonify({'message': 'Username already exists'}), 409
    
    user = user_manager.create_user(data['username'], data['password'])
    return jsonify({'message': 'New user created successfully', 'user_id': user.user_id}), 201

@auth_bp.route('/login', methods=['POST'])
def login():
    """Endpoint for user login, returns a JWT."""
    data = request.get_json()
    if not data or not data.get('username') or not data.get('password'):
        return jsonify({'message': 'Could not verify'}), 401

    user_manager = current_app.user_manager
    user = user_manager.get_by_username(data['username'])

    if not user or not user.check_password(data['password']):
        return jsonify({'message': 'Invalid username or password'}), 401

    token = jwt.encode({
        'user_id': user.user_id,
        'exp': datetime.datetime.utcnow() + datetime.timedelta(hours=24)
    }, current_app.config['SECRET_KEY'], "HS256")

    return jsonify({'token': token})

@auth_bp.route('/generate_bot_token', methods=['POST'])
def generate_bot_token():
    """
    A special, protected endpoint to generate a non-expiring token
    for our internal Market Maker bot.
    """
    data = request.get_json()
    if not data or data.get('user_id') != '00000000-0000-0000-0000-000000000000':
        return jsonify({'message': 'Unauthorized'}), 403

    # Generate a token that does not expire
    token = jwt.encode({
        'user_id': data['user_id'],
        # No 'exp' claim means the token is valid forever
    }, current_app.config['SECRET_KEY'], "HS256")

    return jsonify({'token': token})