package com.pedidai.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.security.autoconfigure.UserDetailsServiceAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// Sin usuario en memoria con contraseña generada: la autenticación es solo por JWT (SecurityConfig)
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
@EnableAsync
@EnableScheduling
public class PedidaiApplication {

	public static void main(String[] args) {
		SpringApplication.run(PedidaiApplication.class, args);
	}

}
