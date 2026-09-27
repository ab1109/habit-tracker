package com.habittracker.checkins.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository {

    /**
     * Persists immediately (flushes) rather than deferring to the transaction's
     * end, so a unique-constraint violation surfaces here as a
     * {@link DuplicateCheckInException}, not later at commit time.
     */
    CheckIn save(CheckIn checkIn);

    Optional<CheckIn> findExisting(UUID habitId, UUID userId, LocalDate localDate);

    /** A user's check-ins for a personal habit with {@code from <= localDate <= to}, oldest first. */
    List<CheckIn> findIndividualHistory(UUID habitId, UUID userId, LocalDate from, LocalDate to);

    /** Every local date a user checked in to a personal habit — the input to streak calculation. */
    List<LocalDate> findIndividualDates(UUID habitId, UUID userId);
}
