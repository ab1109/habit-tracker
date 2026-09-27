package com.habittracker.checkins.api;

import com.habittracker.checkins.domain.CheckIn;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record CheckInResponse(
    UUID id,
    UUID habitId,
    UUID groupId,
    UUID userId,
    UUID performedByUserId,
    Instant recordedAt,
    LocalDate localDate
) {

    public static CheckInResponse from(CheckIn checkIn) {
        return new CheckInResponse(
            checkIn.id(),
            checkIn.habitId(),
            checkIn.groupId(),
            checkIn.userId(),
            checkIn.performedByUserId(),
            checkIn.recordedAt(),
            checkIn.localDate()
        );
    }
}
