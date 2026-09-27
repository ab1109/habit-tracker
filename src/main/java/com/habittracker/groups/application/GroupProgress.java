package com.habittracker.groups.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.groups.domain.Group;
import com.habittracker.habits.domain.Habit;
import com.habittracker.streaks.domain.HabitProgress;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A circle's progress for one week, aggregated from its members' shared
 * habits and its joint habits. Derived on every read; nothing here is stored.
 */
public record GroupProgress(
    Group group,
    LocalDate weekStart,
    int totalDone,
    int totalTarget,
    List<SharedHabitProgress> sharedHabits,
    List<JointHabitProgress> jointHabits,
    List<Activity> recentActivity
) {

    public LocalDate weekEnd() {
        return weekStart.plusDays(6);
    }

    public record SharedHabitProgress(Habit habit, UUID ownerUserId, String ownerDisplayName, HabitProgress progress) {
    }

    /** @param coverage this week's check-ins by the member who performed them */
    public record JointHabitProgress(Habit habit, HabitProgress progress, List<Coverage> coverage) {
    }

    public record Coverage(UUID userId, String displayName, int count) {
    }

    public record Activity(CheckIn checkIn, Habit habit, String displayName) {
    }
}
