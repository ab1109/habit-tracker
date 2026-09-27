package com.habittracker.notifications.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "notification_deliveries")
class NotificationDeliveryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(nullable = false)
    private String kind;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private String subject;

    @Column(nullable = false)
    private String body;

    @Column
    private String error;

    @Column(name = "attempted_at", nullable = false)
    private Instant attemptedAt;

    protected NotificationDeliveryJpaEntity() {
    }

    NotificationDeliveryJpaEntity(UUID id, UUID userId, UUID groupId, String kind, LocalDate periodStart,
                                  String status, String subject, String body, String error, Instant attemptedAt) {
        this.id = id;
        this.userId = userId;
        this.groupId = groupId;
        this.kind = kind;
        this.periodStart = periodStart;
        this.status = status;
        this.subject = subject;
        this.body = body;
        this.error = error;
        this.attemptedAt = attemptedAt;
    }

    UUID getId() { return id; }
    UUID getUserId() { return userId; }
    UUID getGroupId() { return groupId; }
    String getKind() { return kind; }
    LocalDate getPeriodStart() { return periodStart; }
    String getStatus() { return status; }
    String getSubject() { return subject; }
    String getBody() { return body; }
    String getError() { return error; }
    Instant getAttemptedAt() { return attemptedAt; }
}
