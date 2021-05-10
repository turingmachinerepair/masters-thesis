package org.thesis.quadomizer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Класс приложения Spring Boot микросервиса
 */
@SpringBootApplication
public class QuadomizerApplication {

	public static void main(String[] args) {

		SpringApplication.run(QuadomizerApplication.class, args);
		Quadomizer logic = new Quadomizer();
		//logic.run();
	}

}
