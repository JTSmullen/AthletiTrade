package com.athletitrade.dao;

import com.athletitrade.model.User;
import org.springframework.data.repository.CrudRepository;

// user data access objects (JDBC calls)

public interface UserDao extends CrudRepository<User, Integer> {
    User findByUsername(String username);
    User findByEmail(String email);
}