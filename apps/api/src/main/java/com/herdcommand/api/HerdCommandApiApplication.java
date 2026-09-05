package com.herdcommand.api;

import com.herdcommand.api.config.LocalDotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HerdCommandApiApplication {

    public static void main(String[] args) {
        LocalDotenv.load();
        SpringApplication.run(HerdCommandApiApplication.class, args);
    }
}
