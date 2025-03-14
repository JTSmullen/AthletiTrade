package com.athletitrade.service;

import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;

@Service
public class GameLogicService {

    public void updatePlayerPrices() {
        // **TODO:** Implement price update algorithm here
        System.out.println("GameLogicService: updatePlayerPrices() - Price update logic to be implemented.");
    }

    public double calculatePlayerPrice(){
        System.out.println("GameLogicService: calculatePlayerPrice() - Price calculation logic to be implemented");
        return 1;
    }

    // Game Logic Methods

    public void volume(){
        System.out.println("GameLogicService: volume() - Get the volume change");
    }
}