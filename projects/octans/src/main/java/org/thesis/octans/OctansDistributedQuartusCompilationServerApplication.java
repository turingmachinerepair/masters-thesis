package org.thesis.octans;

import org.springframework.beans.factory.annotation.Autowired;
import org.thesis.octans.Arbiter.*;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class OctansDistributedQuartusCompilationServerApplication {

    //@Autowired Arbiter arbiter;

	public static void main(String[] args) {
		SpringApplication.run(OctansDistributedQuartusCompilationServerApplication.class, args);
	}
	
	
    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        return String.format("Hello %s!", name);
    }

}
