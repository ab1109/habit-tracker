package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.checkins.domain.DuplicateCheckInException;
import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.common.domain.ForbiddenException;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    @Mock
    private CheckInRepository checkInRepository;

    @Mock
    private HabitRepository habitRepository;

    private CheckInService service() {
        return new CheckInService(checkInRepository, new HabitAccessPolicy(habitRepository));
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

        when(checkInRepository.save(any(CheckIn.class)))
            .thenThrow(new DuplicateCheckInException("already exists", new RuntimeException("constraint violation")));

        CheckInService service = service();
        CheckInResult result = service.recordCheckIn(habitId, userId, now, utc);

        assertThat(result.alreadyExisted()).isTrue();
        assertThat(result.checkIn()).isEqualTo(winner);
    }
}
