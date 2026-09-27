package com.habittracker.checkins.infrastructure;

import com.habittracker.checkins.domain.CheckIn;
import com.habittracker.checkins.domain.CheckInRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Collection;
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
    public boolean insertIfAbsent(CheckIn checkIn) {
        return springDataRepository.insertIfAbsent(checkIn.id(), checkIn.habitId(), checkIn.groupId(),
            checkIn.userId(), checkIn.performedByUserId(), checkIn.recordedAt(), checkIn.localDate()) == 1;
    }

    @Override
    public void delete(UUID checkInId) {
        springDataRepository.deleteById(checkInId);
    }

    @Override
    public Optional<CheckIn> findExisting(UUID habitId, UUID userId, LocalDate localDate) {
        return springDataRepository.findByHabitIdAndUserIdAndLocalDateAndGroupIdIsNull(habitId, userId, localDate)
            .map(mapper::toDomain);
    }

    @Override
    public Optional<CheckIn> findExistingJoint(UUID habitId, UUID groupId, LocalDate localDate) {
        return springDataRepository.findByHabitIdAndGroupIdAndLocalDate(habitId, groupId, localDate)
            .map(mapper::toDomain);
    }

    @Override
    public List<CheckIn> findIndividualHistory(UUID habitId, UUID userId, LocalDate from, LocalDate to) {
        return springDataRepository
            .findByHabitIdAndUserIdAndGroupIdIsNullAndLocalDateBetweenOrderByLocalDate(habitId, userId, from, to)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<CheckIn> findJointHistory(UUID habitId, UUID groupId, LocalDate from, LocalDate to) {
        return springDataRepository
            .findByHabitIdAndGroupIdAndLocalDateBetweenOrderByLocalDate(habitId, groupId, from, to)
            .stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<LocalDate> findIndividualDates(UUID habitId, UUID userId) {
        return springDataRepository.findIndividualDates(habitId, userId);
    }

    @Override
    public List<LocalDate> findJointDates(UUID habitId, UUID groupId) {
        return springDataRepository.findJointDates(habitId, groupId);
    }

    @Override
    public List<CheckIn> findRecent(Collection<UUID> habitIds, int limit) {
        if (habitIds.isEmpty()) {
            return List.of();
        }
        return springDataRepository.findByHabitIdInOrderByRecordedAtDesc(habitIds, PageRequest.of(0, limit))
            .stream().map(mapper::toDomain).toList();
    }
}
