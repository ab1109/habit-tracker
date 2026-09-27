package com.habittracker.habits.api;

import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.application.HabitService;
import com.habittracker.habits.domain.Habit;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/habits")
public class HabitController {

    private final HabitService habitService;

    public HabitController(HabitService habitService) {
        this.habitService = habitService;
    }

    @PostMapping
    public ResponseEntity<HabitResponse> createHabit(
        @RequestHeader("X-User-Id") UUID userId,
        @Valid @RequestBody CreateHabitRequest request
    ) {
        Habit habit = habitService.createHabit(OwnerType.USER, userId, request.name(), request.toSchedule());
        return ResponseEntity.created(URI.create("/habits/" + habit.id())).body(HabitResponse.from(habit));
    }

    @GetMapping
    public List<HabitResponse> myHabits(@RequestHeader("X-User-Id") UUID userId) {
        return habitService.activeHabitsOf(OwnerType.USER, userId).stream().map(HabitResponse::from).toList();
    }

    @GetMapping("/{id}")
    public HabitResponse getHabit(@PathVariable UUID id) {
        return HabitResponse.from(habitService.getHabit(id));
    }

    @PatchMapping("/{id}/archive")
    public HabitResponse archiveHabit(@PathVariable UUID id) {
        return HabitResponse.from(habitService.archiveHabit(id));
    }
}
