package com.athletitrade.controller;

import com.athletitrade.model.Trade;
import com.athletitrade.service.TradeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// ENDPOINTS FOR THE FRONT END TO CALL. FINISH OTHER THINGS FIRST

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    private final TradeService tradeService;

    @Autowired
    public TradeController(TradeService tradeService) {
        this.tradeService = tradeService;
    }

    @PostMapping("/record")
    public ResponseEntity<Trade> recordTrade(@RequestBody Trade trade) {
        Trade recordedTrade = tradeService.recordTrade(trade);
        return ResponseEntity.status(HttpStatus.CREATED).body(recordedTrade);
    }
}