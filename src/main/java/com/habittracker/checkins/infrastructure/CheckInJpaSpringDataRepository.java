package com.habittracker.checkins.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

interface CheckInJpaSpringDataRepository extends JpaRepository<CheckInJpaEntity, UUID> {

    Optional<CheckInJpaEntity> findByHabitIdAndUserIdAndLocalDateAndGroupIdIsNull(
        UUID habitId, UUID userId, LocalDate localDate);
}
