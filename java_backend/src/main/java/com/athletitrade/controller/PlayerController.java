package com.athletitrade.controller;

import com.athletitrade.dto.PlayerStatsDto;
import com.athletitrade.model.Player;
import com.athletitrade.model.PlayerPriceHistory;
import com.athletitrade.service.PlayerService;
import com.athletitrade.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {

    @Autowired
    private PlayerService playerService;

    @Autowired
    private TradeService tradeService;

    @GetMapping
    public ResponseEntity<List<Player>> getAllPlayers() {
        List<Player> players = playerService.getAllPlayers();
        return ResponseEntity.ok(players);
    }

    @GetMapping("/{playerId}")
    public ResponseEntity<Player> getPlayerById(@PathVariable Integer playerId) {
        return playerService.getPlayerById(playerId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    @GetMapping("/{playerId}/stats")
    public ResponseEntity<PlayerStatsDto> getPlayerStats(@PathVariable Integer playerId,
                                                         @RequestParam(defaultValue = "false") boolean rolling) {
        PlayerStatsDto stats = playerService.getPlayerStats(playerId, rolling);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{playerId}/priceHistory")
    public ResponseEntity<List<PlayerPriceHistory>> getPlayerPriceHistory(@PathVariable Integer playerId) {
        List<PlayerPriceHistory> history = tradeService.getPriceHistory(playerId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/search")
    public ResponseEntity<List<Player>> searchPlayers(@RequestParam String query) {
        List<Player> players = playerService.searchPlayers(query);
        return ResponseEntity.ok(players);
    }
}