package com.zj;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EasyMediaApplication {
	
	private static String jsonPath = "";

	public static void main(String[] args) {
		
		for (int i = 0; i < args.length; i++) {
			if ("-jsonPath".equals(args[i])) {
//                audio = true;
				System.out.println(args[i+1]);
            }
		}
		
//        System.setProperty("spring.devtools.restart.enabled", "false");

		SpringApplication.run(EasyMediaApplication.class, args);
	}
}
