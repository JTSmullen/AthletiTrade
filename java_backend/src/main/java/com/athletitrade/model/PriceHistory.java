package com.athletitrade.model;

import java.math.BigDecimal;
import java.sql.Timestamp;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("PLAYER_PRICE_HISTORY") // Map to the "PLAYER_PRICE_HISTORY" table
public class PriceHistory {
    @Id
    private Integer priceHistoryId;
    private Integer playerId;
    private BigDecimal price;
    private Timestamp timestamp;
    private Integer buyVolume; // Track the buying volume
    private Integer sellVolume; // Track the selling volume

    // Constructors, Getters, Setters
    public PriceHistory() {
        this.buyVolume = 0; // Initialize buy volume to 0
        this.sellVolume = 0; // Initialize sell volume to 0
    } // init empty instance

    public PriceHistory(Integer playerId, BigDecimal price) {
        this.playerId = playerId;
        this.price = price;
        this.buyVolume = 0; // Initialize buy volume to 0
        this.sellVolume = 0; // Initialize sell volume to 0
    } // init instance with info for database

    public PriceHistory(Integer playerId, BigDecimal price, Integer buyVolume, Integer sellVolume) {
        this.playerId = playerId;
        this.price = price;
        this.buyVolume = buyVolume;
        this.sellVolume = sellVolume;
    }

    /*
     * Getters and Setters
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

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Timestamp getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Timestamp timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getBuyVolume() {
        return buyVolume;
    }

    public void setBuyVolume(Integer buyVolume) {
        this.buyVolume = buyVolume;
    }

    public Integer getSellVolume() {
        return sellVolume;
    }

    public void setSellVolume(Integer sellVolume) {
        this.sellVolume = sellVolume;
    }

    public void incrementBuyVolume(Integer quantity) {
        this.buyVolume += quantity;
    }

    public void incrementSellVolume(Integer quantity) {
        this.sellVolume += quantity;
    }
}
