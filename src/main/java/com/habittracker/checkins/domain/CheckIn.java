package com.habittracker.checkins.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CheckIn(
    UUID id,
    UUID habitId,
    UUID userId,
    UUID performedByUserId,
    Instant recordedAt,
    LocalDate localDate
) {

    public static CheckIn create(UUID habitId, UUID userId, Instant recordedAt, LocalDate localDate) {
        return new CheckIn(UUID.randomUUID(), habitId, userId, userId, recordedAt, localDate);
    }
}
