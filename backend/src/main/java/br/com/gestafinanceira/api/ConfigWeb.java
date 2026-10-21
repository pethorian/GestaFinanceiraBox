package br.com.gestafinanceira.api;

import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class ConfigWeb {

    /** Relógio injetável (testes substituem por um Clock fixo). */
    @Bean
    Clock clock() {
        return Clock.systemDefaultZone();
    }

    /** Libera o frontend de desenvolvimento (Vite) a chamar a API. Origens em app.cors-origins. */
    @Bean
    WebMvcConfigurer cors(@Value("${app.cors-origins}") String[] origens) {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins(origens)
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }
        };
    }
}
