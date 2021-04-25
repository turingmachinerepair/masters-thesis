package org.thesis.functionary;

import org.springframework.beans.factory.annotation.Autowired;
import org.thesis.functionary.Functionary.*;


import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class FunctionaryApplication {


	public static void main(String[] args) {
		SpringApplication.run(FunctionaryApplication.class, args);
	}
	
	
    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

}
