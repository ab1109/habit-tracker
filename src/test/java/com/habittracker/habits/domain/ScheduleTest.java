package com.habittracker.habits.domain;

import org.junit.jupiter.api.Test;

import java.time.DayOfWeek;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ScheduleTest {

    @Test
    void dailyScheduleHasNoExtraData() {
        Schedule schedule = new DailySchedule();

        assertThat(schedule.type()).isEqualTo(ScheduleType.DAILY);
    }

    @Test
    void nTimesPerWeekAcceptsValuesOneToSeven() {
        Schedule schedule = new NTimesPerWeekSchedule(3);

        assertThat(schedule.type()).isEqualTo(ScheduleType.N_TIMES_PER_WEEK);
        assertThat(((NTimesPerWeekSchedule) schedule).timesPerWeek()).isEqualTo(3);
    }

    @Test
    void nTimesPerWeekRejectsZero() {
        assertThatThrownBy(() -> new NTimesPerWeekSchedule(0))
            .isInstanceOf(InvalidScheduleException.class)
            .hasMessageContaining("between 1 and 7");
    }

    @Test
    void nTimesPerWeekRejectsMoreThanSeven() {
        assertThatThrownBy(() -> new NTimesPerWeekSchedule(8))
            .isInstanceOf(InvalidScheduleException.class)
            .hasMessageContaining("between 1 and 7");
    }

    @Test
    void specificWeekdaysRejectsEmptySet() {
        assertThatThrownBy(() -> new SpecificWeekdaysSchedule(Set.of()))
            .isInstanceOf(InvalidScheduleException.class)
            .hasMessageContaining("must not be empty");
    }

    @Test
    void specificWeekdaysStoresADefensiveCopy() {
        Set<DayOfWeek> mutableInput = new java.util.HashSet<>(Set.of(DayOfWeek.MONDAY));
        SpecificWeekdaysSchedule schedule = new SpecificWeekdaysSchedule(mutableInput);

        mutableInput.add(DayOfWeek.FRIDAY);

        assertThat(schedule.weekdays()).containsExactly(DayOfWeek.MONDAY);
    }
}
