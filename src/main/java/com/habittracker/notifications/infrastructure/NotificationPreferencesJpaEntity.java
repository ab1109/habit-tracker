package com.habittracker.notifications.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "notification_preferences")
class NotificationPreferencesJpaEntity {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "weekly_digest_enabled", nullable = false)
    private boolean weeklyDigestEnabled;

    @Column(nullable = false)
    private String timezone;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected NotificationPreferencesJpaEntity() {
    }

    NotificationPreferencesJpaEntity(UUID userId, boolean weeklyDigestEnabled, String timezone, Instant updatedAt) {
        this.userId = userId;
        this.weeklyDigestEnabled = weeklyDigestEnabled;
        this.timezone = timezone;
        this.updatedAt = updatedAt;
    }

    UUID getUserId() { return userId; }
    boolean isWeeklyDigestEnabled() { return weeklyDigestEnabled; }
    String getTimezone() { return timezone; }
}
