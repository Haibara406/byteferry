package com.byteferry.byteferry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ByteFerryApplication {

    public static void main(String[] args) {
        SpringApplication.run(ByteFerryApplication.class, args);
    }

}