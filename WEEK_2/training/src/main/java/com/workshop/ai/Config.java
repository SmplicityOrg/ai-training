package com.workshop.ai;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

	
	
	@Bean
	public ChatClient getChatClient(ChatClient.Builder builder) {
		return builder.build();
	}
}
