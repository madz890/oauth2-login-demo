package com.sambrana.oauth2login;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableTransactionManagement
@SpringBootApplication
public class Oath2LoginDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(Oath2LoginDemoApplication.class, args);
    }
}