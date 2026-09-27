package com.habittracker.habits.domain;

import com.habittracker.common.domain.OwnerType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Port owned by the domain: the application layer depends on this interface,
 * never on JPA directly. {@code infrastructure.HabitRepositoryAdapter} is the
 * only class that implements it.
 */
public interface HabitRepository {

    Habit save(Habit habit);

    Optional<Habit> findById(UUID id);

    /** Non-archived habits of one owner, oldest first. */
    List<Habit> findActiveByOwner(OwnerType ownerType, UUID ownerId);
}
