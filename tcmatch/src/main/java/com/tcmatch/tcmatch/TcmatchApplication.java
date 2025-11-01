package com.tcmatch.tcmatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TcmatchApplication {

	public static void main(String[] args) {
		SpringApplication.run(TcmatchApplication.class, args);
	}

}
