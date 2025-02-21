import org.json.JSONArray;
import org.json.JSONObject;

public class testAthletiTradeLogic {

    public static void main(String[] args) throws Exception {
        NbaDataService nbaDataService = new NbaDataService();
        String playerID = nbaDataService.getPlayerID("Stephen Curry");

        if (playerID != null) {
            String gameLog = nbaDataService.getPlayerGameLog(playerID, "2024");
            printGameLogNicely(gameLog);
        } else {
            System.out.println("Player not found");
        }
    }

    public static void printGameLogNicely(String gameLogJson) {
        try {
            JSONObject gameLog = new JSONObject(gameLogJson);
            JSONArray resultSets = gameLog.getJSONArray("resultSets");

            if (resultSets.length() > 0) {
                JSONObject playerGameLog = resultSets.getJSONObject(0);
                JSONArray headers = playerGameLog.getJSONArray("headers");
                JSONArray rowSet = playerGameLog.getJSONArray("rowSet");

                System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------");
                System.out.printf("%-10s %-12s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s %-5s%n",
                        headers.getString(3), headers.getString(4), headers.getString(5), headers.getString(6), headers.getString(7), headers.getString(8), headers.getString(9), headers.getString(10), headers.getString(11), headers.getString(12), headers.getString(13), headers.getString(14), headers.getString(15), headers.getString(16), headers.getString(17), headers.getString(18), headers.getString(19), headers.getString(20), headers.getString(21), headers.getString(22), headers.getString(23), headers.getString(24));
                System.out.println("-------------------------------------------------------------------------------------------------------------------------------------------------------");

                for (int i = 0; i < rowSet.length(); i++) {
                    JSONArray game = rowSet.getJSONArray(i);
                    System.out.printf("%-10s %-12s %-5s %-5d %-5d %-5.3f %-5d %-5d %-5.3f %-5d %-5d %-5.3f %-5d %-5d %-5d %-5d %-5d %-5d %-5d %-5d %-5d %-5d%n",
                            game.getString(3), game.getString(4), game.getString(5), game.getInt(6), game.getInt(7), game.getDouble(8), game.getInt(9), game.getInt(10), game.getDouble(11), game.getInt(12), game.getInt(13), game.getDouble(14), game.getInt(15), game.getInt(16), game.getInt(17), game.getInt(18), game.getInt(19), game.getInt(20), game.getInt(21), game.getInt(22), game.getInt(23), game.getInt(24));
                }
            } else {
                System.out.println("No game log data found.");
            }

        } catch (Exception e) {
            System.err.println("Error parsing game log: " + e.getMessage());
        }
    }
}