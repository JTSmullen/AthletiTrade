package com.athletitrade.dao;

import com.athletitrade.model.PriceHistory;
import org.springframework.data.repository.CrudRepository;

public interface PriceHistoryDao extends CrudRepository<PriceHistory, Integer> {
    // add custom query methods here if needed later
}