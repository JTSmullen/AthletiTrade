package com.athletitrade; // Keep the package as it is

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@ComponentScan(basePackages = "com.athletitrade") // Scan the entire com.athletitrade package
public class AthletiTradeApp {

    public static void main(String[] args) {
        SpringApplication.run(AthletiTradeApp.class, args);
    }
}