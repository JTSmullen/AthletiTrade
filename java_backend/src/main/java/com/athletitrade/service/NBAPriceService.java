package com.athletitrade.service;

import com.athletitrade.dto.PlayerStatsDto;
import com.athletitrade.model.Player;
import com.athletitrade.model.PlayerPriceHistory;
import com.athletitrade.repository.PlayerPriceHistoryRepository;
import com.athletitrade.repository.PlayerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NBAPriceService {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;

    // Cache player prices to avoid redundant calculations
    private final Map<Integer, Double> cachedPrices = new ConcurrentHashMap<>();


    // This is the core pricing algorithm!  It needs substantial refinement.
    public double calculatePrice(PlayerStatsDto stats) {
        // VERY basic example - you'll need to replace this with a sophisticated formula!
        double price = (stats.getPTS() * 2) + (stats.getREB() * 1.5) + (stats.getAST() * 1.8) - (stats.getTOV()*0.8);
        return Math.max(0.1, price); // Ensure price is not negative
    }


    @Transactional
    @Scheduled(fixedRate = 600000) // Run every 60 seconds.  Adjust as needed!
    public void updatePrices() {
        System.out.println("Updating player prices...");
        List<Player> allPlayers = playerService.getAllPlayers();

        for (Player player : allPlayers) {
            try {
                PlayerStatsDto stats = playerService.getPlayerStats(player.getFullName(), true);
                if (stats != null && stats.getWeightedAverage() != null) { //Basic Null check for when player hasnt played
                    double newPrice = calculatePrice(stats);

                    // Check if the price has changed significantly (to avoid unnecessary updates)
                    Double lastPrice = cachedPrices.get(player.getId());
                    if (lastPrice == null || Math.abs(newPrice - lastPrice) > 0.01) { // 0.01 is a threshold
                        cachedPrices.put(player.getId(), newPrice);

                        // Save price history
                        PlayerPriceHistory history = new PlayerPriceHistory();
                        history.setPlayer(player);
                        history.setPrice(newPrice);
                        history.setTimestamp(LocalDateTime.now());
                        playerPriceHistoryRepository.save(history);
                    }
                }
            } catch (Exception e){
                System.out.println("Error getting stats for: "+ player.getFullName());
            }
        }
        System.out.println("Player prices updated.");
    }

    public double getCurrentPrice(Integer playerId) {
        // First, try to get the price from the cache.
        Double cachedPrice = cachedPrices.get(playerId);
        if (cachedPrice != null) {
            return cachedPrice;
        }

        // If not in the cache, get the latest price from the database.
        List<PlayerPriceHistory> history = playerPriceHistoryRepository.findByPlayerIdOrderByTimestampDesc(playerId);
        if (!history.isEmpty()) {
            double price = history.get(0).getPrice();
            cachedPrices.put(playerId, price); // Update the cache
            return price;
        }

        // If no price history, we need to calculate an initial price.  This should ideally happen
        // when the player is first added to the system.
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid player ID: " + playerId));
        PlayerStatsDto stats = playerService.getPlayerStats(player.getFullName(), true);
        double initialPrice = calculatePrice(stats);

        // Save the initial price to the database and cache.
        PlayerPriceHistory historyEntry = new PlayerPriceHistory();
        historyEntry.setPlayer(player);
        historyEntry.setPrice(initialPrice);
        historyEntry.setTimestamp(LocalDateTime.now());
        playerPriceHistoryRepository.save(historyEntry);
        cachedPrices.put(playerId, initialPrice);

        return initialPrice;
    }
}