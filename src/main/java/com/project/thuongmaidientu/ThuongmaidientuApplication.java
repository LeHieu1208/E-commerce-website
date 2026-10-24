package com.project.thuongmaidientu;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ThuongmaidientuApplication {

	public static void main(String[] args) {
		SpringApplication.run(ThuongmaidientuApplication.class, args);
	}

}