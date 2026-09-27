package com.habittracker.checkins.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface CheckInJpaSpringDataRepository extends JpaRepository<CheckInJpaEntity, UUID> {

    /**
     * No conflict target: either partial unique index (individual or joint)
     * may be the one that fires. If a concurrent transaction is inserting the
     * same day, PostgreSQL waits for it to commit and then skips this row.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
        INSERT INTO checkins (id, habit_id, group_id, user_id, performed_by_user_id, recorded_at, local_date)
        VALUES (:id, :habitId, :groupId, :userId, :performedByUserId, :recordedAt, :localDate)
        ON CONFLICT DO NOTHING
        """)
    int insertIfAbsent(UUID id, UUID habitId, UUID groupId, UUID userId, UUID performedByUserId,
                       Instant recordedAt, LocalDate localDate);

    Optional<CheckInJpaEntity> findByHabitIdAndUserIdAndLocalDateAndGroupIdIsNull(
        UUID habitId, UUID userId, LocalDate localDate);

    Optional<CheckInJpaEntity> findByHabitIdAndGroupIdAndLocalDate(UUID habitId, UUID groupId, LocalDate localDate);

    List<CheckInJpaEntity> findByHabitIdAndUserIdAndGroupIdIsNullAndLocalDateBetweenOrderByLocalDate(
        UUID habitId, UUID userId, LocalDate from, LocalDate to);

    List<CheckInJpaEntity> findByHabitIdAndGroupIdAndLocalDateBetweenOrderByLocalDate(
        UUID habitId, UUID groupId, LocalDate from, LocalDate to);

    @Query("""
        select c.localDate from CheckInJpaEntity c
        where c.habitId = :habitId and c.userId = :userId and c.groupId is null
        """)
    List<LocalDate> findIndividualDates(UUID habitId, UUID userId);

    @Query("""
        select c.localDate from CheckInJpaEntity c
        where c.habitId = :habitId and c.groupId = :groupId
        """)
    List<LocalDate> findJointDates(UUID habitId, UUID groupId);

    List<CheckInJpaEntity> findByHabitIdInOrderByRecordedAtDesc(Collection<UUID> habitIds, Pageable pageable);
}
