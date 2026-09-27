package com.habittracker.checkins.api;

import com.habittracker.checkins.application.CheckInHistoryService;
import com.habittracker.checkins.application.CheckInResult;
import com.habittracker.checkins.application.CheckInService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/habits/{habitId}/checkins")
public class CheckInController {

    private final CheckInService checkInService;
    private final CheckInHistoryService historyService;
    private final Clock clock;

    public CheckInController(CheckInService checkInService, CheckInHistoryService historyService, Clock clock) {
        this.checkInService = checkInService;
        this.historyService = historyService;
        this.clock = clock;
    }

    @PostMapping
    public ResponseEntity<CheckInResponse> recordCheckIn(
        @PathVariable UUID habitId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Timezone") String timezone
    ) {
        CheckInResult result = checkInService.recordCheckIn(habitId, userId, clock.instant(), ZoneId.of(timezone));
        HttpStatus status = result.alreadyExisted() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(CheckInResponse.from(result.checkIn()));
    }

    /** Idempotent: 204 whether or not there was a check-in to remove. */
    @DeleteMapping("/today")
    public ResponseEntity<Void> undoTodaysCheckIn(
        @PathVariable UUID habitId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Timezone") String timezone
    ) {
        checkInService.undoTodaysCheckIn(habitId, userId, clock.instant(), ZoneId.of(timezone));
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<CheckInResponse> history(
        @PathVariable UUID habitId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return historyService.history(habitId, userId, from, to).stream().map(CheckInResponse::from).toList();
    }
}
