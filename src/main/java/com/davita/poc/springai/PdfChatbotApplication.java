package com.davita.poc.springai;

import org.springframework.ai.autoconfigure.openai.OpenAiAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = {OpenAiAutoConfiguration.class})
public class PdfChatbotApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfChatbotApplication.class, args);
	}
	
}
