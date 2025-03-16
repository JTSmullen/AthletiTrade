package com.athletitrade.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.sql.Timestamp;

@Table("TRADES") // Map to the "trades" table
public class Trade {
    @Id
    private Integer tradeId;
    private Integer userId;
    private Integer playerId;
    private String tradeType; // 'BUY' or 'SELL' | maybe switch to enum for consistency
    private Integer quantity;
    private BigDecimal price;
    private Timestamp tradeTimestamp;


    public Trade() {} // init empty instance

    public Trade(Integer userId, Integer playerId, String tradeType, Integer quantity, BigDecimal price) {
        this.userId = userId;
        this.playerId = playerId;
        this.tradeType = tradeType;
        this.quantity = quantity;
        this.price = price;
    } // init instance with info

    /*
        Getters and Setters
     */
    public Integer getTradeId() {
        return tradeId;
    }

    public void setTradeId(Integer tradeId) {
        this.tradeId = tradeId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public String getTradeType() {
        return tradeType;
    }

    public void setTradeType(String tradeType) {
        this.tradeType = tradeType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Timestamp getTradeTimestamp() {
        return tradeTimestamp;
    }

    public void setTradeTimestamp(Timestamp tradeTimestamp) {
        this.tradeTimestamp = tradeTimestamp;
    }
}