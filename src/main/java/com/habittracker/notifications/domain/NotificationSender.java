package com.habittracker.notifications.domain;

import java.util.UUID;

/**
 * The delivery channel. Throwing means the attempt failed; the caller
 * records the failure and carries on with other recipients.
 */
public interface NotificationSender {

    void send(UUID userId, String subject, String body);
}
