package com.habittracker.habits.api;

import com.habittracker.habits.domain.DailySchedule;
import com.habittracker.habits.domain.InvalidScheduleException;
import com.habittracker.habits.domain.NTimesPerWeekSchedule;
import com.habittracker.habits.domain.Schedule;
import com.habittracker.habits.domain.ScheduleType;
import com.habittracker.habits.domain.SpecificWeekdaysSchedule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.util.Set;

public record CreateHabitRequest(
    @NotBlank String name,
    @NotNull ScheduleType scheduleType,
    Integer timesPerWeek,
    Set<DayOfWeek> weekdays
) {

    public Schedule toSchedule() {
        return switch (scheduleType) {
            case DAILY -> new DailySchedule();
            case N_TIMES_PER_WEEK -> {
                if (timesPerWeek == null) {
                    throw new InvalidScheduleException("timesPerWeek is required for N_TIMES_PER_WEEK");
                }
                yield new NTimesPerWeekSchedule(timesPerWeek);
            }
            case SPECIFIC_WEEKDAYS -> {
                if (weekdays == null || weekdays.isEmpty()) {
                    throw new InvalidScheduleException("weekdays is required for SPECIFIC_WEEKDAYS");
                }
                yield new SpecificWeekdaysSchedule(weekdays);
            }
        };
    }
}
