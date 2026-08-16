package br.com.techmind.classificador.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    OpenAPI techMindOpenAPI() {
        return new OpenAPI()
                .components(new Components())
                .info(new Info()
                        .title("TechMind Classificador API")
                        .version("v1")
                        .description("API para classificação automática de artigos técnicos.")
                        .contact(new Contact().name("TechMind")));
    }
}
