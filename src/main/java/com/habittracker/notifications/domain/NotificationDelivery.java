package com.habittracker.notifications.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A record of one attempt to deliver a notification.
 *
 * @param periodStart the week (its Monday) the notification is about
 * @param error       why delivery failed; null when sent
 */
public record NotificationDelivery(
    UUID id,
    UUID userId,
    UUID groupId,
    NotificationKind kind,
    LocalDate periodStart,
    DeliveryStatus status,
    String subject,
    String body,
    String error,
    Instant attemptedAt
) {

    public static NotificationDelivery sent(UUID userId, UUID groupId, NotificationKind kind, LocalDate periodStart,
                                            String subject, String body, Instant at) {
        return new NotificationDelivery(UUID.randomUUID(), userId, groupId, kind, periodStart,
            DeliveryStatus.SENT, subject, body, null, at);
    }

    public static NotificationDelivery failed(UUID userId, UUID groupId, NotificationKind kind, LocalDate periodStart,
                                              String subject, String body, String error, Instant at) {
        return new NotificationDelivery(UUID.randomUUID(), userId, groupId, kind, periodStart,
            DeliveryStatus.FAILED, subject, body, error, at);
    }
}
