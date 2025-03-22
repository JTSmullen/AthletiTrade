package com.athletitrade.controller;

import com.athletitrade.dto.BuyOrderDto;
import com.athletitrade.dto.LoginDto;
import com.athletitrade.dto.SellOrderDto;
import com.athletitrade.model.Player;
import com.athletitrade.model.PlayerPriceHistory;
import com.athletitrade.model.Trade;
import com.athletitrade.model.User;
import com.athletitrade.repository.PlayerPriceHistoryRepository;
import com.athletitrade.repository.PlayerRepository;
import com.athletitrade.repository.TradeRepository;
import com.athletitrade.repository.UserRepository;
import com.athletitrade.security.JwtTokenUtil;
import com.athletitrade.service.NBAPriceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get; // Import get method
import static org.hamcrest.Matchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class TradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PlayerRepository playerRepository;

    @Autowired
    private TradeRepository tradeRepository;

    @Autowired
    private PlayerPriceHistoryRepository playerPriceHistoryRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private NBAPriceService nbaPriceService;

    private String jwtToken; // Store JWT for authenticated requests
    private User testUser; // Store test user

    private Player testPlayer;

    @BeforeEach
    void setUp() throws Exception {
        // Clean up the database before each test (optional, but good practice)
        tradeRepository.deleteAll();
        userRepository.deleteAll();
        playerRepository.deleteAll();
        playerPriceHistoryRepository.deleteAll();

        //Create a test user
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword(passwordEncoder.encode("password"));
        testUser.setBalance(1000.0);
        testUser = userRepository.save(testUser);

        // Create Test Player
        testPlayer = new Player();
        testPlayer.setId(1);
        testPlayer.setFullName("LeBron James");
        testPlayer = playerRepository.save(testPlayer);

        //Set the test player price
        PlayerPriceHistory playerPriceHistory = new PlayerPriceHistory();
        playerPriceHistory.setPlayer(testPlayer);
        playerPriceHistory.setPrice(100.0);
        playerPriceHistory.setTimestamp(LocalDateTime.now());
        playerPriceHistoryRepository.save(playerPriceHistory);


        // Get the JWT token for the test user
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("testuser");
        loginDto.setPassword("password");

        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        Map<String, String> jsonResult = objectMapper.readValue(responseBody, Map.class);
        jwtToken = jsonResult.get("token");
    }
    @Test
    void executeBuyTrade_Success() throws Exception {
        // Create a BuyOrderDto
        BuyOrderDto buyOrderDto = new BuyOrderDto();
        buyOrderDto.setPlayerId(testPlayer.getId());
        buyOrderDto.setQuantity(1);

        // Test a buy order
        mockMvc.perform(post("/api/trades/buy")
                        .header("Authorization", "Bearer " + jwtToken) // Set JWT Header
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.player.id").value(testPlayer.getId())); // Should be a 201 Created
        // You might want to add more assertions here (e.g., check user balance, check trade record)

    }


    @Test
    void executeBuyTrade_InsufficientBalance_ReturnsBadRequest() throws Exception {
        // Create a user with an extremely low balance
        User poorUser = new User();
        poorUser.setUsername("pooruser");
        poorUser.setEmail("poor@example.com");
        poorUser.setPassword(passwordEncoder.encode("password"));
        poorUser.setBalance(0.0);
        poorUser = userRepository.save(poorUser);

        // Retrieve a token for poorUser
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername(poorUser.getUsername());
        loginDto.setPassword("password");
        MvcResult result = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk())
                .andReturn();

        String poorUserResponseBody = result.getResponse().getContentAsString();
        Map<String, String> jsonResult = objectMapper.readValue(poorUserResponseBody, Map.class);
        String poorUserJwtToken = jsonResult.get("token");



        // Create a BuyOrderDto with high quantity (exceeding user's balance)
        BuyOrderDto buyOrderDto = new BuyOrderDto();
        buyOrderDto.setPlayerId(testPlayer.getId());
        buyOrderDto.setQuantity(1); // Exceeds the balance of 0

        mockMvc.perform(post("/api/trades/buy")
                        .header("Authorization", "Bearer " + poorUserJwtToken) // Use poorUser's token
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderDto)))
                .andExpect(status().isBadRequest()); // Expect 400 Bad Request
    }

    @Test
    void executeSellTrade_Success() throws Exception {
        // 1. Execute a buy trade first so that the user has something to sell
        BuyOrderDto buyOrderDto = new BuyOrderDto();
        buyOrderDto.setPlayerId(testPlayer.getId());
        buyOrderDto.setQuantity(1);

        mockMvc.perform(post("/api/trades/buy")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buyOrderDto)))
                .andExpect(status().isCreated());

        // Create a SellOrderDto
        SellOrderDto sellOrderDto = new SellOrderDto();
        sellOrderDto.setPlayerId(testPlayer.getId());
        sellOrderDto.setQuantity(1);

        // 2. Sell order (make sure JWT and SellOrderDto are correct)
        mockMvc.perform(post("/api/trades/sell")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sellOrderDto)))
                .andExpect(status().isCreated());
    }
}