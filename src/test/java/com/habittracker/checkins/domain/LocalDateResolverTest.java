package com.habittracker.checkins.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDateResolverTest {

    /**
     * The exact scenario from ARCHITECTURE.md: the same instant belongs to a
     * different local date depending on the checking-in user's timezone.
     * 2026-09-07T02:00:00Z is 2026-09-06 evening in Los Angeles (UTC-7 in
     * September, daylight time) but already 2026-09-07 morning in Kolkata
     * (UTC+5:30) — proving why the timezone must be applied once, at
     * check-in time, using the real user's zone, not guessed later.
     */
    @Test
    void sameInstantResolvesToDifferentLocalDatesInDifferentTimezones() {
        Instant recordedAt = Instant.parse("2026-09-07T02:00:00Z");

        LocalDate losAngelesDate = LocalDateResolver.resolve(recordedAt, ZoneId.of("America/Los_Angeles"));
        LocalDate kolkataDate = LocalDateResolver.resolve(recordedAt, ZoneId.of("Asia/Kolkata"));

        assertThat(losAngelesDate).isEqualTo(LocalDate.of(2026, 9, 6));
        assertThat(kolkataDate).isEqualTo(LocalDate.of(2026, 9, 7));
    }

    @Test
    void midnightLocalTimeResolvesToTheNewDay() {
        // A user in Los Angeles checking in at 00:30 local time — the
        // exact "2026-09-07 00:30 local" example from ARCHITECTURE.md.
        ZoneId losAngeles = ZoneId.of("America/Los_Angeles");
        Instant justAfterMidnightLocal = LocalDate.of(2026, 9, 7)
            .atStartOfDay(losAngeles)
            .plusMinutes(30)
            .toInstant();

        LocalDate resolved = LocalDateResolver.resolve(justAfterMidnightLocal, losAngeles);

        assertThat(resolved).isEqualTo(LocalDate.of(2026, 9, 7));
    }
}
