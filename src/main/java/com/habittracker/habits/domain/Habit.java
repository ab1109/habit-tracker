package com.habittracker.habits.domain;

import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.common.domain.OwnerType;

import java.time.Instant;
import java.util.UUID;

public record Habit(
    UUID id,
    OwnerType ownerType,
    UUID ownerId,
    String name,
    Schedule schedule,
    Instant archivedAt,
    Instant createdAt
) {

    public Habit {
        if (name == null || name.isBlank()) {
            throw new DomainValidationException("name must not be blank");
        }
    }

    public static Habit create(OwnerType ownerType, UUID ownerId, String name, Schedule schedule) {
        return new Habit(
            UUID.randomUUID(),
            ownerType,
            ownerId,
            name,
            schedule,
            null,
            Instant.now()
        );
    }

    public boolean isArchived() {
        return archivedAt != null;
    }

    public Habit archive() {
        return isArchived() ? this : new Habit(id, ownerType, ownerId, name, schedule, Instant.now(), createdAt);
    }
}
