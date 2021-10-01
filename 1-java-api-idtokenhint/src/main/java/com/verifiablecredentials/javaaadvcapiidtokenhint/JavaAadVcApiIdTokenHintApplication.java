package com.verifiablecredentials.javaaadvcapiidtokenhint;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class JavaAadVcApiIdTokenHintApplication {

	public static void main(String[] args) {
		SpringApplication.run(JavaAadVcApiIdTokenHintApplication.class, args);
	}

}
