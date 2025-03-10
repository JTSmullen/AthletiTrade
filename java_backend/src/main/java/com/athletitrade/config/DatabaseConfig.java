package com.athletitrade.config;

import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

/*
    Finished.
    Launches and inits sql database in resources/database/schema.sql on build
    Will reset the database and recreate the tables each run.
 */

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("database/schema.sql") // Loads the schema on launch
                .build();
    }
}