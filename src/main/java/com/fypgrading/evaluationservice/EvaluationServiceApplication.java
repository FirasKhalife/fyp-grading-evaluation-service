package com.fypgrading.evaluationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories("com.fypgrading.evaluationservice.repository")
public class EvaluationServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EvaluationServiceApplication.class, args);
	}

}
