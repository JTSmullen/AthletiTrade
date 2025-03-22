//// DataInitializer.java
//package com.athletitrade.config;
//
//import com.athletitrade.model.Player;
//import com.athletitrade.model.PlayerPriceHistory;
//import com.athletitrade.service.NBAPriceService;
//import com.athletitrade.service.PlayerService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//
//import java.time.LocalDateTime;
//import java.util.List;
//
//@Configuration
//public class DataInitializer {
//
//    @Autowired
//    private PlayerService playerService;
//
//    @Autowired
//    private NBAPriceService nbaPriceService;
//
//    @Bean
//    public CommandLineRunner initializeData() {
//        return args -> {
//            List<Player> players = playerService.getAllPlayers();
//
//            for (Player player : players) {
//                try {
//                    double initialPrice = nbaPriceService.getStartingPriceFromPythonApi(player.getId());
//                    PlayerPriceHistory history = new PlayerPriceHistory();
//                    history.setPlayer(player);
//                    history.setPrice(initialPrice);
//                    history.setTimestamp(LocalDateTime.now());
//                    playerService.savePlayerPriceHistory(history);
//                    Thread.sleep(2500);
//                } catch (Exception e){
//                    System.out.println("Error getting stats for: "+ player.getFullName());
//                }
//            }
//            System.out.println("Database pre-populated with player data and initial prices.");
//        };
//    }
//}