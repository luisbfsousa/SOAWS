package com.ecommerce.catalogueservice.config;

import okhttp3.OkHttpClient;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        OkHttpClient okHttpClient = new OkHttpClient();
        return builder.requestFactory(() -> new OkHttp3ClientHttpRequestFactory(okHttpClient)).build();
    }
}
