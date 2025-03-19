package com.athletitrade.controller;

import com.athletitrade.dto.BuyOrderDto;
import com.athletitrade.dto.SellOrderDto;
import com.athletitrade.model.User;
import com.athletitrade.model.Trade;
import com.athletitrade.service.TradeService;
import com.athletitrade.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/trades")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    @Autowired
    private UserService userService;

    @PostMapping("/buy")
    public ResponseEntity<?> executeBuyTrade(@Valid @RequestBody BuyOrderDto buyOrderDto, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get the username from the UserDetails
            String username = userDetails.getUsername();

            // Find the User entity by username (you'll need a method in UserService for this)
            // Assuming you have a UserService and a findByUsername method:
            User user = userService.findByUsername(username);
            if (user == null) {
                return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED); // Should not happen if auth is working
            }


            Trade trade = tradeService.executeTrade(user.getId(), buyOrderDto.getPlayerId(), Trade.BuySell.BUY, buyOrderDto.getQuantity());
            return new ResponseEntity<>(trade, HttpStatus.CREATED); // 201 Created
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // 400 Bad Request
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST); // Or 500 Internal Server Error, depending on the exception
        }
    }

    @PostMapping("/sell")
    public ResponseEntity<?> executeSellTrade(@Valid @RequestBody SellOrderDto sellOrderDto, @AuthenticationPrincipal UserDetails userDetails) {
        try {
            // Get the username from UserDetails
            String username = userDetails.getUsername();

            // Find the User entity
            User user = userService.findByUsername(username);
            if (user == null) {
                return new ResponseEntity<>("User not found", HttpStatus.UNAUTHORIZED);
            }

            Trade trade = tradeService.executeTrade(user.getId(), sellOrderDto.getPlayerId(), Trade.BuySell.SELL, sellOrderDto.getQuantity());
            return new ResponseEntity<>(trade, HttpStatus.CREATED);
        } catch (RuntimeException e) {
            return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}