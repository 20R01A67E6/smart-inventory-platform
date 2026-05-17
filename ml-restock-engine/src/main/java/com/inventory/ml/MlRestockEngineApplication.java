package com.inventory.ml;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MlRestockEngineApplication {
    public static void main(String[] args) {
        SpringApplication.run(MlRestockEngineApplication.class, args);
    }
}
