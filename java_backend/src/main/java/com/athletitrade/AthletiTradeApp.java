package com.athletitrade;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication // **Keep only @SpringBootApplication - minimal configuration**
public class AthletiTradeApp {

    public static void main(String[] args) {
        SpringApplication.run(AthletiTradeApp.class, args);
    }
}