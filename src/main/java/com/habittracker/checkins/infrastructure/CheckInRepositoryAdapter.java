package com.habittracker.checkins.infrastructure;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import com.habittracker.checkins.domain.DuplicateCheckInException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
