package com.athletitrade.dao;

import com.athletitrade.model.PriceHistory;
import org.springframework.data.repository.CrudRepository;

// price history data access objects (JDBC calls)

public interface PriceHistoryDao extends CrudRepository<PriceHistory, Integer> {
    // add custom query methods here if needed later
}