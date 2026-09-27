package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.habits.domain.Habit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Read side of the check-ins module. Other modules (streaks, groups) read
 * check-in history only through this service, which decides whose
 * check-ins count for a given habit.
 */
@Service
public class CheckInHistoryService {

    static final long MAX_RANGE_DAYS = 400;

    private final CheckInRepository checkInRepository;
    private final HabitAccessPolicy accessPolicy;

    public CheckInHistoryService(CheckInRepository checkInRepository, HabitAccessPolicy accessPolicy) {
        this.checkInRepository = checkInRepository;
        this.accessPolicy = accessPolicy;
    }

    @Transactional(readOnly = true)
    public List<CheckIn> history(UUID habitId, UUID viewerId, LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw new DomainValidationException("from must not be after to");
        }
        if (ChronoUnit.DAYS.between(from, to) > MAX_RANGE_DAYS) {
            throw new DomainValidationException("date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
        Habit habit = accessPolicy.requireViewable(habitId, viewerId);
        return checkInRepository.findIndividualHistory(habit.id(), habit.ownerId(), from, to);
    }

    /**
     * The local dates that count toward this habit's progress. Callers are
     * responsible for having authorized access to the habit.
     */
    @Transactional(readOnly = true)
    public List<LocalDate> checkInDates(Habit habit) {
        return checkInRepository.findIndividualDates(habit.id(), habit.ownerId());
    }
}
