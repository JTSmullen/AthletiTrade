package com.athletitrade.dao;

import com.athletitrade.model.Player;
import org.springframework.data.repository.CrudRepository;

// player data access objects (JDBC calls)

public interface PlayerDao extends CrudRepository<Player, Integer> {
    // add custom query methods here if needed later
}