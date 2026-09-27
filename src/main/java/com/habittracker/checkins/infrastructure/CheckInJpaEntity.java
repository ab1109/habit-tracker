package com.habittracker.checkins.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "checkins")
class CheckInJpaEntity {

    @Id
    private UUID id;

    @Column(name = "habit_id", nullable = false)
    private UUID habitId;

    @Column(name = "group_id")
    private UUID groupId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "performed_by_user_id", nullable = false)
    private UUID performedByUserId;

    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;

    @Column(name = "local_date", nullable = false)
    private LocalDate localDate;

    protected CheckInJpaEntity() {
        // Read-only mapping: rows are inserted with a native INSERT ... ON
        // CONFLICT DO NOTHING (see CheckInJpaSpringDataRepository).
    }

    UUID getId() { return id; }
    UUID getHabitId() { return habitId; }
    UUID getGroupId() { return groupId; }
    UUID getUserId() { return userId; }
    UUID getPerformedByUserId() { return performedByUserId; }
    Instant getRecordedAt() { return recordedAt; }
    LocalDate getLocalDate() { return localDate; }
}
