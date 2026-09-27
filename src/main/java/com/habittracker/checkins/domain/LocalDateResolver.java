package com.habittracker.checkins.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Resolves which calendar day a check-in counts for. This must be called
 * exactly once, at the moment the check-in is recorded, using the checking-in
 * user's timezone — the result is persisted, never recomputed from the UTC
 * timestamp later (the same instant is a different local date in a different
 * timezone, so recomputing later without the original timezone is lossy).
 */
public final class LocalDateResolver {

    private LocalDateResolver() {
    }

    public static LocalDate resolve(Instant recordedAt, ZoneId timezone) {
        return recordedAt.atZone(timezone).toLocalDate();
    }
}
