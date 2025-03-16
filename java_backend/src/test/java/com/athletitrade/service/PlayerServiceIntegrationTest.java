package com.athletitrade.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.client.RestTemplate;

import com.athletitrade.AthletiTradeApp;
import com.athletitrade.config.AppConfig;
import com.athletitrade.dao.PlayerDao;
import com.athletitrade.dao.PriceHistoryDao;
import com.athletitrade.model.Player;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PlayerServiceIntegrationTest {

    private PlayerService playerService;

    private RestTemplate restTemplate;
    private PlayerDao playerDao;
    private ObjectMapper objectMapper;
    private AppConfig appConfig;
    private AthletiTradeApp athletiTradeApp;
    private PriceHistoryDao priceHistoryDao;

    // Before each set up a mock DAO
    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        playerDao = Mockito.mock(PlayerDao.class);
        appConfig = Mockito.mock(AppConfig.class);
        athletiTradeApp = Mockito.mock(AthletiTradeApp.class);
        priceHistoryDao = Mockito.mock(PriceHistoryDao.class);

        objectMapper = new ObjectMapper();

        // Use the updated constructor
        playerService = new PlayerService(playerDao, restTemplate, objectMapper, appConfig, athletiTradeApp,
                priceHistoryDao);
    }

    // Get Players from API
    @Test
    void fetchPlayers_SuccessfulFetch_IntegrationTest_ManualWiring() {
        List<Player> players = playerService.fetchPlayers();

        assertNotNull(players, "Player list should not be null - indicates successful API call and parsing");
        assertFalse(players.isEmpty(), "Player list should not be empty - API should return players");

        if (!players.isEmpty()) {
            Player firstPlayer = players.get(0);
            assertNotNull(firstPlayer.getPlayerId(), "First player should have a Player ID");
            assertNotNull(firstPlayer.getPlayerName(), "First player should have a Player Name");
        }
    }

    // Get game stats for a player ID (Assumes current season)
    @Test
    void fetchPlayerGameStats_SuccessfulFetch_IntegrationTest_ManualWiring() {
        int playerId = 201566;

        JsonNode gameStats = playerService.fetchPlayerGameStats(playerId);

        assertNotNull(gameStats, "Game stats JsonNode should not be null for valid player ID");
        assertTrue(gameStats.isObject(), "Game stats should be a JsonNode object");

        if (gameStats != null && gameStats.isObject()) {
            assertTrue(gameStats.has("resultSets"), "Game stats should contain 'resultSets'");
        }
    }

    // Incorrect playerID call
    @Test
    void fetchPlayerGameStats_ApiNotFound_IntegrationTest_ManualWiring() {
        int playerId = 999999;

        JsonNode gameStats = playerService.fetchPlayerGameStats(playerId);

        assertNull(gameStats, "Game stats JsonNode should be null for invalid player ID (API 404)");
    }

}
