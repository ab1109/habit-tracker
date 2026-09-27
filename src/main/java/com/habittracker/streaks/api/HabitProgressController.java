package com.habittracker.streaks.api;

import com.habittracker.common.api.CurrentUser;
import com.habittracker.checkins.domain.LocalDateResolver;
import com.habittracker.habits.application.HabitService;
import com.habittracker.streaks.application.HabitProgressService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@RestController
public class HabitProgressController {

    private final HabitProgressService progressService;
    private final HabitService habitService;
    private final Clock clock;

    public HabitProgressController(HabitProgressService progressService, HabitService habitService, Clock clock) {
        this.progressService = progressService;
        this.habitService = habitService;
        this.clock = clock;
    }

    @GetMapping("/habits/{habitId}/progress")
    public HabitProgressResponse progress(
        @PathVariable UUID habitId,
        @CurrentUser UUID userId,
        @RequestHeader("X-Timezone") String timezone
    ) {
        LocalDate today = LocalDateResolver.resolve(clock.instant(), ZoneId.of(timezone));
        var progress = progressService.progress(habitId, userId, today);
        return HabitProgressResponse.from(habitService.getHabit(habitId), progress);
    }
}
