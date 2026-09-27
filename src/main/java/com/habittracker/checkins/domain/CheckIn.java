package com.habittracker.checkins.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param groupId           null for a personal habit; the owning circle for a
 *                          joint habit, where one check-in covers the day for
 *                          every member
 * @param userId            whose check-in this is (for a joint habit, the
 *                          member who performed it)
 * @param performedByUserId who actually tapped check-in
 */
public record CheckIn(
    UUID id,
    UUID habitId,
    UUID groupId,
    UUID userId,
    UUID performedByUserId,
    Instant recordedAt,
    LocalDate localDate
) {

    public static CheckIn create(UUID habitId, UUID userId, Instant recordedAt, LocalDate localDate) {
        return new CheckIn(UUID.randomUUID(), habitId, null, userId, userId, recordedAt, localDate);
    }

    public static CheckIn createJoint(UUID habitId, UUID groupId, UUID performedBy,
                                      Instant recordedAt, LocalDate localDate) {
        return new CheckIn(UUID.randomUUID(), habitId, groupId, performedBy, performedBy, recordedAt, localDate);
    }

    public boolean isJoint() {
        return groupId != null;
    }
}
