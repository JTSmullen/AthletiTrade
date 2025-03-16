package com.athletitrade.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.dao.TradeDao;
import com.athletitrade.model.Player;
import com.athletitrade.model.Trade;

@Service
public class GameLogicService {

    private static final Logger log = LoggerFactory.getLogger(GameLogicService.class);

    private final PlayerService playerService;
    private final PlayerDao playerDao;
    private final TradeDao tradeDao;

    // Create GameLogicService on launch
    @Autowired
    public GameLogicService(PlayerService playerService, PlayerDao playerDao, TradeDao tradeDao) {
        this.playerService = playerService;
        this.playerDao = playerDao;
        this.tradeDao = tradeDao;
    }

    @Scheduled(fixedRate = 3600000) // Run every hour (3600000 milliseconds)
    @Transactional
    public void updatePlayerPrices() {
        log.info("GameLogicService: updatePlayerPrices() - Starting price update process.");

        // Fetch all players
        List<Player> allPlayers = (List<Player>) playerDao.findAll();

        // Iterate through each player and update their price
        for (Player player : allPlayers) {
            try {
                updatePlayerPrice(player);
            } catch (Exception e) { // Catch error and continue the process
                log.error("Error updating price for player ID: {}", player.getPlayerId(), e);
                // Consider if you want to continue processing other players or stop here
            }
        }

        // Log the completion message
        log.info("GameLogicService: updatePlayerPrices() - Price update process completed.");
    }

    private void updatePlayerPrice(Player player) {
        int playerId = player.getPlayerId();

        // Fetch the list of trades for this player
        List<Trade> playerTrades = tradeDao.findByPlayerId(playerId);

        // Find the buy and sell volumes
        Map<String, Integer> tradeVolumes = calculateTradeVolumes(playerTrades);
        int buyVolume = tradeVolumes.getOrDefault("BUY", 0);
        int sellVolume = tradeVolumes.getOrDefault("SELL", 0);

        // update the players price on these volumes
        playerService.updatePlayerPrice(playerId, buyVolume, sellVolume);
    }

    public Map<String, Integer> calculateTradeVolumes(List<Trade> trades) {
        // calculate the trading volumes
        Map<String, Integer> tradeVolumes = new HashMap<>();
        tradeVolumes.put("BUY", 0);
        tradeVolumes.put("SELL", 0);

        // If trade was a buy increment buys if sell increment sells by the quantity
        for (Trade trade : trades) {
            String tradeType = trade.getTradeType();
            int quantity = trade.getQuantity();

            if ("BUY".equalsIgnoreCase(tradeType)) {
                tradeVolumes.put("BUY", tradeVolumes.get("BUY") + quantity);
            } else if ("SELL".equalsIgnoreCase(tradeType)) {
                tradeVolumes.put("SELL", tradeVolumes.get("SELL") + quantity);
            } else {
                log.warn("Unknown trade type: {} for trade ID: {}", tradeType, trade.getTradeId());
            }
        }

        // Return HashMap with these values
        return tradeVolumes;
    }
}