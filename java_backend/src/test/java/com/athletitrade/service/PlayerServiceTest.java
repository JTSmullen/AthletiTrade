package com.athletitrade.service;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.model.Player;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// Unit Test - Removed @SpringBootTest - Manual Wiring for Unit Test
//@SpringBootTest  // No longer using SpringBootTest - creating a pure unit test
public class PlayerServiceTest {

    private PlayerService playerService;

    private RestTemplate restTemplate;
    private PlayerDao playerDao;
    @Autowired
    private ObjectMapper objectMapper;

    private final String PYTHON_API_BASE_URL = "http://localhost:5000";

    @BeforeEach
    void setUp() {
        restTemplate = Mockito.mock(RestTemplate.class);
        playerDao = Mockito.mock(PlayerDao.class);
        objectMapper = new ObjectMapper();

        playerService = new PlayerService(playerDao, restTemplate, objectMapper);
    }

    @Test
    void fetchPlayers_SuccessfulFetch_ReturnsPlayerList() throws Exception {
        String mockPlayersJson = """
                {
                  "player": [
                    {
                      "first_name": "Stephen",
                      "full_name": "Stephen Curry",
                      "id": 201939,
                      "is_active": true,
                      "last_name": "Curry"
                    },
                    {
                      "first_name": "Joel",
                      "full_name": "Joel Embiid",
                      "id": 203954,
                      "is_active": true,
                      "last_name": "Embiid"
                    }
                  ]
                }
                """;

        // Configure RestTemplate mock to return mockPlayersJson when /players/list is called
        when(restTemplate.getForObject(PYTHON_API_BASE_URL + "/players/list", String.class))
                .thenReturn(mockPlayersJson);

        // 2. Call the method being tested: fetchPlayers()
        List<Player> players = playerService.fetchPlayers();

        // 3. Assertions - Verify the results
        assertNotNull(players);
        assertEquals(2, players.size());

        // Verify data of the first player - **Assertions adjusted to match JSON fields**
        Player player1 = players.get(0);
        assertEquals(201939, player1.getPlayerId()); // **Check against "id" field**
        assertEquals("Stephen Curry", player1.getPlayerName()); // **Check against "full_name" field**
        assertNull(player1.getTeamId()); // **Expect teamId to be null - not in JSON**
        assertNull(player1.getPosition()); // **Expect position to be null - not in JSON**
        assertEquals(10.00, player1.getCurrentPrice(), 0.001);

        // Verify data of the second player - **Assertions adjusted to match JSON fields**
        Player player2 = players.get(1);
        assertEquals(203954, player2.getPlayerId()); // **Check against "id" field**
        assertEquals("Joel Embiid", player2.getPlayerName()); // **Check against "full_name" field**
        assertNull(player2.getTeamId()); // **Expect teamId to be null - not in JSON**
        assertNull(player2.getPosition()); // **Expect position to be null - not in JSON**
        assertEquals(10.00, player2.getCurrentPrice(), 0.001);
    }

    // You can add more test methods here to cover error scenarios, empty responses, etc.
}