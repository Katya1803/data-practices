package com.dvdrental;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DvdRentalApplication {
    public static void main(String[] args) {
        SpringApplication.run(DvdRentalApplication.class, args);
    }
}
