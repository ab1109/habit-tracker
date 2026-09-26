package com.habittracker.habits.domain;

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
}
