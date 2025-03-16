package com.athletitrade.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.athletitrade.AthletiTradeApp;
import com.athletitrade.config.AppConfig;
import com.athletitrade.dao.PlayerDao;
import com.athletitrade.dao.PriceHistoryDao;
import com.athletitrade.model.Player;
import com.athletitrade.model.PriceHistory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PlayerService {

    private final AthletiTradeApp athletiTradeApp;

    private final AppConfig appConfig;

    private final PlayerDao playerDao; // Player Database connectivity
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final PriceCalculationUtils priceUtils;

    private final PriceHistoryDao priceHistoryDao;

    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private static final String PYTHON_API_BASE_URL = "http://localhost:5000"; // Python API URL | upon build will not
    // be on localhost

    // Autowired inits on build
    @Autowired
    public PlayerService(PlayerDao playerDao, RestTemplate restTemplate, ObjectMapper objectMapper,
            AppConfig appConfig, AthletiTradeApp athletiTradeApp, PriceHistoryDao priceHistoryDao) {
        this.playerDao = playerDao;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.priceUtils = new PriceCalculationUtils();
        this.priceHistoryDao = priceHistoryDao;
        this.appConfig = appConfig;
        this.athletiTradeApp = athletiTradeApp;
    }

    @Transactional
    public Player savePlayer(Player player) {
        return playerDao.save(player);
    }

    public Player getPlayerById(int playerId) {
        Optional<Player> playerOptional = playerDao.findById(playerId);
        if (playerOptional.isPresent()) {
            return playerOptional.get();
        }
        throw new NoSuchElementException("Player not found with ID: " + playerId);
    }

    @Transactional // Add transactional to the whole method
    public List<Player> fetchPlayers() {
        String playersListUrl = PYTHON_API_BASE_URL + "/players/list";
        List<Player> players = new ArrayList<>(); // Accumulate players for batch saving

        try {
            String playersJson = restTemplate.getForObject(playersListUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(playersJson);
            JsonNode playersListNode = rootNode.get("player");

            if (playersListNode != null && playersListNode.isArray()) {
                for (JsonNode playerNode : playersListNode) {
                    int playerId = playerNode.get("id").asInt();
                    JsonNode gameStats = fetchPlayerGameStats(playerId);

                    double currentPrice = 0.0;
                    if (gameStats != null) {
                        JsonNode resultSets = gameStats.get("resultSets");
                        if (resultSets != null) {
                            currentPrice = playerStartingPriceCalculation(resultSets);
                        } else {
                            log.error("'resultSets' not found for player ID {}", playerId); // Use logging!
                        }
                    } else {
                        log.error("Could not fetch game stats for player ID {}", playerId); // Use logging!
                    }

                    log.info("{}: ${}", playerNode.get("full_name").asText(), currentPrice);

                    Player player = new Player();
                    player.setPlayerId(playerId);
                    player.setPlayerName(playerNode.get("full_name").asText());
                    player.setTeamId(null); // Get from API if available
                    player.setPosition(null); // Get from API if available
                    player.setCurrentPrice(new BigDecimal(currentPrice)); // Use BigDecimal
                    players.add(player);
                    // savePlayer(player); // remove

                    Thread.sleep(2200); // Basic rate limiting
                }
                playerDao.saveAll(players); // Batch save *after* the loop
                return players; // Return players
            }
        } catch (IOException e) {
            log.error("Error fetching or parsing player list from Python API", e); // Use logging
        } catch (InterruptedException e) { // Handle InterruptedException
            log.error("Thread interrupted during sleep", e);
            Thread.currentThread().interrupt(); // Restore interrupted status!
        } catch (Exception e) {
            log.error("General error fetching player list", e); // Use logging!
        }
        return Collections.emptyList(); // Return an empty list instead of null on error
    }

    public JsonNode fetchPlayerGameStats(int playerId) {
        String gameLogUrl = PYTHON_API_BASE_URL + "/players/" + playerId + "/game_log"; // Endpoint URL

        try {
            String gameLogJson = restTemplate.getForObject(gameLogUrl, String.class); // ping python_api and store
            // response

            return objectMapper.readTree(gameLogJson); // return json format of game data

            // Exceptions
        } catch (IOException e) {
            System.err.println(
                    "Error fetching game log for player ID " + playerId + " from Python API: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error fetching game log for player ID " + playerId + ": " + e.getMessage());
        }
        return null;
    }

    /*
     * TODO: This method is not complete for real json response. It is a test
     * method. Change for real Json response
     */
    public Player fetchAndUpdatePlayerInfo(int playerId) {
        String playerInfoUrl = PYTHON_API_BASE_URL + "/players/" + playerId + "/info"; // Endpoint URL

        try {
            String playerInfoJson = restTemplate.getForObject(playerInfoUrl, String.class);
            JsonNode playerInfoNode = objectMapper.readTree(playerInfoJson);

            if (playerInfoNode != null) {
                JsonNode playerInfoResult = playerInfoNode.get("resultSets").get(0).get("rowSet").get(0);

                String playerName = playerInfoResult.get(1).asText(); // PLAYER_NAME
                int teamId = playerInfoResult.get(7).asInt(); // TEAM_ID
                String position = playerInfoResult.get(14).asText(); // POSITION

                Player player = getPlayerById(playerId); // throws exception if not found
                if (player == null) {
                    player = new Player();
                    player.setPlayerId(playerId);
                }
                player.setPlayerName(playerName);
                player.setTeamId(teamId);
                player.setPosition(position);
                player.setCurrentPrice(BigDecimal.valueOf(10.00)); // GameLogicService.updatePlayerPrices call

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

    @Transactional
    public void updatePlayerPrice(int playerId, int buyVolume, int sellVolume) {
        Optional<Player> playerOptional = playerDao.findById(playerId);
        if (!playerOptional.isPresent()) {
            log.error("Player not found with ID: {}", playerId);
            return;
        }

        JsonNode gameStats = fetchPlayerGameStats(playerId);
        if (gameStats == null) {
            log.error("Failed to fetch game stats for player: {}", playerId);
            return;
        }

        JsonNode resultSets = gameStats.get("resultSets").get(0);
        if (resultSets == null) {
            log.error("'resultSets' not found for player ID", playerId);
            return;
        }

        Map<String, Double> rollingAvgStats = priceUtils.JsonColumnAveragerrolling(resultSets, 20);
        double newPerformanceWeight = priceUtils.weightedPlayerPerformance(rollingAvgStats);

        Player player = playerOptional.get();
        BigDecimal currentPrice = player.getCurrentPrice();
        BigDecimal performancePriceChange = priceUtils.calculatePriceChange(newPerformanceWeight, currentPrice);

        BigDecimal volumePriceChange = priceUtils.calculateVolumePriceChange(buyVolume, sellVolume);

        BigDecimal totalChange = performancePriceChange.add(volumePriceChange);
        BigDecimal newPrice = currentPrice.add(totalChange);

        if (newPrice.compareTo(BigDecimal.ZERO) < 0) {
            newPrice = BigDecimal.ZERO;
        }

        player.setCurrentPrice(newPrice);
        playerDao.save(player);

        PriceHistory priceHistory = new PriceHistory(playerId, newPrice, buyVolume, sellVolume);
        priceHistory.setTimestamp(Timestamp.from(Instant.now()));
        priceHistoryDao.save(priceHistory);

        log.info("Updated price for player {}: Old Price: {}, New Price: {}, Performance Change: {}, Volume Change: {}",
                playerId, currentPrice, newPrice, performancePriceChange, volumePriceChange);
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

        public double calculatePriceFromWeight(double weight) {
            double price = weight * 10;
            if (price <= 0) {
                Random rand = new Random();
                price = rand.nextDouble(.25, .5);
            }
            return Math.round(price * 100.0) / 100.0;
        }

        public double weightedPlayerPerformance(Map<String, Double> avgPlayerStats) {
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
                System.err.println(
                        "Input JsonNode is not in the expected format (missing rowSet or headers array). Cannot average columns");
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
                            // TODO: Error logging but me lazy rn
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

        public Map<String, Double> JsonColumnAveragerrolling(JsonNode playerGameStats, int windowSize) {
            Map<String, Double> columnSum = new HashMap<>();
            Map<String, Integer> columnCounts = new HashMap<>();
            Map<String, Double> columnAverages = new HashMap<>();

            JsonNode rowSetNode = playerGameStats.get("rowSet");
            JsonNode headersNode = playerGameStats.get("headers");

            if (!rowSetNode.isArray() || !headersNode.isArray()) {
                System.err.println(
                        "Input JsonNode is not in the expected format (missing rowSet or headers array). Cannot average columns");
                return columnAverages; // return empty map
            }

            List<String> headers = new ArrayList<>();
            for (JsonNode headerNode : headersNode) {
                headers.add(headerNode.asText());
            }

            // Ensure we only process the last 'rollingMean' number of rows
            int startIndex = Math.max(0, rowSetNode.size() - windowSize);
            for (int rowIdx = startIndex; rowIdx < rowSetNode.size(); rowIdx++) {
                JsonNode rowNode = rowSetNode.get(rowIdx);
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
                            // TODO: Error logging but me lazy rn
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

        public BigDecimal calculatePriceChange(double newWeight, BigDecimal currentPrice) {
            double newPrice = calculatePriceFromWeight(newWeight);
            BigDecimal newPriceBigDecimal = BigDecimal.valueOf(newPrice);
            BigDecimal priceDifference = newPriceBigDecimal.subtract(currentPrice);
            BigDecimal percentageChange = priceDifference.divide(currentPrice, 4, RoundingMode.HALF_UP);
            return percentageChange.multiply(currentPrice);
        }

        public BigDecimal calculateVolumePriceChange(int buyVolume, int sellVolume) {
            int netVolume = buyVolume - sellVolume;
            BigDecimal volumeEffect;

            if (netVolume > 0) {
                // More buying than selling, price will be driven up to balance
                volumeEffect = BigDecimal.valueOf(0.01 * netVolume);
            } else if (netVolume < 0) {
                // More selling than buying, price will be driven down to balance
                volumeEffect = BigDecimal.valueOf(0.01 * netVolume);
            } else {
                // Balanced, no change
                volumeEffect = BigDecimal.ZERO;
            }
            return volumeEffect;
        }
    }
}