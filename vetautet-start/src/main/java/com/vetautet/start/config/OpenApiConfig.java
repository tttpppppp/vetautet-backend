package com.vetautet.start.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI vetautetOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("VeTau API")
                        .version("v1")
                        .description("OpenAPI documentation for VeTau backend"));
    }
}
