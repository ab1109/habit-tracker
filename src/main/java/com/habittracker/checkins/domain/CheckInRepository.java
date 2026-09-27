package com.habittracker.checkins.domain;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckInRepository {

    /**
     * Inserts the check-in unless one already exists for the same day (per the
     * database's unique indexes), and reports which happened. A conflict does
     * not fail the surrounding transaction, so the caller can go on to read
     * the existing row — with PostgreSQL, a raised constraint violation would
     * abort the transaction and make that read impossible.
     *
     * @return true if this call inserted the row
     */
    boolean insertIfAbsent(CheckIn checkIn);

    Optional<CheckIn> findExisting(UUID habitId, UUID userId, LocalDate localDate);

    /** The joint check-in that already covers this day for the circle, if any. */
    Optional<CheckIn> findExistingJoint(UUID habitId, UUID groupId, LocalDate localDate);

    /** A user's check-ins for a personal habit with {@code from <= localDate <= to}, oldest first. */
    List<CheckIn> findIndividualHistory(UUID habitId, UUID userId, LocalDate from, LocalDate to);

    /** A circle's check-ins for a joint habit with {@code from <= localDate <= to}, oldest first. */
    List<CheckIn> findJointHistory(UUID habitId, UUID groupId, LocalDate from, LocalDate to);

    /** Every local date a user checked in to a personal habit — the input to streak calculation. */
    List<LocalDate> findIndividualDates(UUID habitId, UUID userId);

    /** Every local date a joint habit was covered by its circle. */
    List<LocalDate> findJointDates(UUID habitId, UUID groupId);

    /** The most recently recorded check-ins across these habits, newest first. */
    List<CheckIn> findRecent(Collection<UUID> habitIds, int limit);
}
