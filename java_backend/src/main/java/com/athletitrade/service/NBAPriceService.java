package com.athletitrade.service;

import com.athletitrade.dto.PlayerStatsDto;
import com.athletitrade.model.Player;
import com.athletitrade.model.PlayerPriceHistory;
import com.athletitrade.repository.PlayerPriceHistoryRepository;
import com.athletitrade.repository.PlayerRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NBAPriceService {

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;

    @Autowired
    private OkHttpClient httpClient;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String PYTHON_API_BASE_URL = "http://127.0.0.1:5000/api";

    private final Map<Integer, Double> cachedPrices = new ConcurrentHashMap<>();

    public double calculatePrice(PlayerStatsDto stats) {
        double price = (stats.getPTS() * 2) + (stats.getREB() * 1.5) + (stats.getAST() * 1.8) - (stats.getTOV() * 0.8);
        return Math.max(0.0, price);
    }

//    @Transactional
//    @Scheduled(fixedRate = 600000)
//    public void updatePrices() {
//        System.out.println("Updating player prices...");
//        List<Player> allPlayers = playerRepository.findAll();
//
//        for (Player player : allPlayers) {
//            try {
//                String url = PYTHON_API_BASE_URL + "/player/" + player.getFullName() + "/stats?rolling=true";
//                Request request = new Request.Builder().url(url).build();
//
//                try (Response response = httpClient.newCall(request).execute()) {
//                    if (!response.isSuccessful()) {
//                        throw new IOException("Unexpected code " + response);
//                    }
//
//                    PlayerStatsDto stats = objectMapper.readValue(response.body().string(), PlayerStatsDto.class);
//
//                    if (stats != null && stats.getWeightedAverage() != null) {
//                        double newPrice = calculatePrice(stats);
//                        Double lastPrice = cachedPrices.get(player.getId());
//                        if (lastPrice == null || Math.abs(newPrice - lastPrice) > 0.01) {
//                            cachedPrices.put(player.getId(), newPrice);
//                            PlayerPriceHistory history = new PlayerPriceHistory();
//                            history.setPlayer(player);
//                            history.setPrice(newPrice);
//                            history.setTimestamp(LocalDateTime.now());
//                            playerPriceHistoryRepository.save(history);
//                        }
//                    }
//                }
//            } catch (IOException e) {
//                System.err.println("Error getting stats for: " + player.getFullName() + ": " + e.getMessage());
//            }
//        }
//        System.out.println("Player prices updated.");
//    }

    public double getCurrentPrice(Integer playerId) {
        Double cachedPrice = cachedPrices.get(playerId);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        List<PlayerPriceHistory> history = playerPriceHistoryRepository.findByPlayerIdOrderByTimestampDesc(playerId);
        if (!history.isEmpty()) {
            double price = history.get(0).getPrice();
            cachedPrices.put(playerId, price);
            return price;
        }

        throw new IllegalArgumentException("No price history found for player ID: " + playerId);
    }

    public double getStartingPriceFromPythonApi(Integer playerId) {
        try {
            String url = PYTHON_API_BASE_URL + "/player/" + playerId + "/starting_price";
            Request request = new Request.Builder().url(url).build();
            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response + " for player ID " + playerId);
                }

                String responseBody = response.body().string();
                Map<String, Object> responseMap = objectMapper.readValue(responseBody, new TypeReference<Map<String, Object>>() {});
                Object startingPriceObj = responseMap.get("starting_price");

                if (startingPriceObj == null) {
                    throw new RuntimeException("starting_price not found in response for player ID " + playerId);
                }

                double startingPrice;
                if (startingPriceObj instanceof Number) {
                    startingPrice = ((Number) startingPriceObj).doubleValue();
                } else {
                    throw new RuntimeException("starting_price is not a number for player ID " + playerId + ". Value: " + startingPriceObj);
                }

                return Math.round(startingPrice * 100.0) / 100.0;
            } catch (IOException e) {
                System.err.println("Error getting starting price for player ID " + playerId + ": " + e.getMessage());
                throw new RuntimeException("Error getting starting price for player ID " + playerId, e);
            }
        }catch (Exception e){
            System.out.println("Could not get intial value, please try again");
            throw e;
        }
    }
}