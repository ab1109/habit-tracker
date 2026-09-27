package com.habittracker.checkins.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CheckInJpaSpringDataRepository extends JpaRepository<CheckInJpaEntity, UUID> {

    Optional<CheckInJpaEntity> findByHabitIdAndUserIdAndLocalDateAndGroupIdIsNull(
        UUID habitId, UUID userId, LocalDate localDate);

    List<CheckInJpaEntity> findByHabitIdAndUserIdAndGroupIdIsNullAndLocalDateBetweenOrderByLocalDate(
        UUID habitId, UUID userId, LocalDate from, LocalDate to);

    @Query("""
        select c.localDate from CheckInJpaEntity c
        where c.habitId = :habitId and c.userId = :userId and c.groupId is null
        """)
    List<LocalDate> findIndividualDates(UUID habitId, UUID userId);
}
