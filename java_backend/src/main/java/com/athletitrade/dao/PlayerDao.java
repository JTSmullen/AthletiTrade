package com.athletitrade.dao;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

import com.athletitrade.model.Player;

public interface PlayerDao extends CrudRepository<Player, Integer> {

    Player findByPlayerName(String playerName);

    List<Player> findByTeamId(Integer teamId);

    List<Player> findByPosition(String position);

    List<Player> findByPlayerNameContainingIgnoreCase(String partialName);

    List<Player> findByTeamIdAndPosition(Integer teamId, String position);

    @Modifying
    @Transactional
    @Query(value = "UPDATE PLAYERS SET current_price = :newPrice WHERE player_id = :playerId", nativeQuery = true)
    void updatePlayerPrice(Integer playerId, BigDecimal newPrice);
}