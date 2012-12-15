package net.iteach.service.config;

import net.iteach.core.security.SecurityUtils;
import net.iteach.service.security.DefaultSecurityUtils;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DefaultConfiguration {
	
	@Bean
	public SecurityUtils securityUtils() {
		return new DefaultSecurityUtils();
	}

}