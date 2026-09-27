package com.habittracker.checkins.domain;

import java.time.LocalDate;
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
}
