package com.habittracker.notifications.infrastructure;

import com.habittracker.notifications.domain.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * The initial delivery mechanism: writes the notification to the log. The
 * delivery log in the database is what users see; replace this bean with an
 * email or push sender when users have a contact address.
 */
@Component
class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(UUID userId, String subject, String body) {
        log.info("Notification to {}: {}\n{}", userId, subject, body);
    }
}
