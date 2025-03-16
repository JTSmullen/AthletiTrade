package com.athletitrade.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.athletitrade.model.Player;
import com.athletitrade.model.Trade;

@Service
public class GameLogicService {

    public void updatePlayerPrices() {
        // **TODO:** Implement price update algorithm here
        System.out.println("GameLogicService: updatePlayerPrices() - Price update logic to be implemented.");
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

    private Map<String, Integer> calculateTradeVolumes(List<Trade> trades) {
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