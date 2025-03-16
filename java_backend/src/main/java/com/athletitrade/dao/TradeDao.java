package com.athletitrade.dao;

import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import com.athletitrade.model.Trade;

public interface TradeDao extends CrudRepository<Trade, Integer> {
    List<Trade> findByUserId(Integer userId);

    @Query("SELECT t FROM Trade t WHERE t.userId = :userId AND t.playerId = :playerId")
    List<Trade> findByUserIdAndPlayerId(Integer userId, Integer playerId);

    @Query("SELECT t FROM Trade t WHERE t;playerId = :playerId")
    List<Trade> findByPlayerId(Integer playerId);
}
