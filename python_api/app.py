from flask import Flask, jsonify
from nba_api.stats.endpoints import playercareerstats, commonplayerinfo, teamdashboardbygeneralsplits, playergamelog
from nba_api.live.nba.endpoints import scoreboard
from nba_api.stats.static import players
import json

app = Flask(__name__)

def fetch_player_stats(player_id):
    """Fetches career stats for a given player ID."""
    try:
        career_stats = playercareerstats.PlayerCareerStats(player_id=player_id)
        career_stats_json = career_stats.get_json()
        return json.loads(career_stats_json)
    except Exception as e:
        print(f"Error fetching stats for player ID {player_id}: {e}")
        return None

def fetch_player_info(player_id):
    """Fetches general player information."""
    try:
        player_info = commonplayerinfo.CommonPlayerInfo(player_id=player_id)
        player_info_json = player_info.get_json()
        return json.loads(player_info_json)
    except Exception as e:
        return None

def fetch_live_games():
    """Fetches data for live and upcoming NBA games."""
    try:
        live_score = scoreboard.ScoreBoard()
        live_score_json = live_score.get_json()
        return json.loads(live_score_json)
    except Exception as e:
        print(f"Error fetching live games: {e}")
        return None

def fetch_player_game_log(player_id, season="2024"):
    if not isinstance(player_id, int) or player_id <= 0:
        return {"error": "Invalid player ID"}

    try:
        game_log = playergamelog.PlayerGameLog(player_id=player_id, season=season)


        if game_log is None:
            return {"error": "Player game log not found"}

        game_log_json = game_log.get_json()
        game_log_data = json.loads(game_log_json)

        if not game_log_data['resultSets'] or not game_log_data['resultSets'][0]['rowSet']:
            return {"error": "Player game log not found"}

        return game_log_data

    except Exception as e:
        return {"error": "Error fetching player game log"}


@app.route('/players/<int:player_id>/game_log', methods=['GET'])
def get_player_game_log_route(player_id):
    """API endpoint to get player game log for the current season."""
    game_log = fetch_player_game_log(player_id)
    if "error" in game_log:
        if game_log["error"] == "Invalid player ID":
            return jsonify(game_log), 400
        elif game_log["error"] == "Player game log not found":
             return jsonify(game_log), 404
        else:
            return jsonify(game_log), 500
    
    if game_log:
        return jsonify(game_log)
    
    return jsonify({"error": "Player game log not found"}), 404


@app.route('/players/<int:player_id>/stats', methods=['GET'])
def get_player_stats_route(player_id):
    """API endpoint to get player career stats."""
    stats = fetch_player_stats(player_id)
    if stats:
        return jsonify(stats)
    return jsonify({"error": "Player stats not found"}), 404

@app.route('/players/<int:player_id>/info', methods=['GET'])
def get_player_info_route(player_id):
    """API endpoint to get player information."""
    info = fetch_player_info(player_id)
    if info:
        return jsonify(info)
    return jsonify({"error": "Player info not found"}), 404

@app.route('/live_games', methods=['GET'])
def get_live_games_route():
    """API endpoint to get live game data."""
    live_games = fetch_live_games()
    if live_games:
        return jsonify(live_games)
    return jsonify({"error": "Live game data not found"}), 404

@app.route('/players/list', methods=['GET'])
def get_players_list():
    active_players = []
    all_players = players.get_players()
    for player in all_players:
        if player['is_active']:
            active_players.append(player)
    if active_players is not None:
        return jsonify({'player': active_players})
    else:
        print("No active players found")
        return jsonify({'error': "Active players not found"}), 404


if __name__ == '__main__':
    app.run(debug=True, port=5000)