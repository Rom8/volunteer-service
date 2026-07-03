package ru.rom8.rescue.volunteer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Нужно для swagger UI
 */
@Configuration
@Profile("dev")
public class CorsConfig implements WebMvcConfigurer {

    private static final String[] ALLOWED_ORIGINS = {
            "http://localhost:8081",
            "http://127.0.0.1:8081"
    };

    private static final String[] ALLOWED_METHODS = {
            "GET",
            "POST",
            "PUT",
            "PATCH",
            "DELETE",
            "OPTIONS"
    };

    private static final String ALL_PATHS_PATTERN = "/**";
    private static final String ALL_HEADERS_PATTERN = "*";

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(ALL_PATHS_PATTERN)
                .allowedOrigins(ALLOWED_ORIGINS)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALL_HEADERS_PATTERN);
    }
}
