package com.inditex.product.client.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

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
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(200);
        cm.setDefaultMaxPerRoute(50);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(Timeout.ofSeconds(simuladoApiConnectTimeout))
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .setDefaultRequestConfig(requestConfig)

                .evictIdleConnections(TimeValue.ofSeconds(15))
                .build();

        HttpComponentsClientHttpRequestFactory rf = new HttpComponentsClientHttpRequestFactory(httpClient);
        rf.setConnectionRequestTimeout(simuladoApiConnectTimeout);
        rf.setReadTimeout(simuladoApiReadTimeout);

        return new RestTemplateBuilder()
                .rootUri(simuladoApiHost)
                .requestFactory(() -> rf)
                .build();
    }
}
