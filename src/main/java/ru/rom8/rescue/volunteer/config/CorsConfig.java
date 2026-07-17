package ru.rom8.rescue.volunteer.config;

import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Нужно для swagger UI
 */
@Configuration
@Profile("dev")
@ConfigurationProperties(prefix = "cors")
public class CorsConfig implements WebMvcConfigurer {

    @Setter
    private String[] allowedOrigins;

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
                .allowedOrigins(allowedOrigins)
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALL_HEADERS_PATTERN);
    }
}
