package com.habittracker.habits.domain;

import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.common.domain.OwnerType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HabitTest {

    @Test
    void createGeneratesIdAndIsNotArchived() {
        Habit habit = Habit.create(OwnerType.USER, UUID.randomUUID(), "Meditate", new DailySchedule());

        assertThat(habit.id()).isNotNull();
        assertThat(habit.createdAt()).isNotNull();
        assertThat(habit.isArchived()).isFalse();
    }

    @Test
    void blankNameIsRejected() {
        assertThatThrownBy(() ->
            Habit.create(OwnerType.USER, UUID.randomUUID(), "  ", new DailySchedule())
        ).isInstanceOf(DomainValidationException.class);
    }

    @Test
    void archiveReturnsANewInstanceLeavingTheOriginalUntouched() {
        Habit original = Habit.create(OwnerType.USER, UUID.randomUUID(), "Meditate", new DailySchedule());

        Habit archived = original.archive();

        assertThat(original.isArchived()).isFalse();
        assertThat(archived.isArchived()).isTrue();
        assertThat(archived.id()).isEqualTo(original.id());
    }

    @Test
    void archivingAnAlreadyArchivedHabitIsANoOp() {
        Habit archived = Habit.create(OwnerType.USER, UUID.randomUUID(), "Meditate", new DailySchedule()).archive();

        Habit archivedAgain = archived.archive();

        assertThat(archivedAgain).isSameAs(archived);
    }
}
