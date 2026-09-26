package com.habittracker.habits.api;

import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.NTimesPerWeekSchedule;
import com.habittracker.habits.domain.ScheduleType;
import com.habittracker.habits.domain.SpecificWeekdaysSchedule;

import java.time.DayOfWeek;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record HabitResponse(
    UUID id,
    OwnerType ownerType,
    UUID ownerId,
    String name,
    ScheduleType scheduleType,
    Integer timesPerWeek,
    Set<DayOfWeek> weekdays,
    Instant archivedAt,
    Instant createdAt
) {

    public static HabitResponse from(Habit habit) {
        Integer timesPerWeek = habit.schedule() instanceof NTimesPerWeekSchedule s ? s.timesPerWeek() : null;
        Set<DayOfWeek> weekdays = habit.schedule() instanceof SpecificWeekdaysSchedule s ? s.weekdays() : null;

        return new HabitResponse(
            habit.id(),
            habit.ownerType(),
            habit.ownerId(),
            habit.name(),
            habit.schedule().type(),
            timesPerWeek,
            weekdays,
            habit.archivedAt(),
            habit.createdAt()
        );
    }
}
