package com.athletitrade.service;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.model.Player;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.*;

@Service
public class PlayerService {

    private final PlayerDao playerDao; // Player Database connectivity
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final PriceCalculationUtils priceUtils;

    private static final String PYTHON_API_BASE_URL = "http://localhost:5000"; // Python API URL | upon build will not be on localhost

    // Autowired inits on build
    @Autowired
    public PlayerService(PlayerDao playerDao, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.playerDao = playerDao;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.priceUtils = new PriceCalculationUtils();
    }

    public Player getPlayerById(int playerId) {
        Optional<Player> playerOptional = playerDao.findById(playerId);
        return playerOptional.orElse(null);
    }

    public List<Player> fetchPlayers() {
        String playersListUrl = PYTHON_API_BASE_URL + "/players/list";

        try {
            String playersJson = restTemplate.getForObject(playersListUrl, String.class); // ping python_api and store response
            JsonNode rootNode = objectMapper.readTree(playersJson); // change the string json response to json format

            JsonNode playersListNode = rootNode.get("player"); // Get 'player' root from json response

            if (playersListNode != null && playersListNode.isArray()) {
                List<Player> players = new ArrayList<>();
                for (JsonNode playerNode : playersListNode) {
                    int playerId = playerNode.get("id").asInt(); // Get ID *before* the API call
                    JsonNode gameStats = fetchPlayerGameStats(playerId); // Get the game stats

                    double currentPrice = 0.0; // Default price if stats are unavailable
                    if (gameStats != null) { // Check if gameStats is not null
                        JsonNode resultSets = gameStats.get("resultSets");
                        if (resultSets != null) { // Check if "resultSets" exists
                            currentPrice = playerStartingPriceCalculation(resultSets);
                        } else {
                            System.err.println("Error: 'resultSets' not found for player ID " + playerId);
                        }
                    } else {
                        System.err.println("Error: Could not fetch game stats for player ID " + playerId);
                    }

                    System.out.println(playerNode.get("full_name").asText() + ": $" + currentPrice);

                    Player player = new Player();
                    player.setPlayerId(playerId);  // Use the pre-fetched ID
                    player.setPlayerName(playerNode.get("full_name").asText());
                    player.setTeamId(null); // Placeholder
                    player.setPosition(null); // Placeholder
                    player.setCurrentPrice(currentPrice); // Use the calculated or default price
                    players.add(player);

                    Thread.sleep(2000); // Rate limiting
                }
                return players;
            }

            // Exceptions
        } catch (IOException e) {
            System.err.println("Error fetching or parsing player list from Python API: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error fetching player list: " + e.getMessage());
        }
        return null;
    }

    public JsonNode fetchPlayerGameStats(int playerId) {
        String gameLogUrl = PYTHON_API_BASE_URL + "/players/" + playerId + "/game_log"; // Endpoint URL

        try {
            String gameLogJson = restTemplate.getForObject(gameLogUrl, String.class); // ping python_api and store response

            return objectMapper.readTree(gameLogJson); // return json format of game data

            // Exceptions
        } catch (IOException e) {
            System.err.println("Error fetching game log for player ID " + playerId + " from Python API: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error fetching game log for player ID " + playerId + ": " + e.getMessage());
        }
        return null;
    }

    /*
        TODO: This method is not complete for real json response. It is a test method. Change for real Json response
     */
    public Player fetchAndUpdatePlayerInfo(int playerId) {
        String playerInfoUrl = PYTHON_API_BASE_URL + "/players/" + playerId + "/info"; // Endpoint URL

        try {
            String playerInfoJson = restTemplate.getForObject(playerInfoUrl, String.class);
            JsonNode playerInfoNode = objectMapper.readTree(playerInfoJson);

            if (playerInfoNode != null) {
                JsonNode playerInfoResult = playerInfoNode.get("resultSets").get(0).get("rowSet").get(0);

                String playerName = playerInfoResult.get(1).asText(); // PLAYER_NAME
                int teamId = playerInfoResult.get(7).asInt();     // TEAM_ID
                String position = playerInfoResult.get(14).asText();  // POSITION

                Player player = getPlayerById(playerId);
                if (player == null) {
                    player = new Player();
                    player.setPlayerId(playerId);
                }
                player.setPlayerName(playerName);
                player.setTeamId(teamId);
                player.setPosition(position);
                player.setCurrentPrice(10.00); // GameLogicService.updatePlayerPrices call

                return playerDao.save(player);
            }
        } catch (IOException e) {
            System.err.println("Error fetching or parsing player info from Python API: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error fetching player info: " + e.getMessage());
        }
        return null;
    }


    public double playerStartingPriceCalculation(JsonNode playerGameStats) {
        Map<String, Double> statsAverages = priceUtils.JsonColumnAverager(playerGameStats.get(0));
        double weight = priceUtils.weightedPlayerPerformance(statsAverages);
        return priceUtils.calculatePriceFromWeight(weight);
    }

    final public static class PriceCalculationUtils {

        private final Map<String, Double> weights;

        public PriceCalculationUtils() {
            this.weights = new HashMap<>();
            this.weights.put("AST", 0.15);
            this.weights.put("REB", 0.12);
            this.weights.put("PLUS_MINUS", 0.08);
            this.weights.put("TOV", -0.05);
            this.weights.put("STL", 0.10);
            this.weights.put("BLK", 0.09);
            this.weights.put("PTS", 0.20);
            this.weights.put("FGA", -0.03);
            this.weights.put("3P_PCT", 0.10);
            this.weights.put("2P_PCT", 0.04);
        }

        public PriceCalculationUtils(Map<String, Double> initialWeights) {
            this.weights = new HashMap<>(initialWeights); // Defensive Copy
        }

        public double calculatePriceFromWeight(double weight){
            double price = weight * 10;
            if(price <= 0) {
                Random rand = new Random();
                price = rand.nextDouble(.25, .5);
            }
            return Math.round(price * 100.0) / 100.0;
        }

        public double weightedPlayerPerformance(Map<String, Double> avgPlayerStats){
            double weightedScore = 0.0;
            for (Map.Entry<String, Double> entry : avgPlayerStats.entrySet()) {
                String statName = entry.getKey();
                double statValue = entry.getValue();
                Double weight = weights.get(statName); // Use 'Double' not 'double' to handle nulls

                if (weight != null) {
                    weightedScore += statValue * weight; // Only add if stat is in weights
                } else {
                    // TODO: add a throw here to alert missing weight value
                    // unlikely this happens but good to have
                }
            }
            return weightedScore;
        }

        public Map<String, Double> JsonColumnAverager(JsonNode playerGameStats) {
            Map<String, Double> columnSum = new HashMap<>();
            Map<String, Integer> columnCounts = new HashMap<>();
            Map<String, Double> columnAverages = new HashMap<>();

            JsonNode rowSetNode = playerGameStats.get("rowSet");
            JsonNode headersNode = playerGameStats.get("headers");

            if (!rowSetNode.isArray() || !headersNode.isArray()) {
                System.err.println("Input JsonNode is not in the expected format (missing rowSet or headers array). Cannot average columns");
                return columnAverages; // return empty map
            }

            List<String> headers = new ArrayList<>();
            for (JsonNode headerNode : headersNode) {
                headers.add(headerNode.asText());
            }

            for (JsonNode rowNode : rowSetNode) {
                if (!rowNode.isArray()) {
                    System.err.println("Row in rowSet is not an array. Skipping row.");
                    continue;
                }
                for (int i = 0; i < headers.size(); i++) {
                    String headerName = headers.get(i);
                    if (i < rowNode.size()) {
                        JsonNode valueNode = rowNode.get(i);

                        if (valueNode.isNumber()) {
                            double value = valueNode.asDouble();
                            columnSum.put(headerName, columnSum.getOrDefault(headerName, 0.0) + value);
                            columnCounts.put(headerName, columnCounts.getOrDefault(headerName, 0));
                            columnCounts.put(headerName, columnCounts.get(headerName) + 1);
                        } else {
                            //TODO: Error logging but me lazy rn
                        }
                    } else {
                        // TODO: Error logging but me lazy rn
                    }
                }
            }
            for (Map.Entry<String, Double> entry : columnSum.entrySet()) {
                String columnName = entry.getKey();
                double sum = entry.getValue();
                int count = columnCounts.getOrDefault(columnName, 0);

                if (count > 0) {
                    columnAverages.put(columnName, sum / count);
                } else {
                    columnAverages.put(columnName, 0.0);
                }
            }
            return columnAverages;
        }
    }
}