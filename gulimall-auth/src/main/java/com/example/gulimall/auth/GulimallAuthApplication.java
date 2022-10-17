package com.example.gulimall.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;


@EnableFeignClients("com.example.gulimall.auth.feign")
@EnableDiscoveryClient
@SpringBootApplication
public class GulimallAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(GulimallAuthApplication.class, args);
    }

}
