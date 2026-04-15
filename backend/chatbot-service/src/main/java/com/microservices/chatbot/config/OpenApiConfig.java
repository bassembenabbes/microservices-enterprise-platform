package com.microservices.chatbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration OpenAPI/Swagger pour la documentation automatique de l'API
 */
@Configuration
public class OpenApiConfig {
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("Chatbot Service API")
                .version("1.0.0")
                .description("API du micro-service chatbot IA avec intégration Gemini et RAG")
                .contact(new Contact()
                    .name("Équipe Backend")
                    .email("backend@microservices.local")
                    .url("https://github.com/microservices/chatbot-service")
                )
                .license(new License()
                    .name("Apache 2.0")
                    .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                )
            )
            .servers(List.of(
                new Server()
                    .url("http://localhost:8005")
                    .description("Serveur local"),
                new Server()
                    .url("http://chatbot-service:8005")
                    .description("Serveur Docker"),
                new Server()
                    .url("https://api.example.com/chatbot")
                    .description("Serveur production")
            ));
    }
}

