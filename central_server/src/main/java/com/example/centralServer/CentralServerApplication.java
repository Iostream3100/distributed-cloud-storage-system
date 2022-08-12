package com.example.centralServer;

//import com.example.centralServer.storage.StorageService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
//@EnableConfigurationProperties(StorageProperties.class)
public class CentralServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(CentralServerApplication.class, args);
	}

	@Bean
	CommandLineRunner init() {
		return (args) -> {
//			storageService.deleteAll();
//			storageService.init();
		};
	}
}
