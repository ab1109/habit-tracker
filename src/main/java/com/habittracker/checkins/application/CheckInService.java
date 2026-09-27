package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.Habit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final HabitAccessPolicy accessPolicy;

    public CheckInService(CheckInRepository checkInRepository, HabitAccessPolicy accessPolicy) {
        this.checkInRepository = checkInRepository;
        this.accessPolicy = accessPolicy;
    }

    /**
     * For a joint habit the local date is resolved in the performing member's
     * timezone, and the check-in covers that day for the whole circle.
     */
    @Transactional
    public CheckInResult recordCheckIn(UUID habitId, UUID userId, Instant recordedAt, ZoneId timezone) {
        Habit habit = accessPolicy.requireCheckInAllowed(habitId, userId);

        LocalDate localDate = LocalDateResolver.resolve(recordedAt, timezone);

        boolean joint = habit.ownerType() == OwnerType.GROUP;
        Supplier<Optional<CheckIn>> findExisting = joint
            ? () -> checkInRepository.findExistingJoint(habitId, habit.ownerId(), localDate)
            : () -> checkInRepository.findExisting(habitId, userId, localDate);

        // Layer 1: an application-level pre-check. Handles the common,
        // non-racing case cleanly, without ever touching the database's
        // constraint machinery.
        Optional<CheckIn> existing = findExisting.get();
        if (existing.isPresent()) {
            return new CheckInResult(existing.get(), true);
        }

        // Layer 2: the database's unique constraint is the final authority.
        // Two concurrent requests can both pass the check above before
        // either commits — only one insert can actually win; the loser's
        // insert is skipped and it returns the winner's row.
        CheckIn checkIn = joint
            ? CheckIn.createJoint(habitId, habit.ownerId(), userId, recordedAt, localDate)
            : CheckIn.create(habitId, userId, recordedAt, localDate);
        if (checkInRepository.insertIfAbsent(checkIn)) {
            return new CheckInResult(checkIn, false);
        }
        CheckIn winner = findExisting.get()
            .orElseThrow(() -> new IllegalStateException("Insert was skipped as a duplicate, but no check-in exists"));
        return new CheckInResult(winner, true);
    }
}
