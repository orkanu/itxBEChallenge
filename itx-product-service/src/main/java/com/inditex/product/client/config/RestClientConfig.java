package com.inditex.product.client.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Value("${simulado.api.host}")
    private String simuladoApiHost;
    @Value("${simulado.api.read.timeout:5000}")
    private Integer simuladoApiReadTimeout;
    @Value("${simulado.api.connect.timeout:5000}")
    private Integer simuladoApiConnectTimeout;

    @Bean("simuladoProductClient")
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .rootUri(simuladoApiHost)
                .connectTimeout(Duration.ofMillis(simuladoApiConnectTimeout))
                .readTimeout(Duration.ofMillis(simuladoApiReadTimeout))
                .build();
    }
}
