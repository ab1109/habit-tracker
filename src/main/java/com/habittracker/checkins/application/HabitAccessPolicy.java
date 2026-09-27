package com.habittracker.checkins.application;

import com.habittracker.common.domain.ForbiddenException;
import com.habittracker.common.domain.NotFoundException;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.HabitRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Who may record check-ins for a habit, and who may read its history and
 * progress. Kept in one place so the check-in, history and streak paths
 * can't drift apart.
 */
@Component
public class HabitAccessPolicy {

    private final HabitRepository habitRepository;

    public HabitAccessPolicy(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    public Habit requireCheckInAllowed(UUID habitId, UUID userId) {
        Habit habit = load(habitId);
        if (!isOwner(habit, userId)) {
            throw new ForbiddenException("Only the habit's owner can check in to it");
        }
        return habit;
    }

    public Habit requireViewable(UUID habitId, UUID viewerId) {
        Habit habit = load(habitId);
        if (!isOwner(habit, viewerId)) {
            throw new ForbiddenException("You don't have access to this habit");
        }
        return habit;
    }

    private Habit load(UUID habitId) {
        return habitRepository.findById(habitId)
            .orElseThrow(() -> new NotFoundException("Habit not found: " + habitId));
    }

    private static boolean isOwner(Habit habit, UUID userId) {
        return habit.ownerType() == OwnerType.USER && habit.ownerId().equals(userId);
    }
}
