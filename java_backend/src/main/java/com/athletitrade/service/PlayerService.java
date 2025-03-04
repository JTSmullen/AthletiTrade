package com.athletitrade.service;

import com.athletitrade.dao.PlayerDao;
import com.athletitrade.model.Player;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    private final PlayerDao playerDao;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final String PYTHON_API_BASE_URL = "http://localhost:5000"; // Python API URL

    @Autowired
    public PlayerService(PlayerDao playerDao, RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.playerDao = playerDao;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public Player getPlayerById(int playerId) {
        Optional<Player> playerOptional = playerDao.findById(playerId);
        return playerOptional.orElse(null);
    }

    public List<Player> fetchPlayers() {
        String playersListUrl = PYTHON_API_BASE_URL + "/players/list";

        try {
            String playersJson = restTemplate.getForObject(playersListUrl, String.class);
            JsonNode rootNode = objectMapper.readTree(playersJson);

            JsonNode playersListNode = rootNode.get("player");

            if (playersListNode != null && playersListNode.isArray()) {
                List<Player> players = new ArrayList<>();
                for (JsonNode playerNode : playersListNode) {
                    Player player = new Player();
                    player.setPlayerId(playerNode.get("id").asInt());
                    player.setPlayerName(playerNode.get("full_name").asText());
                    player.setTeamId(null);
                    player.setPosition(null);
                    player.setCurrentPrice(10.00);
                    players.add(player);
                }
                return players;
            }
        } catch (IOException e) {
            System.err.println("Error fetching or parsing player list from Python API: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error fetching player list: " + e.getMessage());
        }
        return null;
    }

    public Player fetchAndUpdatePlayerInfo(int playerId) {
        String playerInfoUrl = PYTHON_API_BASE_URL + "/players/" + playerId + "/info";

        try {
            String playerInfoJson = restTemplate.getForObject(playerInfoUrl, String.class);
            JsonNode playerInfoNode = objectMapper.readTree(playerInfoJson);

            if (playerInfoNode != null) {
                JsonNode playerInfoResult = playerInfoNode.get("resultSets").get(0).get("rowSet").get(0);

                String playerName = playerInfoResult.get(1).asText(); // PLAYER_NAME
                int teamId = playerInfoResult.get(7).asInt();     // TEAM_ID
                String position = playerInfoResult.get(14).asText();  // POSITION

                Player player = getPlayerById(playerId);
                if (player == null) {
                    player = new Player();
                    player.setPlayerId(playerId);
                }
                player.setPlayerName(playerName);
                player.setTeamId(teamId);
                player.setPosition(position);
                player.setCurrentPrice(10.00);

                return playerDao.save(player);
            }
        } catch (IOException e) {
            System.err.println("Error fetching or parsing player info from Python API: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("General error fetching player info: " + e.getMessage());
        }
        return null;
    }

}