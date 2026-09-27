package com.habittracker.checkins.infrastructure;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.checkins.domain.DuplicateCheckInException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
class CheckInRepositoryAdapter implements CheckInRepository {

    private final CheckInJpaSpringDataRepository springDataRepository;
    private final CheckInMapper mapper;

    CheckInRepositoryAdapter(CheckInJpaSpringDataRepository springDataRepository, CheckInMapper mapper) {
        this.springDataRepository = springDataRepository;
        this.mapper = mapper;
    }

    @Override
    public CheckIn save(CheckIn checkIn) {
        try {
            CheckInJpaEntity saved = springDataRepository.saveAndFlush(mapper.toEntity(checkIn));
            return mapper.toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            throw new DuplicateCheckInException(
                "Check-in already exists for habit " + checkIn.habitId()
                    + ", user " + checkIn.userId() + ", date " + checkIn.localDate(),
                e
            );
        }
    }

    @Override
    public Optional<CheckIn> findExisting(UUID habitId, UUID userId, LocalDate localDate) {
        return springDataRepository.findByHabitIdAndUserIdAndLocalDateAndGroupIdIsNull(habitId, userId, localDate)
            .map(mapper::toDomain);
    }

    @Override
    public List<CheckIn> findIndividualHistory(UUID habitId, UUID userId, LocalDate from, LocalDate to) {
        return springDataRepository
            .findByHabitIdAndUserIdAndGroupIdIsNullAndLocalDateBetweenOrderByLocalDate(habitId, userId, from, to)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LocalDate> findIndividualDates(UUID habitId, UUID userId) {
        return springDataRepository.findIndividualDates(habitId, userId);
    }
}
