from flask import Flask, jsonify, request
from nba_api.stats.static import players, teams
from nba_api.stats.endpoints import playergamelog, scoreboardv2, commonplayerinfo
# Removed: from nba_api.stats.library.parameters import GameDate  # No longer needed
import pandas as pd
from datetime import datetime, timedelta
import requests

app = Flask(__name__)

# --- Helper Functions ---

def get_player_id_by_name(player_name):
    """Finds a player ID by their full name."""
    try:
        nba_players = players.get_players()
        player = [player for player in nba_players if player['full_name'] == player_name][0]
        return player['id']
    except IndexError:
        return None

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
        today = datetime.today()
        one_year_ago = today - timedelta(days=365)
        date_from = one_year_ago.strftime('%m/%d/%Y')  # Format for nba_api
        date_to = today.strftime('%m/%d/%Y')

        game_log = playergamelog.PlayerGameLog(player_id=player_id, date_from_nullable=date_from, date_to_nullable=date_to)
        df = game_log.get_data_frames()[0]
        return df
    except requests.exceptions.RequestException as e:
        print(f"Error fetching game log: {e}")
        return pd.DataFrame()

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

    # *Only* these weights are used:
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
        "FG_PCT": 0.04,  # Corrected to FG_PCT
    }

    weighted_sum = 0
    for stat, weight in weights.items():
        if stat in stats.columns and pd.api.types.is_numeric_dtype(stats[stat]):
            # Handle percentages correctly
            if stat.endswith("_PCT"):
                weighted_sum += stats[stat].mean() * weight
            else:
                weighted_sum += stats[stat].mean() * weight
        # No 'else' condition:  If the stat isn't present or isn't numeric, we *ignore* it.

    return {"weighted_average": weighted_sum}

def get_games_on_date(date_str):
    """Gets the games on a specific date."""
    try:
        # Format the date string to YYYY-MM-DD for the API
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
    except ValueError as e: # Handles Invalid date input
        print(f"Invalid date format {e}")
        return []

def get_live_scoreboard():
    """Gets the live scoreboard for today."""
    today = datetime.today().strftime('%Y-%m-%d')
    return get_games_on_date(today)

# --- API Endpoints ---

@app.route('/api/player/<player_name>/stats', methods=['GET'])
def get_player_stats(player_name):
    """Gets stats for a player by name, with rolling average option."""
    player_id = get_player_id_by_name(player_name)
    if player_id is None:
        return jsonify({'error': 'Player not found'}), 404

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