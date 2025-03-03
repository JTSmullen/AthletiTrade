package com.athletitrade.service;

import com.athletitrade.dao.TradeDao;
import com.athletitrade.model.Trade;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TradeService {

    private final TradeDao tradeDao;

    @Autowired
    public TradeService(TradeDao tradeDao) {
        this.tradeDao = tradeDao;
    }

    public Trade recordTrade(Trade trade) {
        return tradeDao.save(trade);
    }

    // Other trade-related service methods | execute trade, get trade history, etc.
}