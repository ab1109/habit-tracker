package com.habittracker.notifications.application;

import com.habittracker.common.domain.OwnerType;
import com.habittracker.groups.application.GroupProgress;
import com.habittracker.groups.domain.Group;
import com.habittracker.habits.domain.DailySchedule;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.NTimesPerWeekSchedule;
import com.habittracker.notifications.domain.WeeklyDigest;
import com.habittracker.streaks.domain.HabitProgress;
import com.habittracker.streaks.domain.ProgressCalculator;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WeeklyDigestComposerTest {

    private static final LocalDate SUNDAY = LocalDate.of(2026, 9, 20);
    private static final LocalDate MONDAY = LocalDate.of(2026, 9, 14);

    private final UUID maya = UUID.randomUUID();
    private final UUID dev = UUID.randomUUID();
    private final Group flat = Group.create("Flat 4B", maya, "Maya").addMember(maya, dev, "Dev");

    @Test
    void summarisesWhoHeldWhoDroppedOffAndWhatWentUncovered() {
        Habit mayasRun = Habit.create(OwnerType.USER, maya, "Run", new NTimesPerWeekSchedule(3));
        Habit devsRead = Habit.create(OwnerType.USER, dev, "Read", new DailySchedule());
        Habit kitchen = Habit.create(OwnerType.GROUP, flat.id(), "Kitchen reset", new DailySchedule());

        HabitProgress runMet = progress(mayasRun, List.of(MONDAY, MONDAY.plusDays(2), MONDAY.plusDays(4)));
        HabitProgress readMissed = progress(devsRead, List.of(MONDAY));
        HabitProgress kitchenFiveOfSeven = progress(kitchen,
            List.of(MONDAY, MONDAY.plusDays(1), MONDAY.plusDays(2), MONDAY.plusDays(3), MONDAY.plusDays(4)));

        GroupProgress progress = new GroupProgress(flat, MONDAY, 0, 0,
            List.of(new GroupProgress.SharedHabitProgress(mayasRun, maya, "Maya", runMet),
                new GroupProgress.SharedHabitProgress(devsRead, dev, "Dev", readMissed)),
            List.of(new GroupProgress.JointHabitProgress(kitchen, kitchenFiveOfSeven, List.of())),
            List.of());

        WeeklyDigest digest = WeeklyDigestComposer.compose(progress);

        assertThat(digest.onTarget()).containsExactly(new WeeklyDigest.OnTarget("Maya", "Run"));
        assertThat(digest.behind()).containsExactly(new WeeklyDigest.Behind("Dev", "Read", 1, 7));
        assertThat(digest.uncoveredJointHabits()).containsExactly(new WeeklyDigest.Uncovered("Kitchen reset", 2));
        assertThat(digest.subject()).isEqualTo("Flat 4B — your week (14 Sep–20 Sep)");
        assertThat(digest.body())
            .contains("Held their streak:\n  • Maya — Run")
            .contains("Dropped off:\n  • Dev — Read (1 of 7)")
            .contains("Went uncovered:\n  • Kitchen reset — 2 days");
    }

    @Test
    void aCircleWithNothingSharedSaysSo() {
        WeeklyDigest digest = WeeklyDigestComposer.compose(
            new GroupProgress(flat, MONDAY, 0, 0, List.of(), List.of(), List.of()));

        assertThat(digest.body()).isEqualTo("Nothing is shared with Flat 4B yet.");
    }

    private static HabitProgress progress(Habit habit, List<LocalDate> dates) {
        HabitProgress p = ProgressCalculator.calculate(habit.schedule(), dates, SUNDAY);
        assertThat(p.thisWeek().weekStart()).isEqualTo(MONDAY);
        return p;
    }
}
