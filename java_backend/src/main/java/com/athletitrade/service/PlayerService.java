package com.athletitrade.service;

import com.athletitrade.dto.PlayerStatsDto;
import com.athletitrade.model.Player;
import com.athletitrade.model.PlayerPriceHistory;
import com.athletitrade.repository.PlayerPriceHistoryRepository;
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
    private OkHttpClient httpClient;
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;

    @Autowired
    private NBAPriceService nbaPriceService;
    private static final String PYTHON_API_BASE_URL = "http://127.0.0.1:5000/api";

    public List<Player> getAllPlayers() {
        List<Player> players = playerRepository.findAll();
        if (!players.isEmpty()) {
            return players;
        }

        try {
            Request request = new Request.Builder()
                    .url(PYTHON_API_BASE_URL + "/players")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                List<Player> fetchedPlayers = objectMapper.readValue(response.body().string(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Player.class));

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

    public PlayerStatsDto getPlayerStats(Integer playerId, boolean rolling) {
        try {
            String url = PYTHON_API_BASE_URL + "/player/" + playerId + "/stats?rolling=" + rolling;
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

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

    @Transactional
    public void refreshPlayerData() {
        try {
            List<Player> currentPlayers = playerRepository.findAll();

            Request request = new Request.Builder()
                    .url(PYTHON_API_BASE_URL + "/players")
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }
                List<Player> fetchedPlayers = objectMapper.readValue(response.body().string(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Player.class));

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

    //PlayerService.java

    @Transactional
    public void savePlayerPriceHistory(PlayerPriceHistory playerPriceHistory) {
        playerPriceHistoryRepository.save(playerPriceHistory);
    }
}