package com.athletitrade.dao;

import com.athletitrade.model.Trade;
import org.springframework.data.repository.CrudRepository;

// trade data access objects (JDBC calls)

public interface TradeDao extends CrudRepository<Trade, Integer> {
    // add custom query methods here if needed later
}