package com.ppesafety.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class PpeSafetyApplication {

    public static void main(String[] args) {
        SpringApplication.run(PpeSafetyApplication.class, args);
    }
}
