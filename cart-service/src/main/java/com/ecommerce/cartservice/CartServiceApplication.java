package com.ecommerce.cartservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class CartServiceApplication{

    public static void main(String[] args){
        SpringApplication.run(CartServiceApplication.class, args);
    }
    //synchronous HTTP client used to call other services
    @Bean
    public RestTemplate restTemplate(){
        return new RestTemplate();
    }
}
