package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.common.domain.ForbiddenException;
import com.habittracker.habits.application.CircleMembership;
import com.habittracker.habits.application.HabitAccessPolicy;
import com.habittracker.common.domain.NotFoundException;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.DailySchedule;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.HabitRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock
    private CheckInRepository checkInRepository;

    @Mock
    private HabitRepository habitRepository;

    @Mock
    private CircleMembership circleMembership;

    private CheckInService service() {
        return new CheckInService(checkInRepository, new HabitAccessPolicy(habitRepository, circleMembership));
    }

    @Test
    void recordCheckInThrowsNotFoundWhenHabitDoesNotExist() {
        UUID habitId = UUID.randomUUID();
        when(habitRepository.findById(habitId)).thenReturn(Optional.empty());

        CheckInService service = service();

        assertThatThrownBy(() ->
            service.recordCheckIn(habitId, UUID.randomUUID(), Instant.now(), ZoneId.of("UTC"))
        ).isInstanceOf(NotFoundException.class);
    }

    @Test
    void onlyTheOwnerCanCheckInToAPersonalHabit() {
        Habit habit = Habit.create(OwnerType.USER, UUID.randomUUID(), "Meditate", new DailySchedule());
        when(habitRepository.findById(habit.id())).thenReturn(Optional.of(habit));

        assertThatThrownBy(() ->
            service().recordCheckIn(habit.id(), UUID.randomUUID(), Instant.now(), ZoneId.of("UTC"))
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aJointCheckInThatAnotherMemberAlreadyMadeReturnsTheirs() {
        UUID groupId = UUID.randomUUID();
        UUID maya = UUID.randomUUID();
        UUID dev = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-20T15:00:00Z");
        LocalDate today = LocalDate.of(2026, 9, 20);
        Habit joint = Habit.create(OwnerType.GROUP, groupId, "Kitchen reset", new DailySchedule());
        CheckIn mayas = CheckIn.createJoint(joint.id(), groupId, maya, now, today);

        when(habitRepository.findById(joint.id())).thenReturn(Optional.of(joint));
        when(circleMembership.isMember(groupId, dev)).thenReturn(true);
        when(checkInRepository.findExistingJoint(joint.id(), groupId, today)).thenReturn(Optional.of(mayas));

        CheckInResult result = service().recordCheckIn(joint.id(), dev, now, ZoneId.of("UTC"));

        assertThat(result.alreadyExisted()).isTrue();
        assertThat(result.checkIn().performedByUserId()).isEqualTo(maya);
    }

    @Test
    void undoingRemovesTodaysCheckIn() {
        UUID userId = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-20T15:00:00Z");
        Habit habit = Habit.create(OwnerType.USER, userId, "Meditate", new DailySchedule());
        CheckIn mine = CheckIn.create(habit.id(), userId, now, LocalDate.of(2026, 9, 20));
        when(habitRepository.findById(habit.id())).thenReturn(Optional.of(habit));
        when(checkInRepository.findExisting(habit.id(), userId, LocalDate.of(2026, 9, 20))).thenReturn(Optional.of(mine));

        assertThat(service().undoTodaysCheckIn(habit.id(), userId, now, ZoneId.of("UTC"))).isTrue();
        verify(checkInRepository).delete(mine.id());
    }

    @Test
    void undoingWhenNothingWasCheckedInIsANoOp() {
        UUID userId = UUID.randomUUID();
        Habit habit = Habit.create(OwnerType.USER, userId, "Meditate", new DailySchedule());
        when(habitRepository.findById(habit.id())).thenReturn(Optional.of(habit));
        when(checkInRepository.findExisting(any(), any(), any())).thenReturn(Optional.empty());

        assertThat(service().undoTodaysCheckIn(habit.id(), userId, Instant.now(), ZoneId.of("UTC"))).isFalse();
        verify(checkInRepository, never()).delete(any());
    }

    @Test
    void aMemberCannotUndoAnotherMembersJointCheckIn() {
        UUID groupId = UUID.randomUUID();
        UUID maya = UUID.randomUUID();
        UUID dev = UUID.randomUUID();
        Instant now = Instant.parse("2026-09-20T15:00:00Z");
        LocalDate today = LocalDate.of(2026, 9, 20);
        Habit joint = Habit.create(OwnerType.GROUP, groupId, "Kitchen reset", new DailySchedule());
        when(habitRepository.findById(joint.id())).thenReturn(Optional.of(joint));
        when(circleMembership.isMember(groupId, dev)).thenReturn(true);
        when(checkInRepository.findExistingJoint(joint.id(), groupId, today))
            .thenReturn(Optional.of(CheckIn.createJoint(joint.id(), groupId, maya, now, today)));

        assertThatThrownBy(() -> service().undoTodaysCheckIn(joint.id(), dev, now, ZoneId.of("UTC")))
            .isInstanceOf(ForbiddenException.class);
        verify(checkInRepository, never()).delete(any());
    }

    @Test
    void aNonMemberCannotCheckInToAJointHabit() {
        UUID groupId = UUID.randomUUID();
        Habit joint = Habit.create(OwnerType.GROUP, groupId, "Kitchen reset", new DailySchedule());
        when(habitRepository.findById(joint.id())).thenReturn(Optional.of(joint));

        assertThatThrownBy(() ->
            service().recordCheckIn(joint.id(), UUID.randomUUID(), Instant.now(), ZoneId.of("UTC"))
        ).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void aRacingDuplicateInsertStillReturnsTheWinningCheckIn() {
        UUID habitId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Instant now = Instant.now();
        ZoneId utc = ZoneId.of("UTC");
        LocalDate today = LocalDateResolver.resolve(now, utc);

        Habit habit = Habit.create(OwnerType.USER, userId, "Meditate", new DailySchedule());
        when(habitRepository.findById(habitId)).thenReturn(Optional.of(habit));

        // The check-in that "wins" the race by committing first.
        CheckIn winner = CheckIn.create(habitId, userId, now, today);

        when(checkInRepository.findExisting(habitId, userId, today))
            .thenReturn(Optional.empty())      // 1st call: the pre-check — nothing yet
            .thenReturn(Optional.of(winner));  // 2nd call: after losing the race

        // The insert is skipped: the other request's row already holds the day.
        when(checkInRepository.insertIfAbsent(any(CheckIn.class))).thenReturn(false);

        CheckInService service = service();
        CheckInResult result = service.recordCheckIn(habitId, userId, now, utc);

        assertThat(result.alreadyExisted()).isTrue();
        assertThat(result.checkIn()).isEqualTo(winner);
    }
}
