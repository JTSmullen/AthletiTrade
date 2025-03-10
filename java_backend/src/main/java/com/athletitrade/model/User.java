package com.athletitrade.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("USERS")
public class User {
    @Id
    private Integer userId;
    private String username;
    private String password; // hash passwords
    private String email;
    private Double balance;

    public User() {} // init empty instance

    public User(String username, String password, String email, Double balance) {
        this.username = username;
        this.password = password;
        this.email = email;
        this.balance = balance;
    } // init instance with info

    /*
        Getters and Setters
     */
    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Double getBalance() {
        return balance;
    }

    public void setBalance(Double balance) {
        this.balance = balance;
    }
}