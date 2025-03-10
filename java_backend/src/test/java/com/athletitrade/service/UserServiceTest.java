package com.athletitrade.service;

import com.athletitrade.dao.UserDao;
import com.athletitrade.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

// Mock use of UserService class to ensure java logic works to isolate errors.

public class UserServiceTest {

    private UserService userService;

    private UserDao userDao;

    private User testUser;

    @BeforeEach
    void setUp() {
        userDao = Mockito.mock(UserDao.class);

        userService = new UserService(userDao);

        testUser = new User("testuser", "password123", "test@example.com", 10000.00);
        testUser.setUserId(1);
    }

    @Test
    void loginUser_ValidCredentials_ReturnsUser() {
        when(userDao.findByUsername("testuser")).thenReturn(testUser);

        User loggedInUser = userService.loginUser("testuser", "password123");

        assertNotNull(loggedInUser);
        assertEquals("testuser", loggedInUser.getUsername());
        assertEquals("test@example.com", loggedInUser.getEmail());
        assertEquals("password123", loggedInUser.getPassword());
    }

    @Test
    void loginUser_InvalidPassword_ReturnsNull() {
        when(userDao.findByUsername("testuser")).thenReturn(testUser);

        User loggedInUser = userService.loginUser("testuser", "wrongpassword");

        assertNull(loggedInUser);
    }

    @Test
    void loginUser_UserNotFound_ReturnsNull() {
        when(userDao.findByUsername("nonexistentuser")).thenReturn(null);

        User loggedInUser = userService.loginUser("nonexistentuser", "anypassword");

        assertNull(loggedInUser);
    }

    @Test
    void registerUser_ValidInput_UserSavedAndReturned() {
        User newUser = new User("newUser", "newPassword", "new@example.com", 10000.00);

        when(userDao.save(Mockito.any(User.class))).thenReturn(newUser); // Mock save to return newUser

        User registeredUser = userService.registerUser("newUser", "newPassword", "new@example.com");

        assertNotNull(registeredUser);
        assertEquals("newUser", registeredUser.getUsername());
        assertEquals("new@example.com", registeredUser.getEmail());
        assertEquals("newPassword", registeredUser.getPassword());
        assertEquals(10000.00, registeredUser.getBalance());

        verify(userDao, Mockito.times(1)).save(Mockito.any(User.class));
    }
}