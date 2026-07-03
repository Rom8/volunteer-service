package ru.rom8.rescue.volunteer.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

public class EnvTest {

    @Test
    void printEnv() {
        System.out.println("=== Environment Variables ===");
        Map<String, String> env = System.getenv();
        for (String key : env.keySet()) {
            if (key.toLowerCase().contains("docker") || key.toLowerCase().contains("testcontainer") || key.toLowerCase().contains("ryuk")) {
                System.out.println(key + " = " + env.get(key));
            }
        }
        System.out.println("=== System Properties ===");
        System.getProperties().stringPropertyNames().stream()
                .filter(key -> key.toLowerCase().contains("docker") || key.toLowerCase().contains("testcontainer") || key.toLowerCase().contains("ryuk"))
                .forEach(key -> System.out.println(key + " = " + System.getProperty(key)));
    }
}
