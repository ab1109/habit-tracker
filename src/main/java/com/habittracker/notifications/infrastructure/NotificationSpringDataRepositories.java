package com.habittracker.notifications.infrastructure;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

interface NotificationPreferencesSpringDataRepository extends JpaRepository<NotificationPreferencesJpaEntity, UUID> {
}

interface NotificationDeliverySpringDataRepository extends JpaRepository<NotificationDeliveryJpaEntity, UUID> {

    boolean existsByUserIdAndGroupIdAndKindAndPeriodStartAndStatus(
        UUID userId, UUID groupId, String kind, LocalDate periodStart, String status);

    List<NotificationDeliveryJpaEntity> findByUserIdOrderByAttemptedAtDesc(UUID userId, Pageable pageable);
}
