package com.autocode.history;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class HistoryIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(HistoryIndexerApplication.class, args);
    }
}
