package com.athletitrade.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.sql.Timestamp;

@Table("PLAYER_PRICE_HISTORY") // Map to the "PLAYER_PRICE_HISTORY" table
public class PriceHistory {
    @Id
    private Integer priceHistoryId;
    private Integer playerId;
    private Double price;
    private Timestamp timestamp;

    // Constructors, Getters, Setters
    public PriceHistory() {} // init empty instance

    public PriceHistory(Integer playerId, Double price) {
        this.playerId = playerId;
        this.price = price;
    } // init instance with info for database

    /*
        Getters and Setters
     */
    public Integer getPriceHistoryId() {
        return priceHistoryId;
    }

    public void setPriceHistoryId(Integer priceHistoryId) {
        this.priceHistoryId = priceHistoryId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }
}