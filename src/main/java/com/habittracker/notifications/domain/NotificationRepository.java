package com.habittracker.notifications.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Optional<NotificationPreferences> findPreferences(UUID userId);

    NotificationPreferences savePreferences(NotificationPreferences preferences);

    void recordDelivery(NotificationDelivery delivery);

    boolean wasSent(UUID userId, UUID groupId, NotificationKind kind, LocalDate periodStart);

    /** Newest first. */
    List<NotificationDelivery> findDeliveries(UUID userId, int limit);
}
