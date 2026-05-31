package com.videsol.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class VidesolBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(VidesolBackendApplication.class, args);
    }
}
