package com.athletitrade.dao;

import com.athletitrade.model.PriceHistory;
import org.springframework.data.repository.CrudRepository;

public interface PriceHistoryDao extends CrudRepository<PriceHistory, Integer> {
    // You might add methods here later to query the price history (e.g., by player ID, by date range)
}