package com.oms.payment.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI().info(new Info()
                .title("Payment Service")
                .version("1.0.0")
                .description("Simulates payment for OrderCreated events and publishes PaymentCompleted or PaymentFailed."));
    }
}
