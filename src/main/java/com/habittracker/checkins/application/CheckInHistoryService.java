package com.habittracker.checkins.application;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.common.domain.DomainValidationException;
import com.habittracker.habits.application.HabitAccessPolicy;
import com.habittracker.habits.domain.Habit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Read side of the check-ins module. Other modules (streaks, groups) read
 * check-in history only through this service, which decides whose
 * check-ins count for a given habit: the owner's for a personal habit, the
 * circle's for a joint one.
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
        return checkInsBetween(accessPolicy.requireViewable(habitId, viewerId), from, to);
    }

    // The methods below take an already-loaded Habit: callers are responsible
    // for having authorized access to it.

    @Transactional(readOnly = true)
    public List<CheckIn> checkInsBetween(Habit habit, LocalDate from, LocalDate to) {
        return switch (habit.ownerType()) {
            case USER -> checkInRepository.findIndividualHistory(habit.id(), habit.ownerId(), from, to);
            case GROUP -> checkInRepository.findJointHistory(habit.id(), habit.ownerId(), from, to);
        };
    }

    /** The local dates that count toward this habit's progress. */
    @Transactional(readOnly = true)
    public List<LocalDate> checkInDates(Habit habit) {
        return switch (habit.ownerType()) {
            case USER -> checkInRepository.findIndividualDates(habit.id(), habit.ownerId());
            case GROUP -> checkInRepository.findJointDates(habit.id(), habit.ownerId());
        };
    }

    /** Newest first. */
    @Transactional(readOnly = true)
    public List<CheckIn> recent(Collection<Habit> habits, int limit) {
        return checkInRepository.findRecent(habits.stream().map(Habit::id).toList(), limit);
    }
}
