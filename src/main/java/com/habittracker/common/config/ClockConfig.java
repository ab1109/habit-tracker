package com.habittracker.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * "Now" is injected rather than read with {@code Instant.now()} wherever a
 * workflow's outcome depends on the current date (streaks, weekly digests),
 * so tests can pin it.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
