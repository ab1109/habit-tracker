package com.habittracker.habits.application;

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
 *
 * <ul>
 *   <li>Personal habit: only the owner checks in; the owner and members of
 *       any circle it is shared with may view it.</li>
 *   <li>Joint habit (owned by a circle): any member may check in and view.</li>
 * </ul>
 */
@Component
public class HabitAccessPolicy {

    private final HabitRepository habitRepository;
    private final CircleMembership circleMembership;

    public HabitAccessPolicy(HabitRepository habitRepository, CircleMembership circleMembership) {
        this.habitRepository = habitRepository;
        this.circleMembership = circleMembership;
    }

    public Habit requireCheckInAllowed(UUID habitId, UUID userId) {
        Habit habit = load(habitId);
        boolean allowed = switch (habit.ownerType()) {
            case USER -> habit.ownerId().equals(userId);
            case GROUP -> circleMembership.isMember(habit.ownerId(), userId);
        };
        if (!allowed) {
            throw new ForbiddenException(habit.ownerType() == OwnerType.USER
                ? "Only the habit's owner can check in to it"
                : "Only members of the circle can check in to its joint habits");
        }
        return habit;
    }

    /** Archiving follows the same rule as checking in: the owner, or any member for a joint habit. */
    public Habit requireCanArchive(UUID habitId, UUID userId) {
        try {
            return requireCheckInAllowed(habitId, userId);
        } catch (ForbiddenException e) {
            throw new ForbiddenException("Only the habit's owner (or, for a joint habit, its circle) can archive it");
        }
    }

    public Habit requireViewable(UUID habitId, UUID viewerId) {
        Habit habit = load(habitId);
        boolean allowed = switch (habit.ownerType()) {
            case USER -> habit.ownerId().equals(viewerId) || circleMembership.isSharedWithCircleOf(habitId, viewerId);
            case GROUP -> circleMembership.isMember(habit.ownerId(), viewerId);
        };
        if (!allowed) {
            throw new ForbiddenException("You don't have access to this habit");
        }
        return habit;
    }

    private Habit load(UUID habitId) {
        return habitRepository.findById(habitId)
            .orElseThrow(() -> new NotFoundException("Habit not found: " + habitId));
    }
}
