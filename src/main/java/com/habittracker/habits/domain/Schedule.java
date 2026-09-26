package com.habittracker.habits.domain;

/**
 * How often a habit is expected to be done. Exactly three kinds exist —
 * see the permits clause — so any code that switches over a Schedule
 * (e.g. the future streak calculator) is checked by the compiler for
 * completeness: forget a case, and the build fails, not the user's request.
 */
public sealed interface Schedule permits DailySchedule, NTimesPerWeekSchedule, SpecificWeekdaysSchedule {

    ScheduleType type();
}
