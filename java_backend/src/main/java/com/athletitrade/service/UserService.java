package com.athletitrade.service;

import com.athletitrade.dao.UserDao;
import com.athletitrade.model.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserDao userDao;

    @Autowired
    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User registerUser(String username, String password, String email) {
        // TODO: Hash the password before saving
        User newUser = new User(username, password, email, 10000.00);
        return userDao.save(newUser);
    }

    public User loginUser(String username, String password) {
        User user = userDao.findByUsername(username);
        if (user != null && user.getPassword().equals(password)) { // Replace with hashed password comparison
            return user;
        }
        return null; // Or throw an exception
    }

    public User getUserByUsername(String username) {
        return userDao.findByUsername(username);
    }

    // other user-related service methods | update profile, get balance, ect
}