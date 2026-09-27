package com.habittracker.streaks.domain;

import com.habittracker.habits.domain.DailySchedule;
import com.habittracker.habits.domain.NTimesPerWeekSchedule;
import com.habittracker.habits.domain.Schedule;
import com.habittracker.habits.domain.SpecificWeekdaysSchedule;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Derives streaks and weekly progress from check-in history. A pure function
 * of (schedule, check-in local dates, today) — no clock, no storage.
 *
 * <p>Semantics of the current, incomplete period: if it is already satisfied
 * it counts toward the streak; if not, it is "open" and the streak runs
 * through the previous period without being broken. Check-in dates after
 * {@code today} are ignored — they can exist for a joint habit when another
 * member's timezone is already on the next day.
 */
public final class ProgressCalculator {

    static final int WEEKS_OF_HISTORY = 12;

    private ProgressCalculator() {
    }

    public static HabitProgress calculate(Schedule schedule, Collection<LocalDate> checkInDates, LocalDate today) {
        NavigableSet<LocalDate> dates = new TreeSet<>(new TreeSet<>(checkInDates).headSet(today, true));

        return switch (schedule) {
            case DailySchedule ignored -> byScheduledDays(StreakUnit.DAYS, day -> true, 7, dates, today);
            case SpecificWeekdaysSchedule s -> byScheduledDays(
                StreakUnit.SCHEDULED_DAYS, day -> s.weekdays().contains(day.getDayOfWeek()),
                s.weekdays().size(), dates, today);
            case NTimesPerWeekSchedule s -> byWeeks(s.timesPerWeek(), dates, today);
        };
    }

    public static LocalDate weekStartOf(LocalDate date) {
        return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    /**
     * Daily and specific-weekday habits: each scheduled day is one link. Days
     * outside the schedule are skipped entirely — they neither extend nor
     * break the streak, and check-ins on them don't count.
     */
    private static HabitProgress byScheduledDays(StreakUnit unit, Predicate<LocalDate> scheduled, int weeklyTarget,
                                                 NavigableSet<LocalDate> dates, LocalDate today) {
        NavigableSet<LocalDate> qualifying = new TreeSet<>();
        dates.stream().filter(scheduled).forEach(qualifying::add);

        boolean todayRequired = scheduled.test(today);
        boolean met = todayRequired && qualifying.contains(today);

        int current = 0;
        LocalDate cursor = met ? today : previousScheduled(today, scheduled);
        while (qualifying.contains(cursor)) {
            current++;
            cursor = previousScheduled(cursor, scheduled);
        }

        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate date : qualifying) {
            run = previous != null && nextScheduled(previous, scheduled).equals(date) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = date;
        }

        List<WeekProgress> weeks = weeks(today, weekStart -> {
            int done = (int) qualifying.subSet(weekStart, true, weekStart.plusDays(6), true).size();
            return new WeekProgress(weekStart, Math.min(done, weeklyTarget), weeklyTarget);
        });

        return new HabitProgress(unit, current, longest, met, todayRequired && !met, weeks.getLast(), weeks);
    }

    /**
     * N-times-per-week habits: each ISO week is one link, met when it has at
     * least N distinct check-in days. A single empty day is never a miss.
     */
    private static HabitProgress byWeeks(int timesPerWeek, NavigableSet<LocalDate> dates, LocalDate today) {
        Predicate<LocalDate> weekMet = weekStart ->
            dates.subSet(weekStart, true, weekStart.plusDays(6), true).size() >= timesPerWeek;

        LocalDate thisWeekStart = weekStartOf(today);
        boolean met = weekMet.test(thisWeekStart);

        int current = 0;
        LocalDate cursor = met ? thisWeekStart : thisWeekStart.minusWeeks(1);
        while (weekMet.test(cursor)) {
            current++;
            cursor = cursor.minusWeeks(1);
        }

        Set<LocalDate> metWeeks = new TreeSet<>();
        dates.forEach(date -> {
            LocalDate weekStart = weekStartOf(date);
            if (weekMet.test(weekStart)) {
                metWeeks.add(weekStart);
            }
        });
        int longest = 0;
        int run = 0;
        LocalDate previous = null;
        for (LocalDate weekStart : metWeeks) {
            run = previous != null && previous.plusWeeks(1).equals(weekStart) ? run + 1 : 1;
            longest = Math.max(longest, run);
            previous = weekStart;
        }

        List<WeekProgress> weeks = weeks(today, weekStart -> {
            int done = dates.subSet(weekStart, true, weekStart.plusDays(6), true).size();
            return new WeekProgress(weekStart, Math.min(done, timesPerWeek), timesPerWeek);
        });

        return new HabitProgress(StreakUnit.WEEKS, current, longest, met, !met, weeks.getLast(), weeks);
    }

    private static List<WeekProgress> weeks(LocalDate today, Function<LocalDate, WeekProgress> forWeek) {
        LocalDate thisWeekStart = weekStartOf(today);
        List<WeekProgress> weeks = new ArrayList<>(WEEKS_OF_HISTORY);
        for (int i = WEEKS_OF_HISTORY - 1; i >= 0; i--) {
            weeks.add(forWeek.apply(thisWeekStart.minusWeeks(i)));
        }
        return weeks;
    }

    private static LocalDate previousScheduled(LocalDate date, Predicate<LocalDate> scheduled) {
        LocalDate candidate = date.minusDays(1);
        while (!scheduled.test(candidate)) {
            candidate = candidate.minusDays(1);
        }
        return candidate;
    }

    private static LocalDate nextScheduled(LocalDate date, Predicate<LocalDate> scheduled) {
        LocalDate candidate = date.plusDays(1);
        while (!scheduled.test(candidate)) {
            candidate = candidate.plusDays(1);
        }
        return candidate;
    }
}
