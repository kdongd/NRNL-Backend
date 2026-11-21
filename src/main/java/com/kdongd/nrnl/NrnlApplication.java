package com.kdongd.nrnl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class NrnlApplication {

	public static void main(String[] args) {
		SpringApplication.run(NrnlApplication.class, args);
	}

}
