package com.athletitrade.dao;

import org.springframework.data.repository.CrudRepository;

import com.athletitrade.model.User;

public interface UserDao extends CrudRepository<User, Integer> {
    User findByUsername(String username);

    User findByEmail(String email);
}