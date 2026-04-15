package com.microservices.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {KafkaAutoConfiguration.class})
@EnableAsync
@EnableScheduling
public class ChatbotApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ChatbotApplication.class, args);
    }
}
