package br.com.automationcode.velocity_cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class VelocityCartApplication {

	public static void main(String[] args) {
		SpringApplication.run(VelocityCartApplication.class, args);
	}

}