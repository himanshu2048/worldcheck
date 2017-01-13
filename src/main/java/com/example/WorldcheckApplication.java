package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@ComponentScan("com.hsbc")
public class WorldcheckApplication {

	public static void main(String[] args) {
		SpringApplication.run(WorldcheckApplication.class, args);
	}
}
