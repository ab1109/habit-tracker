package com.habittracker.habits.domain;

public record NTimesPerWeekSchedule(int timesPerWeek) implements Schedule {

    public NTimesPerWeekSchedule {
        if (timesPerWeek < 1 || timesPerWeek > 7) {
            throw new InvalidScheduleException(
                "timesPerWeek must be between 1 and 7, got " + timesPerWeek);
        }
    }

    @Override
    public ScheduleType type() {
        return ScheduleType.N_TIMES_PER_WEEK;
    }
}
