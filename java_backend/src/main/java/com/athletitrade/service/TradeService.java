package com.athletitrade.service;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.dao.TradeDao;
import com.athletitrade.dao.UserDao;
import com.athletitrade.exception.InsufficientFundsException;
import com.athletitrade.exception.InvalidTradeException;
import com.athletitrade.exception.PlayerNotFoundException;
import com.athletitrade.exception.UserNotFoundException;
import com.athletitrade.model.Player;
import com.athletitrade.model.Trade;
import com.athletitrade.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
public class TradeService {

    private static final Logger log = LoggerFactory.getLogger(TradeService.class);

    private final TradeDao tradeDao;
    private final UserDao userDao;
    private final PlayerDao playerDao;
    private final PlayerService playerService; // Inject PlayerService

    // Constructor to set params
    @Autowired
    public TradeService(TradeDao tradeDao, UserDao userDao, PlayerDao playerDao, PlayerService playerService) {
        this.tradeDao = tradeDao;
        this.userDao = userDao;
        this.playerDao = playerDao;
        this.playerService = playerService;
    }

    // Save a trade in the database
    @Transactional
    public Trade recordTrade(Trade trade) {
        return tradeDao.save(trade);
    }

    // User buys a player
    @Transactional
    public Trade executeBuyTrade(Integer userId, Integer playerId, int quantity) {
        // quantity must be positive
        if (quantity <= 0) {
            throw new InvalidTradeException("Quantity must be positive.");
        }

        // 2. Get User and Player
        User user = userDao.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        Player player = playerDao.findById(playerId).orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        // 3. Calculate total cost
        BigDecimal tradePrice = player.getCurrentPrice();
        BigDecimal totalCost = tradePrice.multiply(BigDecimal.valueOf(quantity));

        // 4. Check if user has sufficient funds
        if (user.getBalance().compareTo(totalCost) < 0) {
            throw new InsufficientFundsException("Insufficient funds to complete the trade.");
        }

        // 5. Deduct cost from user's balance
        user.setBalance(user.getBalance().subtract(totalCost));
        userDao.save(user); // Save updated user balance

        // 6. Create and save the trade record
        Trade trade = new Trade();
        trade.setUserId(userId);
        trade.setPlayerId(playerId);
        trade.setTradeType("BUY");
        trade.setQuantity(quantity);
        trade.setPrice(tradePrice); // Record the price *at the time of the trade*
        return tradeDao.save(trade); // Save trade and return saved object

    }


    @Transactional
    public Trade executeSellTrade(Integer userId, Integer playerId, int quantity) {
        // Validate input. No shorting players (maybe in the future)
        if (quantity <= 0) {
            throw new InvalidTradeException("Quantity must be positive.");
        }

        // Get the playerID and userID to change values in database
        User user = userDao.findById(userId).orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        Player player = playerDao.findById(playerId).orElseThrow(() -> new PlayerNotFoundException("Player not found with ID: " + playerId));

        // check to see if that user owns the player
        int ownedQuantity = getQuantityOwned(userId, playerId);
        if (ownedQuantity < quantity){
            throw new InsufficientFundsException("User does not own enough of this player");
        }

        // calculate the amount the user will receive
        BigDecimal tradePrice = player.getCurrentPrice();
        BigDecimal totalCredit = tradePrice.multiply(BigDecimal.valueOf(quantity));

        // Add the credit to the users balance (This is available and spendable balance)
        user.setBalance(user.getBalance().add(totalCredit));
        userDao.save(user);

        // Create new trade transaction
        Trade trade = new Trade();
        trade.setUserId(userId);
        trade.setPlayerId(playerId);
        trade.setTradeType("SELL");
        trade.setQuantity(quantity);
        trade.setPrice(tradePrice);
        return tradeDao.save(trade);
    }

    // Return all trades by a  user
    public List<Trade> getTradesByUser(Integer userId) {
        return tradeDao.findByUserId(userId);
    }

    // Get the amount of buys and sell the player has, subtract them to find how many trades user is in
    public int getQuantityOwned(Integer userId, Integer playerId) {
        List<Trade> userTrades = tradeDao.findByUserIdAndPlayerId(userId, playerId);

        int bought = 0;
        int sold = 0;

        for(Trade trade : userTrades) {
            if (trade.getTradeType().equals("BUY")) {
                bought += trade.getQuantity();
            }
            else {
                sold += trade.getQuantity();
            }
        }

        return bought - sold;
    }

    // Return all trades made
    public List<Trade> getAllTrades() {
        return (List<Trade>) tradeDao.findAll();
    }

    // ... (other methods, e.g., getTradesByPlayer, cancelTrade, etc.) ...
}