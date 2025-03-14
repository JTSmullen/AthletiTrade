from flask import Flask, jsonify
from nba_api.stats.endpoints import playercareerstats, commonplayerinfo, teamdashboardbygeneralsplits, playergamelog
from nba_api.live.nba.endpoints import scoreboard
from nba_api.stats.static import players
import json

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

import pandas as pd

def format_game_log_as_table(game_log_data):
    if "error" in game_log_data:
        print(game_log_data["error"])
        return

    if not game_log_data['resultSets'] or not game_log_data['resultSets'][0]['rowSet']:
        print("No game log data found.")
        return

    headers = game_log_data['resultSets'][0]['headers']
    rows = game_log_data['resultSets'][0]['rowSet']

    df = pd.DataFrame(rows, columns=headers)
    print(df)

game_log_data = fetch_player_game_log(1630560)
format_game_log_as_table(game_log_data)