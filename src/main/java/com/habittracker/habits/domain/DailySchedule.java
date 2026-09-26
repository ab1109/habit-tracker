package com.habittracker.habits.domain;

public record DailySchedule() implements Schedule {

    @Override
    public ScheduleType type() {
        return ScheduleType.DAILY;
    }
}
