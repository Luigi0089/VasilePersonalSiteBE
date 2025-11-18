package it.vasilepersonalsite.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class FeignConfig {

    @Value("${github.token}")
    private String githubToken;

    @Bean
    public RequestInterceptor githubAuthInterceptor() {
        return template -> {
            template.header("User-Agent", "Vasile-Personal-Site/1.0");
            if (githubToken != null && !githubToken.isBlank()) {
                template.header("Authorization", "Bearer " + githubToken);
            }
        };
    }
}