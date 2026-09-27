package com.habittracker.notifications.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;

/**
 * Runs hourly because "Sunday 18:00" happens at a different instant in each
 * member's timezone. Disable with
 * {@code habit-tracker.notifications.scheduler.enabled=false}.
 */
@Component
@ConditionalOnProperty(name = "habit-tracker.notifications.scheduler.enabled", matchIfMissing = true)
class WeeklyDigestJob {

    private static final Logger log = LoggerFactory.getLogger(WeeklyDigestJob.class);

    private final WeeklyDigestService digestService;
    private final Clock clock;

    WeeklyDigestJob(WeeklyDigestService digestService, Clock clock) {
        this.digestService = digestService;
        this.clock = clock;
    }

    @Scheduled(cron = "0 0 * * * *")
    void run() {
        int sent = digestService.sendDue(clock.instant());
        if (sent > 0) {
            log.info("Sent {} weekly digest(s)", sent);
        }
    }
}
