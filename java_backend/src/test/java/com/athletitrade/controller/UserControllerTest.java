package com.athletitrade.controller;

import com.athletitrade.dto.LoginDto;
import com.athletitrade.dto.RegistrationDto;
import com.athletitrade.model.User;
import com.athletitrade.repository.UserRepository;
import com.athletitrade.security.JwtTokenUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.is;


@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private ObjectMapper objectMapper;

    private User createTestUser() {
        User user = new User();
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setPassword(passwordEncoder.encode("password"));
        user.setBalance(1000.0);
        return userRepository.save(user);
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();

    }
    @Test
    public void registerUser_Success() throws Exception {
        RegistrationDto registrationDto = new RegistrationDto();
        registrationDto.setUsername("newuser");
        registrationDto.setEmail("newuser@example.com");
        registrationDto.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username", is("newuser")));
    }


    @Test
    public void registerUser_DuplicateUsername_ReturnsBadRequest() throws Exception {
        createTestUser();

        RegistrationDto registrationDto = new RegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("newuser2@example.com");
        registrationDto.setPassword("password123");

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest());
    }
    @Test
    public void registerUser_InvalidEmail_ReturnsBadRequest() throws Exception{
        RegistrationDto registrationDto = new RegistrationDto();
        registrationDto.setUsername("testuser");
        registrationDto.setEmail("invalid email");
        registrationDto.setPassword("password");
        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registrationDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void loginUser_Success() throws Exception {
        User user = createTestUser();

        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("testuser");
        loginDto.setPassword("password");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isOk()) // Verify 200 OK
                .andExpect(jsonPath("$.token").exists());
    }
    @Test
    public void loginUser_InvalidCredentials_ReturnsUnauthorized() throws Exception {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername("nonexistentuser");
        loginDto.setPassword("invalidpassword");

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDto)))
                .andExpect(status().isUnauthorized());
    }
}