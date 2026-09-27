package com.habittracker.habits.application;

import com.habittracker.common.domain.NotFoundException;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.HabitRepository;
import com.habittracker.habits.domain.Schedule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class HabitService {

    private final HabitRepository habitRepository;
    private final HabitAccessPolicy accessPolicy;

    public HabitService(HabitRepository habitRepository, HabitAccessPolicy accessPolicy) {
        this.habitRepository = habitRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public Habit createHabit(OwnerType ownerType, UUID ownerId, String name, Schedule schedule) {
        Habit habit = Habit.create(ownerType, ownerId, name, schedule);
        return habitRepository.save(habit);
    }

    /** For API callers: the habit, if {@code viewerId} may see it. */
    @Transactional(readOnly = true)
    public Habit getHabit(UUID id, UUID viewerId) {
        return accessPolicy.requireViewable(id, viewerId);
    }

    /** For other modules that have already authorized access (or need none). */
    @Transactional(readOnly = true)
    public Habit getHabit(UUID id) {
        return habitRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Habit not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Habit> activeHabitsOf(OwnerType ownerType, UUID ownerId) {
        return habitRepository.findActiveByOwner(ownerType, ownerId);
    }

    @Transactional
    public Habit archiveHabit(UUID id, UUID requestedBy) {
        Habit habit = accessPolicy.requireCanArchive(id, requestedBy);
        return habitRepository.save(habit.archive());
    }
}
