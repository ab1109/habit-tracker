package com.habittracker.streaks.domain;

import java.time.LocalDate;

/**
 * @param weekStart the Monday of the ISO week
 * @param done      qualifying check-in days in the week, capped at {@code target}
 * @param target    how many qualifying days the schedule asks for in a week
 */
public record WeekProgress(LocalDate weekStart, int done, int target) {

    public LocalDate weekEnd() {
        return weekStart.plusDays(6);
    }

    public boolean met() {
        return done >= target;
    }
}
