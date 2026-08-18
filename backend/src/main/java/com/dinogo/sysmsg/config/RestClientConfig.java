package com.dinogo.sysmsg.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** 外部 member、seller、order 模組共用的 RestClient Builder。 */
@Configuration
public class RestClientConfig {
    @Bean
    RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }
}
