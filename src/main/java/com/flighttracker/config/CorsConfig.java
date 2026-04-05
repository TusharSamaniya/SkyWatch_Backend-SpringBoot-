package com.flighttracker.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
	
	public WebMvcConfigurer corsConfiguration() {
		
		return new WebMvcConfigurer() {
			
			public void addCorsMappings(CorsRegistry registry) {
				registry.addMapping("/api/**")
				.allowedOrigins("http://localhost:3000")
				.allowedMethods("GET", "POST", "PUT", "DELETE")
				.allowedHeaders("*");
			}
		};
		
	}

}
