package com.autocode.codegraph;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@ConfigurationPropertiesScan
@SpringBootApplication
public class CodegraphRunnerApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodegraphRunnerApplication.class, args);
    }
}
