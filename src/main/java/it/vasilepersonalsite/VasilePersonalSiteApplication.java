package it.vasilepersonalsite;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients(basePackages = "it.vasilepersonalsite")
@EnableCaching
public class VasilePersonalSiteApplication {

	public static void main(String[] args) {
		SpringApplication.run(VasilePersonalSiteApplication.class, args);
	}

}
