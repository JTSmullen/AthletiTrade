package com.athletitrade.service;

import com.athletitrade.model.Player;
import com.athletitrade.model.PlayerPriceHistory;
import com.athletitrade.model.Trade;
import com.athletitrade.model.User;
import com.athletitrade.repository.PlayerPriceHistoryRepository;
import com.athletitrade.repository.PlayerRepository;
import com.athletitrade.repository.TradeRepository;
import com.athletitrade.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TradeService {

    @Autowired
    private TradeRepository tradeRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;

    @Autowired
    private NBAPriceService nbaPriceService; // For getting the current price


    @Transactional
    public Trade executeTrade(Long userId, Integer playerId, Trade.BuySell buySell, Integer quantity) {
        // Input validation
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive.");
        }
        // 1. Get the user and player.
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + userId));
        Player player = playerRepository.findById(playerId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid player ID: " + playerId));

        // 2. Get the current price of the player.
        double currentPrice = nbaPriceService.getCurrentPrice(playerId);


        // 3. Check if it's a buy or sell order.
        if (buySell == Trade.BuySell.BUY) {
            // 4. (Buy) Check if the user has enough balance.
            double totalPrice = currentPrice * quantity;
            if (user.getBalance() < totalPrice) {
                throw new RuntimeException("Insufficient balance.");
            }

            // 5. (Buy) Deduct the balance from the user.
            user.setBalance(user.getBalance() - totalPrice);

        } else { // Sell
            //check that use owns shares
            //TODO

            //add to user balance
            double totalPrice = currentPrice * quantity;
            user.setBalance(user.getBalance() + totalPrice);
        }


        // 6. Create the trade record.
        Trade trade = new Trade();
        trade.setUser(user);
        trade.setPlayer(player);
        trade.setBuySell(buySell);
        trade.setQuantity(quantity);
        trade.setPrice(currentPrice);
        trade.setTimestamp(LocalDateTime.now());

        // 7. Save the trade.
        tradeRepository.save(trade);

        // 8. Update the user (balance might have changed).
        userRepository.save(user); // Save updated user


        return trade;
    }

    public List<PlayerPriceHistory> getPriceHistory(Integer playerId) {
        return playerPriceHistoryRepository.findByPlayerIdOrderByTimestampDesc(playerId);
    }

    public List<Trade> getTradesByUser(Long userId) {
        return tradeRepository.findByUserId(userId);
    }

    public List<Trade> getTradesByPlayer(Integer playerId){
        return tradeRepository.findByPlayerId(playerId);
    }

    private int getCurrentHoldings(User user, Player player) {
        List<Trade> trades = tradeRepository.findByUserAndPlayerOrderByTimestampDesc(user, player);

        int holdings = 0;
        for (Trade trade : trades) {
            if (trade.getBuySell() == Trade.BuySell.BUY) {
                holdings += trade.getQuantity();
            } else {
                holdings -= trade.getQuantity();
            }
        }
        return holdings;
    }

}