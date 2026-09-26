package com.habittracker.habits.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "habits")
class HabitJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_type", nullable = false)
    private String ownerType;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Column(name = "schedule_type", nullable = false)
    private String scheduleType;

    @Column(name = "schedule_params", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String scheduleParams;

    @Column(name = "archived_at")
    private Instant archivedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected HabitJpaEntity() {
        // Hibernate requires a no-arg constructor to build this object via
        // reflection when loading a row; our own code never calls this one.
    }

    HabitJpaEntity(UUID id, String ownerType, UUID ownerId, String name,
                   String scheduleType, String scheduleParams,
                   Instant archivedAt, Instant createdAt) {
        this.id = id;
        this.ownerType = ownerType;
        this.ownerId = ownerId;
        this.name = name;
        this.scheduleType = scheduleType;
        this.scheduleParams = scheduleParams;
        this.archivedAt = archivedAt;
        this.createdAt = createdAt;
    }

    UUID getId() { return id; }
    String getOwnerType() { return ownerType; }
    UUID getOwnerId() { return ownerId; }
    String getName() { return name; }
    String getScheduleType() { return scheduleType; }
    String getScheduleParams() { return scheduleParams; }
    Instant getArchivedAt() { return archivedAt; }
    Instant getCreatedAt() { return createdAt; }
}
