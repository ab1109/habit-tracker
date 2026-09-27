package com.habittracker.streaks.domain;

import com.habittracker.habits.domain.DailySchedule;
import com.habittracker.habits.domain.NTimesPerWeekSchedule;
import com.habittracker.habits.domain.SpecificWeekdaysSchedule;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static java.time.DayOfWeek.FRIDAY;
import static java.time.DayOfWeek.MONDAY;
import static java.time.DayOfWeek.WEDNESDAY;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Calendar used throughout: September 2026. Monday the 14th to Sunday the
 * 20th is one ISO week; the 7th and 21st are the Mondays either side.
 */
class ProgressCalculatorTest {

    private static LocalDate sep(int day) {
        return LocalDate.of(2026, 9, day);
    }

    @Nested
    class Daily {

        private final DailySchedule daily = new DailySchedule();

        @Test
        void emptyHistoryHasNoStreakAndTodayIsOpen() {
            HabitProgress p = ProgressCalculator.calculate(daily, List.of(), sep(16));

            assertThat(p.streakUnit()).isEqualTo(StreakUnit.DAYS);
            assertThat(p.currentStreak()).isZero();
            assertThat(p.longestStreak()).isZero();
            assertThat(p.currentPeriodOpen()).isTrue();
            assertThat(p.thisWeek()).isEqualTo(new WeekProgress(sep(14), 0, 7));
        }

        @Test
        void consecutiveDaysIncludingTodayCount() {
            HabitProgress p = ProgressCalculator.calculate(daily, List.of(sep(14), sep(15), sep(16)), sep(16));

            assertThat(p.currentStreak()).isEqualTo(3);
            assertThat(p.currentPeriodMet()).isTrue();
            assertThat(p.currentPeriodOpen()).isFalse();
        }

        @Test
        void todayNotYetCheckedInIsOpenAndDoesNotBreakTheStreak() {
            HabitProgress p = ProgressCalculator.calculate(daily, List.of(sep(14), sep(15)), sep(16));

            assertThat(p.currentStreak()).isEqualTo(2);
            assertThat(p.currentPeriodOpen()).isTrue();
        }

        @Test
        void aMissingDayEndsTheStreak() {
            // 13th and 14th done, 15th missed, today (16th) not yet.
            HabitProgress p = ProgressCalculator.calculate(daily, List.of(sep(13), sep(14)), sep(16));

            assertThat(p.currentStreak()).isZero();
            assertThat(p.longestStreak()).isEqualTo(2);
        }

        @Test
        void longestStreakSpansTheLongestRunInHistory() {
            List<LocalDate> dates = List.of(sep(1), sep(2), sep(3), sep(4), sep(10), sep(15), sep(16));

            HabitProgress p = ProgressCalculator.calculate(daily, dates, sep(16));

            assertThat(p.currentStreak()).isEqualTo(2);
            assertThat(p.longestStreak()).isEqualTo(4);
        }

        @Test
        void duplicateDatesCountOnce() {
            HabitProgress p = ProgressCalculator.calculate(daily, List.of(sep(16), sep(16)), sep(16));

            assertThat(p.currentStreak()).isEqualTo(1);
            assertThat(p.thisWeek().done()).isEqualTo(1);
        }

        @Test
        void checkInsAfterTodayAreIgnored() {
            // A joint-habit member in a later timezone may already be on the 17th.
            HabitProgress p = ProgressCalculator.calculate(daily, List.of(sep(15), sep(17)), sep(16));

            assertThat(p.currentStreak()).isEqualTo(1);
            assertThat(p.currentPeriodMet()).isFalse();
            assertThat(p.thisWeek().done()).isEqualTo(1);
        }

        @Test
        void streakCrossesMonthAndWeekBoundaries() {
            List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 30), LocalDate.of(2026, 8, 31), sep(1), sep(2));

            HabitProgress p = ProgressCalculator.calculate(daily, dates, sep(2));

            assertThat(p.currentStreak()).isEqualTo(4);
        }
    }

    @Nested
    class NTimesPerWeek {

        private final NTimesPerWeekSchedule threeAWeek = new NTimesPerWeekSchedule(3);

        @Test
        void aWeekMeetsTheTargetOnNonConsecutiveDays() {
            // Last week: Mon 7th, Thu 10th, Sun 13th — three scattered days.
            HabitProgress p = ProgressCalculator.calculate(threeAWeek, List.of(sep(7), sep(10), sep(13)), sep(16));

            assertThat(p.streakUnit()).isEqualTo(StreakUnit.WEEKS);
            assertThat(p.currentStreak()).isEqualTo(1);
            assertThat(p.weeks().get(p.weeks().size() - 2)).isEqualTo(new WeekProgress(sep(7), 3, 3));
        }

        @Test
        void emptyDaysInsideTheCurrentWeekAreNotAMiss() {
            // Last week met; this week only one check-in so far on Wednesday.
            List<LocalDate> dates = List.of(sep(7), sep(8), sep(9), sep(14));

            HabitProgress p = ProgressCalculator.calculate(threeAWeek, dates, sep(16));

            assertThat(p.currentStreak()).isEqualTo(1);
            assertThat(p.currentPeriodOpen()).isTrue();
            assertThat(p.thisWeek()).isEqualTo(new WeekProgress(sep(14), 1, 3));
        }

        @Test
        void currentWeekCountsOnceItsTargetIsMet() {
            List<LocalDate> dates = List.of(sep(7), sep(8), sep(9), sep(14), sep(15), sep(16));

            HabitProgress p = ProgressCalculator.calculate(threeAWeek, dates, sep(16));

            assertThat(p.currentStreak()).isEqualTo(2);
            assertThat(p.currentPeriodMet()).isTrue();
        }

        @Test
        void aWeekBelowTargetBreaksTheStreak() {
            // Aug 31 week met, Sep 7 week only twice, this week open.
            List<LocalDate> dates = List.of(
                LocalDate.of(2026, 8, 31), sep(1), sep(2), sep(7), sep(8));

            HabitProgress p = ProgressCalculator.calculate(threeAWeek, dates, sep(16));

            assertThat(p.currentStreak()).isZero();
            assertThat(p.longestStreak()).isEqualTo(1);
        }

        @Test
        void extraCheckInsAreCappedAtTheTarget() {
            List<LocalDate> dates = List.of(sep(14), sep(15), sep(16), sep(17));

            HabitProgress p = ProgressCalculator.calculate(threeAWeek, dates, sep(17));

            assertThat(p.thisWeek()).isEqualTo(new WeekProgress(sep(14), 3, 3));
        }
    }

    @Nested
    class SpecificWeekdays {

        private final SpecificWeekdaysSchedule monWedFri = new SpecificWeekdaysSchedule(Set.of(MONDAY, WEDNESDAY, FRIDAY));

        @Test
        void nonRequiredDaysDoNotBreakTheStreak() {
            // Fri 11th, Mon 14th, Wed 16th — the weekend and Tuesday are skipped.
            HabitProgress p = ProgressCalculator.calculate(monWedFri, List.of(sep(11), sep(14), sep(16)), sep(17));

            assertThat(p.streakUnit()).isEqualTo(StreakUnit.SCHEDULED_DAYS);
            assertThat(p.currentStreak()).isEqualTo(3);
            // Thursday isn't required: not met, but not open either.
            assertThat(p.currentPeriodMet()).isFalse();
            assertThat(p.currentPeriodOpen()).isFalse();
        }

        @Test
        void checkInsOnNonRequiredDaysDoNotCount() {
            HabitProgress p = ProgressCalculator.calculate(monWedFri, List.of(sep(15), sep(17)), sep(17));

            assertThat(p.currentStreak()).isZero();
            assertThat(p.thisWeek()).isEqualTo(new WeekProgress(sep(14), 0, 3));
        }

        @Test
        void missingARequiredDayEndsTheStreak() {
            // Mon 14th done, Wed 16th missed, today Fri 18th not yet.
            HabitProgress p = ProgressCalculator.calculate(monWedFri, List.of(sep(11), sep(14)), sep(18));

            assertThat(p.currentStreak()).isZero();
            assertThat(p.longestStreak()).isEqualTo(2);
            assertThat(p.currentPeriodOpen()).isTrue();
        }

        @Test
        void requiredTodayNotYetDoneIsOpen() {
            HabitProgress p = ProgressCalculator.calculate(monWedFri, List.of(sep(11), sep(14)), sep(16));

            assertThat(p.currentStreak()).isEqualTo(2);
            assertThat(p.currentPeriodOpen()).isTrue();
        }
    }

    @Test
    void weeksCoverTheLastTwelveIsoWeeksEndingWithTheCurrentOne() {
        HabitProgress p = ProgressCalculator.calculate(new DailySchedule(), List.of(), sep(20));

        assertThat(p.weeks()).hasSize(12);
        assertThat(p.weeks().getFirst().weekStart()).isEqualTo(LocalDate.of(2026, 6, 29));
        assertThat(p.weeks().getLast()).isEqualTo(p.thisWeek());
        assertThat(p.thisWeek().weekStart()).isEqualTo(sep(14));
    }
}
