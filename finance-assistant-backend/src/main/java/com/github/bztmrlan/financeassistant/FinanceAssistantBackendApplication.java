package com.github.bztmrlan.financeassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.retry.annotation.EnableRetry;

@SpringBootApplication
@EnableScheduling
@EnableRetry
public class FinanceAssistantBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(FinanceAssistantBackendApplication.class, args);
	}

}
