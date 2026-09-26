package com.habittracker.habits.domain;

import java.time.DayOfWeek;
import java.util.Set;

public record SpecificWeekdaysSchedule(Set<DayOfWeek> weekdays) implements Schedule {

    public SpecificWeekdaysSchedule {
        if (weekdays == null || weekdays.isEmpty()) {
            throw new InvalidScheduleException("weekdays must not be empty");
        }
        weekdays = Set.copyOf(weekdays);
    }

    @Override
    public ScheduleType type() {
        return ScheduleType.SPECIFIC_WEEKDAYS;
    }
}
