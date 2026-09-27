package com.habittracker.habits.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface HabitJpaSpringDataRepository extends JpaRepository<HabitJpaEntity, UUID> {

    List<HabitJpaEntity> findByOwnerTypeAndOwnerIdAndArchivedAtIsNullOrderByCreatedAt(String ownerType, UUID ownerId);
}
