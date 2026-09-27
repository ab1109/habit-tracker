package com.habittracker.streaks.application;

import com.habittracker.checkins.application.CheckInHistoryService;
import com.habittracker.checkins.application.HabitAccessPolicy;
import com.habittracker.habits.domain.Habit;
import com.habittracker.streaks.domain.HabitProgress;
import com.habittracker.streaks.domain.ProgressCalculator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class HabitProgressService {

    private final HabitAccessPolicy accessPolicy;
    private final CheckInHistoryService historyService;

    public HabitProgressService(HabitAccessPolicy accessPolicy, CheckInHistoryService historyService) {
        this.accessPolicy = accessPolicy;
        this.historyService = historyService;
    }

    /**
     * @param today the viewer's current local date — progress is always
     *              relative to whoever is looking
     */
    @Transactional(readOnly = true)
    public HabitProgress progress(UUID habitId, UUID viewerId, LocalDate today) {
        return progress(accessPolicy.requireViewable(habitId, viewerId), today);
    }

    /** For callers that have already authorized access to the habit (e.g. group progress). */
    @Transactional(readOnly = true)
    public HabitProgress progress(Habit habit, LocalDate today) {
        return ProgressCalculator.calculate(habit.schedule(), historyService.checkInDates(habit), today);
    }
}
