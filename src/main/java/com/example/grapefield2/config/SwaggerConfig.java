package com.example.grapefield2.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Value("${server.port:8080}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .openapi("3.1.0")
                .info(new Info()
                        .title("GrapeField 2.0 API")
                        .description("한국 공연 정보 통합 플랫폼 API")
                        .version("v2.0")
                        .contact(new Contact()
                                .name("김지원")
                                .email("j0a0j@naver.com")
                                .url("https://github.com/J0a0J/Grapefield-2.0")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("로컬 개발 서버"),
                        new Server()
                                .url("https://grapefield-2.kro.kr")
                                .description("운영 서버")
                ));
    }
}
