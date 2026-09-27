package com.habittracker.checkins.api;

import com.habittracker.checkins.domain.CheckIn;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CheckInResponse(
    UUID id,
    UUID habitId,
    UUID userId,
    Instant recordedAt,
    LocalDate localDate
) {

    public static CheckInResponse from(CheckIn checkIn) {
        return new CheckInResponse(
            checkIn.id(),
            checkIn.habitId(),
            checkIn.userId(),
            checkIn.recordedAt(),
            checkIn.localDate()
        );
    }
}
