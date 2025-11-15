package com.demo.service.a;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration class for creating a RestTemplate bean used for making HTTP client requests.
 * This allows the application to easily inject a pre-configured RestTemplate instance where needed.
 *
 * The RestTemplate created by this configuration can be utilized to interact with external services
 * by simplifying HTTP operations such as GET, POST, PUT, DELETE, etc.
 *
 * Features provided by using this RestTemplate can include integration with tracing,
 * connection pooling, and other customizations implemented through the RestTemplateBuilder.
 */
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .build();
    }
}
