package com.habittracker.checkins.api;

import com.habittracker.checkins.application.CheckInResult;
import com.habittracker.checkins.application.CheckInService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/habits/{habitId}/checkins")
public class CheckInController {

    private final CheckInService checkInService;

    public CheckInController(CheckInService checkInService) {
        this.checkInService = checkInService;
    }

    @PostMapping
    public ResponseEntity<CheckInResponse> recordCheckIn(
        @PathVariable UUID habitId,
        @RequestHeader("X-User-Id") UUID userId,
        @RequestHeader("X-Timezone") String timezone
    ) {
        CheckInResult result = checkInService.recordCheckIn(habitId, userId, Instant.now(), ZoneId.of(timezone));
        HttpStatus status = result.alreadyExisted() ? HttpStatus.OK : HttpStatus.CREATED;
        return ResponseEntity.status(status).body(CheckInResponse.from(result.checkIn()));
    }
}
