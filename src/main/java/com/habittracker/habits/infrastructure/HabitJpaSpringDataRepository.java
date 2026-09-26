package com.habittracker.habits.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface HabitJpaSpringDataRepository extends JpaRepository<HabitJpaEntity, UUID> {
}
