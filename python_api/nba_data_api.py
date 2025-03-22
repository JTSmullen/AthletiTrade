from flask import Flask, jsonify, request
from nba_api.stats.static import players, teams
from nba_api.stats.endpoints import playergamelog, scoreboardv2, commonplayerinfo
import pandas as pd
from datetime import datetime, timedelta
import requests

app = Flask(__name__)

# --- Helper Functions ---

def get_team_id_by_name(team_name):
    """Finds a team ID by their name or abbreviation."""
    try:
        nba_teams = teams.get_teams()
        team = [team for team in nba_teams if team['full_name'] == team_name or team['abbreviation'] == team_name][0]
        return team['id']
    except IndexError:
        return None

def get_player_game_log(player_id):
    """Fetches the game log for a player for the past year."""
    try:
        game_log = playergamelog.PlayerGameLog(player_id=player_id, season=2024)
        df = game_log.get_data_frames()[0]
        return df
    except requests.exceptions.RequestException as e:
        print(f"Error fetching game log: {e}")
        return pd.DataFrame()
    
def get_player_starting_price_from_stats(player_stats):
    """Calculates the starting price for a player based on their stats."""
    if player_stats.empty:
        return 0

    weights = {
        "AST": 0.15,
        "REB": 0.12,
        "PLUS_MINUS": 0.08,
        "TOV": -0.05,
        "STL": 0.10,
        "BLK": 0.09,
        "PTS": 0.20,
        "FGA": -0.03,
        "FG3_PCT": 0.10,
        "FG_PCT": 0.04,
    }

    weighted_sum = 0
    for stat, weight in weights.items():
        if stat in player_stats.columns and pd.api.types.is_numeric_dtype(player_stats[stat]):
            if stat.endswith("_PCT"):
                weighted_sum += player_stats[stat].mean() * weight
            else:
                weighted_sum += player_stats[stat].mean() * weight
    return weighted_sum


def calculate_weighted_average(stats, rolling=False, last_n_games=10):
    """Calculates the weighted average of player stats using *only* the provided weights.

    Args:
        stats (pd.DataFrame): DataFrame of player stats.
        rolling (bool):  Whether to calculate a rolling average.
        last_n_games (int): Number of games to consider for the rolling average.

    Returns:
        dict: Weighted average stats, or an empty dict if stats are empty.
    """
    if stats.empty:
        return {}

    if rolling:
        stats = stats.head(last_n_games)

    weights = {
        "AST": 0.15,
        "REB": 0.12,
        "PLUS_MINUS": 0.08,
        "TOV": -0.05,
        "STL": 0.10,
        "BLK": 0.09,
        "PTS": 0.20,
        "FGA": -0.03,
        "FG3_PCT": 0.10,
        "FG_PCT": 0.04,
    }

    weighted_sum = 0
    for stat, weight in weights.items():
        if stat in stats.columns and pd.api.types.is_numeric_dtype(stats[stat]):
            if stat.endswith("_PCT"):
                weighted_sum += stats[stat].mean() * weight
            else:
                weighted_sum += stats[stat].mean() * weight
    return {"weighted_average": weighted_sum}

def get_games_on_date(date_str):
    """Gets the games on a specific date."""
    try:
        game_date = datetime.strptime(date_str, '%Y-%m-%d').strftime('%Y-%m-%d')
        scoreboard = scoreboardv2.ScoreboardV2(game_date=game_date)
        games_df = scoreboard.get_data_frames()[0]

        games = []
        for index, row in games_df.iterrows():
            games.append({
                "game_id": row["GAME_ID"],
                "game_date": row["GAME_DATE_EST"],
                "home_team_id": row["HOME_TEAM_ID"],
                "visitor_team_id": row["VISITOR_TEAM_ID"],
                "game_status_text": row["GAME_STATUS_TEXT"]
            })
        return games

    except requests.exceptions.RequestException as e:
        print(f"Error fetching scoreboard: {e}")
        return []
    except ValueError as e:
        print(f"Invalid date format {e}")
        return []

def get_live_scoreboard():
    """Gets the live scoreboard for today."""
    today = datetime.today().strftime('%Y-%m-%d')
    return get_games_on_date(today)

# --- API Endpoints ---

@app.route('/api/player/<int:player_id>/starting_price', methods=['GET'])
def get_player_starting_price(player_id):
    """Gets the starting price for a player."""
    player_stats = get_player_game_log(player_id)
    return jsonify({'starting_price': get_player_starting_price_from_stats(player_stats)})

@app.route('/api/player/<int:player_id>/stats', methods=['GET'])
def get_player_stats(player_id):
    """Gets stats for a player by name, with rolling average option."""
    rolling = request.args.get('rolling', 'false').lower() == 'true'

    game_log_df = get_player_game_log(player_id)
    weighted_average = calculate_weighted_average(game_log_df, rolling=rolling)
    game_log_json = game_log_df.to_json(orient="records")
    result = {
        "weighted_average": weighted_average,
        'gamelog': game_log_json
    }
    return jsonify(result)

@app.route('/api/players', methods=['GET'])
def get_all_players():
    active_players = players.get_active_players()
    simplified_players = [{'id': p['id'], 'full_name': p['full_name']} for p in active_players]
    return jsonify(simplified_players)

@app.route('/api/games', methods=['GET'])
def get_games():
    """Gets games for a specific date (or today if no date provided)."""
    date_str = request.args.get('date', datetime.today().strftime('%Y-%m-%d'))
    games = get_games_on_date(date_str)
    return jsonify(games)

@app.route('/api/games/live', methods=['GET'])
def get_live_games():
    """Gets the live scoreboard for today."""
    scoreboard = get_live_scoreboard()
    return jsonify(scoreboard)

if __name__ == '__main__':
    app.run(debug=True, port=5000)