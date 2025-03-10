package com.athletitrade.service;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.model.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PlayerServiceIntegrationTest {

    private PlayerService playerService;

    private RestTemplate restTemplate;
    private PlayerDao playerDao;
    private ObjectMapper objectMapper;

    private final String PYTHON_API_BASE_URL = "http://localhost:8000";

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();

        playerDao = Mockito.mock(PlayerDao.class);

        objectMapper = new ObjectMapper();

        playerService = new PlayerService(playerDao, restTemplate, objectMapper);
    }

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

    @Test
    void fetchPlayerGameStats_ApiNotFound_IntegrationTest_ManualWiring() {
        int playerId = 999999;

        JsonNode gameStats = playerService.fetchPlayerGameStats(playerId);

        assertNull(gameStats, "Game stats JsonNode should be null for invalid player ID (API 404)");
    }

}