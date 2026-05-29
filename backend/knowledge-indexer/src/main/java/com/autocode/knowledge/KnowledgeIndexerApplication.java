package com.autocode.knowledge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class KnowledgeIndexerApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeIndexerApplication.class, args);
    }
}
