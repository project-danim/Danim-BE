 package com.project.danim_be.security.config;


 import org.springframework.context.annotation.Configuration;
 import org.springframework.web.servlet.config.annotation.CorsRegistry;
 import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

 @Configuration
 public class WebMvcConfig implements WebMvcConfigurer {
 	@Override
 	public void addCorsMappings(CorsRegistry registry) {
 		registry.addMapping("/**")
 			.allowedOrigins("https://da-nim.com:443")
                .allowedMethods("*")
                .allowedHeaders("*")
        ;
 	}
 }
