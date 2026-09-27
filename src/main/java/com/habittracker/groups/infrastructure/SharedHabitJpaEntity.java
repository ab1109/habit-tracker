package com.habittracker.groups.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "group_shared_habits")
@IdClass(SharedHabitJpaEntity.Key.class)
class SharedHabitJpaEntity {

    record Key(UUID groupId, UUID habitId) implements Serializable {
        Key() {
            this(null, null);
        }
    }

    @Id
    @Column(name = "group_id")
    private UUID groupId;

    @Id
    @Column(name = "habit_id")
    private UUID habitId;

    @Column(name = "shared_by_user_id", nullable = false)
    private UUID sharedByUserId;

    @Column(name = "shared_at", nullable = false)
    private Instant sharedAt;

    protected SharedHabitJpaEntity() {
    }

    SharedHabitJpaEntity(UUID groupId, UUID habitId, UUID sharedByUserId, Instant sharedAt) {
        this.groupId = groupId;
        this.habitId = habitId;
        this.sharedByUserId = sharedByUserId;
        this.sharedAt = sharedAt;
    }

    UUID getHabitId() { return habitId; }
}
