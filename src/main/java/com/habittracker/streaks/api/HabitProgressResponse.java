package com.habittracker.streaks.api;

import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.ScheduleType;
import com.habittracker.streaks.domain.HabitProgress;
import com.habittracker.streaks.domain.StreakUnit;
import com.habittracker.streaks.domain.WeekProgress;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record HabitProgressResponse(
    UUID habitId,
    ScheduleType scheduleType,
    StreakUnit streakUnit,
    int currentStreak,
    int longestStreak,
    boolean currentPeriodMet,
    boolean currentPeriodOpen,
    Week thisWeek,
    List<Week> weeks
) {

    public record Week(LocalDate weekStart, LocalDate weekEnd, int done, int target, boolean met) {

        static Week from(WeekProgress week) {
            return new Week(week.weekStart(), week.weekEnd(), week.done(), week.target(), week.met());
        }
    }

    public static HabitProgressResponse from(Habit habit, HabitProgress progress) {
        return new HabitProgressResponse(
            habit.id(),
            habit.schedule().type(),
            progress.streakUnit(),
            progress.currentStreak(),
            progress.longestStreak(),
            progress.currentPeriodMet(),
            progress.currentPeriodOpen(),
            Week.from(progress.thisWeek()),
            progress.weeks().stream().map(Week::from).toList()
        );
    }
}
