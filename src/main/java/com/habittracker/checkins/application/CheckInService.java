package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.checkins.domain.DuplicateCheckInException;
import com.habittracker.checkins.domain.LocalDateResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final HabitAccessPolicy accessPolicy;

    public CheckInService(CheckInRepository checkInRepository, HabitAccessPolicy accessPolicy) {
        this.checkInRepository = checkInRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional
    public CheckInResult recordCheckIn(UUID habitId, UUID userId, Instant recordedAt, ZoneId timezone) {
        accessPolicy.requireCheckInAllowed(habitId, userId);

        LocalDate localDate = LocalDateResolver.resolve(recordedAt, timezone);

        // Layer 1: an application-level pre-check. Handles the common,
        // non-racing case cleanly, without ever touching the database's
        // constraint machinery.
        Optional<CheckIn> existing = checkInRepository.findExisting(habitId, userId, localDate);
        if (existing.isPresent()) {
            return new CheckInResult(existing.get(), true);
        }

        // Layer 2: the database's unique constraint is the final authority.
        // Two concurrent requests can both pass the check above before
        // either commits — only one insert can actually win.
        try {
            CheckIn saved = checkInRepository.save(CheckIn.create(habitId, userId, recordedAt, localDate));
            return new CheckInResult(saved, false);
        } catch (DuplicateCheckInException e) {
            CheckIn winner = checkInRepository.findExisting(habitId, userId, localDate)
                .orElseThrow(() -> new IllegalStateException(
                    "Expected an existing check-in after a duplicate was detected", e));
            return new CheckInResult(winner, true);
        }
    }
}
