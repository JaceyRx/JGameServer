package com.jacey.game.chat.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource(locations = { "classpath:spring.xml" })
public class SpringConfig {

	public SpringConfig() {
	}
}
