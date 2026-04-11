package com.flighttracker.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry; // Fixed import!
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
	
	@Bean // @Bean goes here on the outer method!
	public WebMvcConfigurer corsConfiguration() {
		
		return new WebMvcConfigurer() {
			
			@Override
			public void addCorsMappings(CorsRegistry registry) { // Removed @Bean from here
				registry.addMapping("/api/**")
		        .allowedOrigins("http://localhost:5173", "https://skyfind.tech", "https://www.skyfind.tech")
		        .allowedMethods("GET", "POST", "PUT", "DELETE")
		        .allowedHeaders("*");
			}
		};
		
	}
}