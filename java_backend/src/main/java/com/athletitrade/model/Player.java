package com.athletitrade.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("PLAYERS") // Map to the "PLAYERS" table
public class Player {
    @Id
    private Integer playerId;
    private String playerName;
    private Integer teamId;
    private String position;
    private Double currentPrice;

    public Player() {} // init empty player instance

    public Player(Integer playerId, String playerName, Integer teamId, String position, Double currentPrice) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.teamId = teamId;
        this.position = position;
        this.currentPrice = currentPrice;
    } // init player instance with info from python_api

    /*
        Getters / Setters
     */
    public Integer getPlayerId() {
        return playerId;
    }

    public void setPlayerId(Integer playerId) {
        this.playerId = playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public Integer getTeamId() {
        return teamId;
    }

    public void setTeamId(Integer teamId) {
        this.teamId = teamId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(Double currentPrice) {
        this.currentPrice = currentPrice;
    }
}