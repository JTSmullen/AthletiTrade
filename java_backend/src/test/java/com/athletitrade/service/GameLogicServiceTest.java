package com.athletitrade.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.dao.TradeDao;
import com.athletitrade.model.Player;
import com.athletitrade.model.Trade;

@ExtendWith(MockitoExtension.class)
class GameLogicServiceTest {

    @Mock
    private PlayerService playerService;

    @Mock
    private PlayerDao playerDao;

    @Mock
    private TradeDao tradeDao;

    @InjectMocks
    private GameLogicService gameLogicService;

    private Player player1;
    private Player player2;
    private Trade trade1;
    private Trade trade2;
    private Trade trade3;

    @BeforeEach
    void setUp() {
        player1 = new Player();
        player1.setPlayerId(1);
        player2 = new Player();
        player2.setPlayerId(2);

        trade1 = new Trade();
        trade1.setTradeId(1);
        trade1.setPlayerId(1);
        trade1.setTradeType("BUY");
        trade1.setQuantity(10);

        trade2 = new Trade();
        trade2.setTradeId(2);
        trade2.setPlayerId(1);
        trade2.setTradeType("SELL");
        trade2.setQuantity(5);

        trade3 = new Trade();
        trade3.setTradeId(3);
        trade3.setPlayerId(2);
        trade3.setTradeType("BUY");
        trade3.setQuantity(20);
    }

    @Test
    void updatePlayerPrices_shouldUpdateAllPlayerPrices() {
        // Arrange
        List<Player> allPlayers = Arrays.asList(player1, player2);
        when(playerDao.findAll()).thenReturn(allPlayers);

        List<Trade> player1Trades = Arrays.asList(trade1, trade2);
        when(tradeDao.findByPlayerId(1)).thenReturn(player1Trades);

        List<Trade> player2Trades = Arrays.asList(trade3);
        when(tradeDao.findByPlayerId(2)).thenReturn(player2Trades);

        // Act
        gameLogicService.updatePlayerPrices();

        // Assert
        verify(playerDao, times(1)).findAll();
        verify(tradeDao, times(1)).findByPlayerId(1);
        verify(tradeDao, times(1)).findByPlayerId(2);
        verify(playerService, times(1)).updatePlayerPrice(1, 10, 5);
        verify(playerService, times(1)).updatePlayerPrice(2, 20, 0);
    }

    @Test
    void updatePlayerPrices_shouldHandleExceptionWhenUpdatingPlayerPrice() {
        // Arrange
        List<Player> allPlayers = Arrays.asList(player1, player2);
        when(playerDao.findAll()).thenReturn(allPlayers);

        List<Trade> player1Trades = Arrays.asList(trade1, trade2);
        when(tradeDao.findByPlayerId(1)).thenReturn(player1Trades);

        List<Trade> player2Trades = Arrays.asList(trade3);
        when(tradeDao.findByPlayerId(2)).thenReturn(player2Trades);

        // Simulate an exception when updating player 2's price
        doThrow(new RuntimeException("Simulated error")).when(playerService).updatePlayerPrice(2, 20, 0);

        // Act
        gameLogicService.updatePlayerPrices();

        // Assert
        verify(playerDao, times(1)).findAll();
        verify(tradeDao, times(1)).findByPlayerId(1);
        verify(tradeDao, times(1)).findByPlayerId(2);
        verify(playerService, times(1)).updatePlayerPrice(1, 10, 5);
        verify(playerService, times(1)).updatePlayerPrice(2, 20, 0); // Verify that the method was still called
    }

    @Test
    void calculateTradeVolumes_shouldCalculateCorrectVolumes() {
        // Arrange
        List<Trade> trades = Arrays.asList(trade1, trade2);

        // Act
        java.util.Map<String, Integer> volumes = gameLogicService.calculateTradeVolumes(trades);

        // Assert
        assertEquals(10, volumes.get("BUY"));
        assertEquals(5, volumes.get("SELL"));
    }

    @Test
    void calculateTradeVolumes_shouldHandleEmptyTrades() {
        // Arrange
        List<Trade> trades = new ArrayList<>();

        // Act
        java.util.Map<String, Integer> volumes = gameLogicService.calculateTradeVolumes(trades);

        // Assert
        assertEquals(0, volumes.get("BUY"));
        assertEquals(0, volumes.get("SELL"));
    }

    @Test
    void calculateTradeVolumes_shouldHandleUnknownTradeType() {
        // Arrange
        Trade unknownTrade = new Trade();
        unknownTrade.setTradeId(4);
        unknownTrade.setPlayerId(1);
        unknownTrade.setTradeType("UNKNOWN");
        unknownTrade.setQuantity(5);
        List<Trade> trades = Arrays.asList(trade1, unknownTrade);

        // Act
        java.util.Map<String, Integer> volumes = gameLogicService.calculateTradeVolumes(trades);

        // Assert
        assertEquals(10, volumes.get("BUY"));
        assertEquals(0, volumes.get("SELL"));
    }
}
