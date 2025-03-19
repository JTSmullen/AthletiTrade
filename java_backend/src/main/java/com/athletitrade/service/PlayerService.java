package com.athletitrade.service;

import com.athletitrade.dto.PlayerStatsDto;
import com.athletitrade.model.Player;
import com.athletitrade.repository.PlayerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class PlayerService {

    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private OkHttpClient httpClient; // Inject the OkHttpClient bean
    @Autowired
    private ObjectMapper objectMapper; //Auto wire the object mapper

    @Autowired
    private NBAPriceService nbaPriceService;
    private static final String PYTHON_API_BASE_URL = "http://127.0.0.1:5000/api"; //Should be in a config

    public List<Player> getAllPlayers() {
        //Check if players exist in DB
        List<Player> players = playerRepository.findAll();
        if (!players.isEmpty()) {
            return players;
        }

        //Fetch players from python api if not in db
        try {
            Request request = new Request.Builder()
                    .url(PYTHON_API_BASE_URL + "/players")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                // Deserialize the JSON array to a List of Player objects
                List<Player> fetchedPlayers = objectMapper.readValue(response.body().string(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Player.class));

                // Save all fetched players to the database
                playerRepository.saveAll(fetchedPlayers);

                return fetchedPlayers;
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch or save player data", e);
        }
    }

    public Optional<Player> getPlayerById(Integer id) {
        return playerRepository.findById(id);
    }

    public PlayerStatsDto getPlayerStats(String playerName, boolean rolling) {
        try {
            String url = PYTHON_API_BASE_URL + "/player/" + playerName + "/stats?rolling=" + rolling;
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                // Deserialize the JSON response into PlayerStatsDto
                return objectMapper.readValue(response.body().string(), PlayerStatsDto.class);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to fetch player stats", e);
        }
    }

    public Player savePlayer(Player player) {
        return playerRepository.save(player);
    }

    @Transactional
    public List<Player> saveAllPlayers(List<Player> players){
        return playerRepository.saveAll(players);
    }

    @Transactional // Important for data consistency
    public void refreshPlayerData() {
        try {
            // 1. Get the current list of players from the database.
            List<Player> currentPlayers = playerRepository.findAll();

            // 2. Fetch the latest player data from the Python API.
            Request request = new Request.Builder()
                    .url(PYTHON_API_BASE_URL + "/players")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                List<Player> fetchedPlayers = objectMapper.readValue(response.body().string(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Player.class));

                // 3. Update or add players.
                playerRepository.saveAll(fetchedPlayers);

            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to refresh player data", e);
        }
    }
    public double getCurrentPrice(Integer playerId) {
        return nbaPriceService.getCurrentPrice(playerId);
    }

    public Optional<Player> getPlayerByName(String fullName){
        return playerRepository.findByFullName(fullName);
    }

    public List<Player> searchPlayers(String query) {
        return playerRepository.findByFullNameContainingIgnoreCase(query);
    }
}