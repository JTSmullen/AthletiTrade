import unittest
import json
from datetime import datetime, timedelta
import pandas as pd
from nba_data_api import app, get_player_id_by_name, get_team_id_by_name, get_player_game_log, calculate_weighted_average, get_games_on_date, get_live_scoreboard # Import your app and functions

class NBADateAPITestCase(unittest.TestCase):

    def setUp(self):
        """Setup method to create a test client before each test."""
        self.app = app.test_client()
        self.app.testing = True  # Enable testing mode

    def tearDown(self):
        """Teardown method executed after each test."""
        pass  # Add any cleanup code here if necessary

    # --- Helper Function Tests ---

    def test_get_player_id_by_name(self):
        player_id = get_player_id_by_name("LeBron James")
        self.assertIsNotNone(player_id)
        self.assertEqual(player_id, 2544)  # Known ID for LeBron James

        player_id_not_found = get_player_id_by_name("Nonexistent Player")
        self.assertIsNone(player_id_not_found)

    def test_get_team_id_by_name(self):
        team_id = get_team_id_by_name("Los Angeles Lakers")
        self.assertIsNotNone(team_id)
        self.assertEqual(team_id, 1610612747)  # Known ID for Lakers

        team_id_abbr = get_team_id_by_name("LAL")  # Test abbreviation
        self.assertEqual(team_id_abbr, 1610612747)

        team_id_not_found = get_team_id_by_name("Nonexistent Team")
        self.assertIsNone(team_id_not_found)

    def test_get_player_game_log(self):
        player_id = get_player_id_by_name("LeBron James")
        game_log_df = get_player_game_log(player_id)
        self.assertIsInstance(game_log_df, pd.DataFrame)
        self.assertFalse(game_log_df.empty)  # Should not be empty

        #Test for a player id that doesn't exist
        game_log_empty = get_player_game_log(999999)  # Invalid ID
        self.assertTrue(game_log_empty.empty)

    def test_calculate_weighted_average(self):
        # Create a sample DataFrame for testing
        data = {
            'AST': [10, 8, 12],
            'REB': [5, 7, 6],
            'PLUS_MINUS': [2, -1, 3],
            'TOV': [2, 3, 1],
            'STL': [1, 2, 1],
            'BLK': [0, 1, 2],
            'PTS': [25, 30, 28],
            'FGA': [20, 22, 18],
            'FG3_PCT': [0.4, 0.35, 0.45],
            'FG_PCT': [0.5, 0.55, 0.6],
            'NON_EXISTENT_STAT': [1,2,3]  # Test for non existent stats
        }
        stats_df = pd.DataFrame(data)
        # Test with rolling average
        rolling_result = calculate_weighted_average(stats_df, rolling=True, last_n_games=3)
        self.assertIsInstance(rolling_result, dict)
        self.assertIn("weighted_average", rolling_result)
        self.assertTrue(rolling_result["weighted_average"] > 0) #Should be positive

        # Test without rolling average
        non_rolling_result = calculate_weighted_average(stats_df)
        self.assertIsInstance(non_rolling_result, dict)
        self.assertIn("weighted_average", non_rolling_result)
        self.assertTrue(non_rolling_result["weighted_average"] > 0) #Should be positive

        #Test empty Dataframe
        empty_df = pd.DataFrame()
        empty_result = calculate_weighted_average(empty_df)
        self.assertEqual(empty_result, {})

    def test_get_games_on_date(self):
        # Test with a known date that had games (e.g., 2023-10-26 - Adjust as needed)
        games = get_games_on_date("2023-10-26")
        self.assertIsInstance(games, list)
        self.assertTrue(len(games) > 0)  # Assuming there were games on that date

        #Test with a date with no games
        games_empty = get_games_on_date("2023-08-01") # No games in August usually
        self.assertIsInstance(games_empty, list)
        self.assertEqual(len(games_empty), 0)  # Should be empty

    def test_get_live_scoreboard(self):
        scoreboard = get_live_scoreboard()
        self.assertIsInstance(scoreboard, list)
        # We can't guarantee there will be games *today*, so we just check the type.

    # --- API Endpoint Tests ---

    def test_get_player_stats_endpoint(self):
        # Test with a valid player name
        response = self.app.get('/api/player/LeBron%20James/stats')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.get_data(as_text=True))
        self.assertIn('weighted_average', data)
        self.assertIn('gamelog', data)

        # Test with rolling average
        response_rolling = self.app.get('/api/player/LeBron%20James/stats?rolling=true')
        self.assertEqual(response_rolling.status_code, 200)
        data_rolling = json.loads(response_rolling.get_data(as_text=True))
        self.assertIn('weighted_average', data_rolling)

        # Test with an invalid player name
        response_invalid = self.app.get('/api/player/Invalid%20Player/stats')
        self.assertEqual(response_invalid.status_code, 404)  # Expect 404 Not Found

    def test_get_all_players_endpoint(self):
        response = self.app.get('/api/players')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.get_data(as_text=True))
        self.assertIsInstance(data, list)
        # Check if the list contains dictionaries with 'id' and 'full_name'
        if data:  # Check if the list is not empty
            self.assertIn('id', data[0])
            self.assertIn('full_name', data[0])

    def test_get_games_endpoint(self):
        # Test with a specific date
        response = self.app.get('/api/games?date=2023-10-26')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.get_data(as_text=True))
        self.assertIsInstance(data, list)

        #Test without a date, should give todays
        response_today = self.app.get('/api/games')
        self.assertEqual(response_today.status_code, 200)
        data_today = json.loads(response_today.get_data(as_text=True))
        self.assertIsInstance(data_today, list)

    def test_get_live_games_endpoint(self):
        response = self.app.get('/api/games/live')
        self.assertEqual(response.status_code, 200)
        data = json.loads(response.get_data(as_text=True))
        self.assertIsInstance(data, list)


if __name__ == '__main__':
    unittest.main()