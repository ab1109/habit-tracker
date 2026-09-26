package com.habittracker.habits.application;

import com.habittracker.common.domain.NotFoundException;
import com.habittracker.common.domain.OwnerType;
import com.habittracker.habits.domain.Habit;
import com.habittracker.habits.domain.HabitRepository;
import com.habittracker.habits.domain.Schedule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class HabitService {

    private final HabitRepository habitRepository;

    public HabitService(HabitRepository habitRepository) {
        this.habitRepository = habitRepository;
    }

    @Transactional
    public Habit createHabit(OwnerType ownerType, UUID ownerId, String name, Schedule schedule) {
        Habit habit = Habit.create(ownerType, ownerId, name, schedule);
        return habitRepository.save(habit);
    }

    @Transactional(readOnly = true)
    public Habit getHabit(UUID id) {
        return habitRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Habit not found: " + id));
    }

    @Transactional
    public Habit archiveHabit(UUID id) {
        Habit habit = getHabit(id);
        return habitRepository.save(habit.archive());
    }
}
