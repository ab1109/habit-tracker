package com.habittracker.streaks.domain;

import java.util.List;

/**
 * Everything derived from a habit's check-in history as of one local date.
 * Never stored: recomputed on every read.
 *
 * @param currentPeriodMet  today (or this week, for {@link StreakUnit#WEEKS})
 *                          is already satisfied and is included in the streak
 * @param currentPeriodOpen today/this week is required but not yet satisfied;
 *                          it is still in progress, so it does not break the streak
 * @param weeks             the last twelve ISO weeks, oldest first, ending with the current one
 */
public record HabitProgress(
    StreakUnit streakUnit,
    int currentStreak,
    int longestStreak,
    boolean currentPeriodMet,
    boolean currentPeriodOpen,
    WeekProgress thisWeek,
    List<WeekProgress> weeks
) {
}
