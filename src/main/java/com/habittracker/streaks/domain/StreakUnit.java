package com.habittracker.streaks.domain;

/**
 * What one link of a streak is, which depends on the schedule type.
 */
public enum StreakUnit {
    /** Daily habits: consecutive calendar days. */
    DAYS,
    /** N-times-per-week habits: consecutive ISO weeks that met the target. */
    WEEKS,
    /** Specific-weekday habits: consecutive scheduled days; other days are skipped. */
    SCHEDULED_DAYS
}
