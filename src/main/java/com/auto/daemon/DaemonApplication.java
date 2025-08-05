package com.auto.daemon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.auto.daemon.domain.repository")
public class DaemonApplication {

	public static void main(String[] args) {
		
		SpringApplication application = new SpringApplicationBuilder(DaemonApplication.class)
				.listeners(new ApplicationPidFileWriter())
				.build();
		
		application.run(args);
	}

}