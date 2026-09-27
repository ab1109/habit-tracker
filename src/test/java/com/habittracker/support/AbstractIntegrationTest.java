package com.habittracker.support;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests: boots the real Spring application context
 * against a throwaway Postgres container (not the persistent dev database on
 * 5433), so tests can prove real DB constraints without touching dev data.
 *
 * <p>The container is a JVM-wide singleton, started once and stopped by
 * Testcontainers' Ryuk reaper on exit. It deliberately does not use
 * {@code @Testcontainers}/{@code @Container}: that lifecycle stops the
 * container after each test class, but Spring caches the application context
 * across classes — so the second class would get a context still pointing at
 * the first, now-dead container's port.
 */
@SpringBootTest
@AutoConfigureMockMvc
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        // Tests drive WeeklyDigestService with explicit instants instead.
        registry.add("habit-tracker.notifications.scheduler.enabled", () -> "false");
    }
}
